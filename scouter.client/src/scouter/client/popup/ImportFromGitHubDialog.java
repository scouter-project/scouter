/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.client.popup;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import scouter.client.util.ClientFileUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ZipUtil;
import scouter.util.FileUtil;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ImportFromGitHubDialog {

	private static final String HISTORY_FILE = "github_import_history.properties";
	private static final String KEY_REPO_URLS = "repoUrls";
	private static final String KEY_BRANCHES = "branches";
	private static final String KEY_PATHS = "paths";
	private static final String KEY_LAST_REPO_URL = "lastRepoUrl";
	private static final String KEY_LAST_BRANCH = "lastBranch";
	private static final String KEY_LAST_PATH = "lastPath";
	private static final String KEY_LAST_TOKEN = "lastToken";
	private static final String KEY_LAST_IMPORT_TIME = "lastImportTime";
	private static final String KEY_SNOOZE_UNTIL = "snoozeUntil";
	private static final int MAX_HISTORY = 10;

	private final Shell parentShell;
	private Shell dialog;

	private Combo repoUrlCombo;
	private Combo branchCombo;
	private Combo pathCombo;
	private Text tokenText;
	private Table fileTable;
	private Button importButton;

	private java.util.List<GitHubFileEntry> fileEntries = new ArrayList<>();

	public ImportFromGitHubDialog(Shell parentShell) {
		this.parentShell = parentShell;
	}

	public void open() {
		dialog = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText("Import from GitHub");
		dialog.setLayout(new GridLayout(3, false));
		dialog.setSize(600, 450);

		// Repository URL
		Label repoUrlLabel = new Label(dialog, SWT.NONE);
		repoUrlLabel.setText("Repository URL:");
		repoUrlCombo = new Combo(dialog, SWT.DROP_DOWN);
		repoUrlCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		repoUrlCombo.setToolTipText("https://github.com/owner/repo");

		// Branch
		Label branchLabel = new Label(dialog, SWT.NONE);
		branchLabel.setText("Branch:");
		branchCombo = new Combo(dialog, SWT.DROP_DOWN);
		branchCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		branchCombo.setText("main");

		// Path
		Label pathLabel = new Label(dialog, SWT.NONE);
		pathLabel.setText("Path:");
		pathCombo = new Combo(dialog, SWT.DROP_DOWN);
		pathCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		pathCombo.setText("/");

		// Token (optional)
		Label tokenLabel = new Label(dialog, SWT.NONE);
		tokenLabel.setText("Token:");
		tokenText = new Text(dialog, SWT.BORDER | SWT.PASSWORD);
		tokenText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		tokenText.setMessage("auto-detect from git credential");

		// Load button
		Button loadButton = new Button(dialog, SWT.PUSH);
		loadButton.setText("Load");
		loadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadFileList();
			}
		});

		// File table
		fileTable = new Table(dialog, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		fileTable.setLayoutData(tableData);
		fileTable.setHeaderVisible(true);
		fileTable.setLinesVisible(true);

		TableColumn nameColumn = new TableColumn(fileTable, SWT.NONE);
		nameColumn.setText("File Name");
		nameColumn.setWidth(350);

		TableColumn sizeColumn = new TableColumn(fileTable, SWT.NONE);
		sizeColumn.setText("Size");
		sizeColumn.setWidth(100);

		fileTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importButton.setEnabled(fileTable.getSelectionIndex() >= 0);
			}
		});

		// Bottom buttons
		Composite buttonComposite = new Composite(dialog, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 3, 1));
		buttonComposite.setLayout(new GridLayout(2, true));

		importButton = new Button(buttonComposite, SWT.PUSH);
		importButton.setText("Import");
		importButton.setEnabled(false);
		importButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		importButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importSelectedFile();
			}
		});

		Button cancelButton = new Button(buttonComposite, SWT.PUSH);
		cancelButton.setText("Cancel");
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialog.close();
			}
		});

		loadHistory();

		dialog.open();
	}

	private void loadFileList() {
		String repoUrl = repoUrlCombo.getText().trim();
		String branch = branchCombo.getText().trim();
		String path = pathCombo.getText().trim();

		if (repoUrl.isEmpty()) {
			MessageDialog.openWarning(dialog, "Warning", "Please enter Repository URL.");
			return;
		}

		ParsedRepo parsed = parseRepoUrl(repoUrl);
		if (parsed == null) {
			MessageDialog.openWarning(dialog, "Warning",
					"Invalid Repository URL.\nExpected format: https://github.com/owner/repo");
			return;
		}

		if (branch.isEmpty()) {
			branch = "main";
		}
		if (path.isEmpty() || path.equals("/")) {
			path = "";
		}
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		String apiUrl = parsed.apiBase + "/repos/" + parsed.ownerRepo + "/contents/" + path + "?ref=" + branch;

		fileTable.removeAll();
		fileEntries.clear();
		importButton.setEnabled(false);

		final String finalBranch = branch;
		final String finalPath = path;

		try {
			String resolvedToken = resolveGitCredential(parsed.host);

			HttpResponse response = executeGitHubApiGet(apiUrl, resolvedToken);
			int statusCode = response.getStatusLine().getStatusCode();

			// If 401 with "token" format, retry with "Bearer" format
			if (statusCode == 401 && resolvedToken != null) {
				EntityUtils.consumeQuietly(response.getEntity());
				response = executeGitHubApiGet(apiUrl, "Bearer:" + resolvedToken);
				statusCode = response.getStatusLine().getStatusCode();
			}

			HttpEntity entity = response.getEntity();
			String json = EntityUtils.toString(entity, "UTF-8");

			if (statusCode != 200) {
				MessageDialog.openError(dialog, "Error",
						"GitHub API returned status " + statusCode + "."
						+ "\nURL: " + apiUrl
						+ "\nToken: " + (resolvedToken != null ? maskToken(resolvedToken) + " (len=" + resolvedToken.length() + ")" : "(none)")
						+ "\nResponse: " + (json.length() > 300 ? json.substring(0, 300) + "..." : json));
				return;
			}

			java.util.List<GitHubFileEntry> entries = parseGitHubContentsResponse(json);
			for (GitHubFileEntry entry : entries) {
				if (entry.name.toLowerCase().endsWith(".zip") && "file".equals(entry.type)) {
					entry.host = parsed.host;
					entry.apiBase = parsed.apiBase;
					entry.ownerRepo = parsed.ownerRepo;
					entry.branch = finalBranch;
					entry.filePath = (finalPath.isEmpty() ? "" : finalPath + "/") + entry.name;
					fileEntries.add(entry);
					TableItem item = new TableItem(fileTable, SWT.NONE);
					item.setText(0, entry.name);
					item.setText(1, formatSize(entry.size));
				}
			}

			if (fileEntries.isEmpty()) {
				MessageDialog.openInformation(dialog, "Info", "No .zip files found in the specified path.");
			}

			saveHistory(repoUrl, finalBranch, pathCombo.getText().trim());

		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(dialog, "Error", "Failed to load file list: " + e.getMessage());
		}
	}

	private void importSelectedFile() {
		int index = fileTable.getSelectionIndex();
		if (index < 0 || index >= fileEntries.size()) {
			return;
		}

		GitHubFileEntry entry = fileEntries.get(index);

		String downloadUrl = entry.downloadUrl;
		String acceptHeader = null;
		if (downloadUrl == null || downloadUrl.isEmpty()) {
			downloadUrl = entry.apiBase + "/repos/" + entry.ownerRepo + "/contents/" + entry.filePath + "?ref=" + entry.branch;
			acceptHeader = "application/vnd.github.v3.raw";
		}

		try {
			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpGet httpGet = new HttpGet(downloadUrl);
			httpGet.addHeader("User-Agent", "Scouter-Client");
			if (acceptHeader != null) {
				httpGet.addHeader("Accept", acceptHeader);
			}
			addAuthHeader(httpGet, entry.host);

			HttpResponse response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode != 200) {
				MessageDialog.openError(dialog, "Error", "Failed to download file. HTTP status: " + statusCode);
				return;
			}

			String workspaceRootName = Platform.getInstanceLocation().getURL().getFile();
			String tempDir = workspaceRootName + "/import-temp";
			ClientFileUtil.deleteDirectory(new File(tempDir));
			FileUtil.mkdirs(tempDir);

			String tempFilePath = tempDir + "/" + entry.name;
			HttpEntity downloadEntity = response.getEntity();
			InputStream inputStream = downloadEntity.getContent();
			FileOutputStream fos = new FileOutputStream(tempFilePath);
			try {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					fos.write(buffer, 0, bytesRead);
				}
			} finally {
				fos.close();
				inputStream.close();
			}

			String importWorkingDirName = workspaceRootName + "/import-working";
			ClientFileUtil.deleteDirectory(new File(importWorkingDirName));
			FileUtil.mkdirs(importWorkingDirName);
			try {
				ZipUtil.decompress(tempFilePath, importWorkingDirName);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			ClientFileUtil.deleteDirectory(new File(tempDir));

			saveLastImportTime();

			dialog.close();

			MessageDialog.openInformation(parentShell, "Info", "Import completed.\nRestarting...");
			ExUtil.exec(new Runnable() {
				public void run() {
					PlatformUI.getWorkbench().restart();
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(dialog, "Error", "Failed to import file: " + e.getMessage());
		}
	}

	private ParsedRepo parseRepoUrl(String repoUrl) {
		try {
			if (!repoUrl.startsWith("http://") && !repoUrl.startsWith("https://")) {
				repoUrl = "https://" + repoUrl;
			}
			URI uri = new URI(repoUrl);
			String host = uri.getHost();
			if (host == null) return null;

			String uriPath = uri.getPath();
			if (uriPath == null || uriPath.isEmpty()) return null;
			if (uriPath.startsWith("/")) {
				uriPath = uriPath.substring(1);
			}
			if (uriPath.endsWith("/")) {
				uriPath = uriPath.substring(0, uriPath.length() - 1);
			}

			String[] segments = uriPath.split("/");
			if (segments.length < 2) return null;

			String repo = segments[1];
			if (repo.endsWith(".git")) {
				repo = repo.substring(0, repo.length() - 4);
			}
			String ownerRepo = segments[0] + "/" + repo;
			String apiBase;
			if ("github.com".equalsIgnoreCase(host)) {
				apiBase = "https://api.github.com";
			} else {
				apiBase = "https://" + host + "/api/v3";
			}

			ParsedRepo result = new ParsedRepo();
			result.host = host;
			result.apiBase = apiBase;
			result.ownerRepo = ownerRepo;
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	private String getHistoryFilePath() {
		try {
			String workspace = Platform.getInstanceLocation().getURL().getFile();
			return workspace + "/" + HISTORY_FILE;
		} catch (Exception e) {
			return null;
		}
	}

	private void loadHistory() {
		String filePath = getHistoryFilePath();
		if (filePath == null) return;

		File file = new File(filePath);
		if (!file.exists()) return;

		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(file)) {
			props.load(fis);
		} catch (Exception e) {
			return;
		}

		String repoUrls = props.getProperty(KEY_REPO_URLS, "");
		String branches = props.getProperty(KEY_BRANCHES, "");
		String paths = props.getProperty(KEY_PATHS, "");

		if (!repoUrls.isEmpty()) {
			for (String item : repoUrls.split("\n")) {
				if (!item.trim().isEmpty()) {
					repoUrlCombo.add(item.trim());
				}
			}
		}
		if (!branches.isEmpty()) {
			for (String item : branches.split("\n")) {
				if (!item.trim().isEmpty()) {
					branchCombo.add(item.trim());
				}
			}
		}
		if (!paths.isEmpty()) {
			for (String item : paths.split("\n")) {
				if (!item.trim().isEmpty()) {
					pathCombo.add(item.trim());
				}
			}
		}

		// Restore last-used values
		String lastRepoUrl = props.getProperty(KEY_LAST_REPO_URL, "");
		String lastBranch = props.getProperty(KEY_LAST_BRANCH, "");
		String lastPath = props.getProperty(KEY_LAST_PATH, "");
		String lastToken = props.getProperty(KEY_LAST_TOKEN, "");

		if (!lastRepoUrl.isEmpty()) {
			repoUrlCombo.setText(lastRepoUrl);
		}
		if (!lastBranch.isEmpty()) {
			branchCombo.setText(lastBranch);
		}
		if (!lastPath.isEmpty()) {
			pathCombo.setText(lastPath);
		}
		if (!lastToken.isEmpty()) {
			tokenText.setText(lastToken);
		}
	}

	private void saveHistory(String repoUrl, String branch, String path) {
		String filePath = getHistoryFilePath();
		if (filePath == null) return;

		String token = tokenText != null ? tokenText.getText().trim() : "";

		Properties props = new Properties();
		File file = new File(filePath);
		if (file.exists()) {
			try (FileInputStream fis = new FileInputStream(file)) {
				props.load(fis);
			} catch (Exception e) {
				// ignore
			}
		}

		String repoUrls = addToHistory(props.getProperty(KEY_REPO_URLS, ""), repoUrl);
		String branches = addToHistory(props.getProperty(KEY_BRANCHES, ""), branch);
		String paths = addToHistory(props.getProperty(KEY_PATHS, ""), path);

		props.setProperty(KEY_REPO_URLS, repoUrls);
		props.setProperty(KEY_BRANCHES, branches);
		props.setProperty(KEY_PATHS, paths);

		// Save last-used values
		props.setProperty(KEY_LAST_REPO_URL, repoUrl);
		props.setProperty(KEY_LAST_BRANCH, branch);
		props.setProperty(KEY_LAST_PATH, path);
		props.setProperty(KEY_LAST_TOKEN, token);

		try (FileOutputStream fos = new FileOutputStream(file)) {
			props.store(fos, "GitHub Import History");
		} catch (Exception e) {
			// ignore
		}

		refreshComboItems(repoUrlCombo, repoUrls);
		refreshComboItems(branchCombo, branches);
		refreshComboItems(pathCombo, paths);
	}

	private String addToHistory(String existing, String newValue) {
		if (newValue == null || newValue.trim().isEmpty()) return existing;
		newValue = newValue.trim();

		LinkedList<String> list = new LinkedList<>();
		if (!existing.isEmpty()) {
			for (String item : existing.split("\n")) {
				if (!item.trim().isEmpty()) {
					list.add(item.trim());
				}
			}
		}
		list.remove(newValue);
		list.addFirst(newValue);
		while (list.size() > MAX_HISTORY) {
			list.removeLast();
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) sb.append("\n");
			sb.append(list.get(i));
		}
		return sb.toString();
	}

	private void refreshComboItems(Combo combo, String history) {
		String currentText = combo.getText();
		combo.removeAll();
		if (!history.isEmpty()) {
			for (String item : history.split("\n")) {
				if (!item.trim().isEmpty()) {
					combo.add(item.trim());
				}
			}
		}
		combo.setText(currentText);
	}

	private java.util.List<GitHubFileEntry> parseGitHubContentsResponse(String json) {
		java.util.List<GitHubFileEntry> entries = new ArrayList<>();
		json = json.trim();
		if (!json.startsWith("[")) {
			return entries;
		}

		int i = 1; // skip '['
		while (i < json.length()) {
			int objStart = json.indexOf('{', i);
			if (objStart < 0) break;
			int objEnd = findMatchingBrace(json, objStart);
			if (objEnd < 0) break;

			String obj = json.substring(objStart, objEnd + 1);
			String name = extractJsonStringValue(obj, "name");
			String type = extractJsonStringValue(obj, "type");
			String downloadUrl = extractJsonStringValue(obj, "download_url");
			long size = extractJsonLongValue(obj, "size");

			if (name != null && type != null) {
				GitHubFileEntry entry = new GitHubFileEntry();
				entry.name = name;
				entry.type = type;
				entry.downloadUrl = downloadUrl;
				entry.size = size;
				entries.add(entry);
			}

			i = objEnd + 1;
		}

		return entries;
	}

	private int findMatchingBrace(String json, int start) {
		int depth = 0;
		boolean inString = false;
		boolean escaped = false;
		for (int i = start; i < json.length(); i++) {
			char c = json.charAt(i);
			if (escaped) {
				escaped = false;
				continue;
			}
			if (c == '\\') {
				escaped = true;
				continue;
			}
			if (c == '"') {
				inString = !inString;
				continue;
			}
			if (inString) continue;
			if (c == '{') depth++;
			else if (c == '}') {
				depth--;
				if (depth == 0) return i;
			}
		}
		return -1;
	}

	private String extractJsonStringValue(String json, String key) {
		String searchKey = "\"" + key + "\"";
		int keyIndex = json.indexOf(searchKey);
		if (keyIndex < 0) return null;

		int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
		if (colonIndex < 0) return null;

		int valueStart = colonIndex + 1;
		while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
			valueStart++;
		}

		if (valueStart >= json.length()) return null;

		if (json.charAt(valueStart) == 'n' && json.startsWith("null", valueStart)) {
			return null;
		}

		if (json.charAt(valueStart) != '"') return null;

		int valueEnd = valueStart + 1;
		boolean escaped = false;
		StringBuilder sb = new StringBuilder();
		while (valueEnd < json.length()) {
			char c = json.charAt(valueEnd);
			if (escaped) {
				sb.append(c);
				escaped = false;
			} else if (c == '\\') {
				escaped = true;
			} else if (c == '"') {
				return sb.toString();
			} else {
				sb.append(c);
			}
			valueEnd++;
		}
		return null;
	}

	private long extractJsonLongValue(String json, String key) {
		String searchKey = "\"" + key + "\"";
		int keyIndex = json.indexOf(searchKey);
		if (keyIndex < 0) return 0;

		int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
		if (colonIndex < 0) return 0;

		int valueStart = colonIndex + 1;
		while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
			valueStart++;
		}

		StringBuilder sb = new StringBuilder();
		while (valueStart < json.length()) {
			char c = json.charAt(valueStart);
			if (c >= '0' && c <= '9') {
				sb.append(c);
			} else if (sb.length() > 0) {
				break;
			}
			valueStart++;
		}

		if (sb.length() == 0) return 0;
		try {
			return Long.parseLong(sb.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String formatSize(long bytes) {
		if (bytes < 1024) return bytes + " B";
		if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
		return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
	}

	private static class ParsedRepo {
		String host;
		String apiBase;
		String ownerRepo;
	}

	private static class GitHubFileEntry {
		String name;
		String type;
		String downloadUrl;
		long size;
		String host;
		String apiBase;
		String ownerRepo;
		String branch;
		String filePath;
	}

	private String resolveGitCredential(String host) {
		// 1. Check UI token field
		if (tokenText != null) {
			String uiToken = tokenText.getText().trim();
			if (!uiToken.isEmpty()) {
				return uiToken;
			}
		}

		// 2. Try git credential fill (host-specific, works for both github.com and Enterprise)
		String gitPath = findGitPath();
		if (gitPath != null) {
			try {
				ProcessBuilder pb = new ProcessBuilder(gitPath, "credential", "fill");
				pb.redirectErrorStream(false);
				Process process = pb.start();

				OutputStream os = process.getOutputStream();
				os.write(("protocol=https\nhost=" + host + "\n\n").getBytes("UTF-8"));
				os.flush();
				os.close();

				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
				String password = null;
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("password=")) {
						password = line.substring("password=".length());
					}
				}
				reader.close();

				boolean finished = process.waitFor(5, TimeUnit.SECONDS);
				if (!finished) {
					process.destroyForcibly();
				} else if (password != null && !password.trim().isEmpty()) {
					return password.trim();
				}
			} catch (Exception e) {
				// git credential not available
			}
		}

		// 3. Fallback: environment variables (only for github.com, as they are not host-specific)
		if ("github.com".equalsIgnoreCase(host)) {
			String envToken = System.getenv("GH_TOKEN");
			if (envToken == null || envToken.isEmpty()) {
				envToken = System.getenv("GITHUB_TOKEN");
			}
			if (envToken != null && !envToken.isEmpty()) {
				return envToken;
			}
		}

		return null;
	}

	private String findGitPath() {
		String[] candidates = {
			"/usr/bin/git",
			"/usr/local/bin/git",
			"/opt/homebrew/bin/git",
			"/opt/local/bin/git",
			"git"
		};
		for (String candidate : candidates) {
			try {
				File f = new File(candidate);
				if (f.isAbsolute() && f.canExecute()) {
					return candidate;
				}
				if (!f.isAbsolute()) {
					ProcessBuilder pb = new ProcessBuilder(candidate, "--version");
					pb.redirectErrorStream(true);
					Process p = pb.start();
					boolean done = p.waitFor(3, TimeUnit.SECONDS);
					if (done && p.exitValue() == 0) {
						return candidate;
					}
					if (!done) {
						p.destroyForcibly();
					}
				}
			} catch (Exception e) {
				// try next
			}
		}
		return null;
	}

	private HttpResponse executeGitHubApiGet(String url, String token) throws Exception {
		RequestConfig config = RequestConfig.custom()
				.setRedirectsEnabled(false)
				.setConnectTimeout(10000)
				.setSocketTimeout(30000)
				.build();
		HttpClient httpClient = HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.build();

		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Accept", "application/vnd.github.v3+json");
		httpGet.addHeader("User-Agent", "Scouter-Client");
		addAuthToRequest(httpGet, token);

		HttpResponse response = httpClient.execute(httpGet);
		int statusCode = response.getStatusLine().getStatusCode();

		// Follow redirect manually to preserve Authorization header
		if (statusCode == 301 || statusCode == 302 || statusCode == 307) {
			String redirectUrl = response.getFirstHeader("Location").getValue();
			EntityUtils.consumeQuietly(response.getEntity());
			HttpGet redirectGet = new HttpGet(redirectUrl);
			redirectGet.addHeader("Accept", "application/vnd.github.v3+json");
			redirectGet.addHeader("User-Agent", "Scouter-Client");
			addAuthToRequest(redirectGet, token);
			response = httpClient.execute(redirectGet);
		}

		return response;
	}

	private void addAuthToRequest(HttpGet httpGet, String token) {
		if (token == null) return;
		// "Bearer:xxx" format means use Bearer, otherwise use "token" prefix
		if (token.startsWith("Bearer:")) {
			httpGet.addHeader("Authorization", "Bearer " + token.substring(7));
		} else {
			httpGet.addHeader("Authorization", "token " + token);
		}
	}

	private void addAuthHeader(HttpGet httpGet, String host) {
		String token = resolveGitCredential(host);
		if (token != null) {
			httpGet.addHeader("Authorization", "token " + token);
		}
	}

	private String maskToken(String token) {
		if (token == null) return "null";
		if (token.length() <= 8) return "***";
		return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
	}

	private void saveLastImportTime() {
		String filePath = getHistoryFilePath();
		if (filePath == null) return;

		Properties props = new Properties();
		File file = new File(filePath);
		if (file.exists()) {
			try (FileInputStream fis = new FileInputStream(file)) {
				props.load(fis);
			} catch (Exception e) {
				// ignore
			}
		}
		props.setProperty(KEY_LAST_IMPORT_TIME, String.valueOf(System.currentTimeMillis()));
		try (FileOutputStream fos = new FileOutputStream(file)) {
			props.store(fos, "GitHub Import History");
		} catch (Exception e) {
			// ignore
		}
	}

	public static void snooze30Days() {
		try {
			String workspace = Platform.getInstanceLocation().getURL().getFile();
			String filePath = workspace + "/" + HISTORY_FILE;
			File file = new File(filePath);
			Properties props = new Properties();
			if (file.exists()) {
				try (FileInputStream fis = new FileInputStream(file)) {
					props.load(fis);
				}
			}
			long until = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000;
			props.setProperty(KEY_SNOOZE_UNTIL, String.valueOf(until));
			try (FileOutputStream fos = new FileOutputStream(file)) {
				props.store(fos, "GitHub Import History");
			}
		} catch (Exception e) {
			// ignore
		}
	}

	public static boolean hasNewSettings() {
		try {
			String workspace = Platform.getInstanceLocation().getURL().getFile();
			String filePath = workspace + "/" + HISTORY_FILE;
			File file = new File(filePath);
			if (!file.exists()) return false;

			Properties props = new Properties();
			try (FileInputStream fis = new FileInputStream(file)) {
				props.load(fis);
			}

			// Check snooze
			String snoozeUntil = props.getProperty(KEY_SNOOZE_UNTIL, "");
			if (!snoozeUntil.isEmpty()) {
				try {
					long until = Long.parseLong(snoozeUntil);
					if (System.currentTimeMillis() < until) return false;
				} catch (NumberFormatException e) {
					// ignore
				}
			}

			String repoUrl = props.getProperty(KEY_LAST_REPO_URL, "");
			String branch = props.getProperty(KEY_LAST_BRANCH, "");
			String path = props.getProperty(KEY_LAST_PATH, "");
			String token = props.getProperty(KEY_LAST_TOKEN, "");
			String lastImportTimeStr = props.getProperty(KEY_LAST_IMPORT_TIME, "");

			if (repoUrl.isEmpty()) return false;

			long lastImportTime = 0;
			if (!lastImportTimeStr.isEmpty()) {
				try {
					lastImportTime = Long.parseLong(lastImportTimeStr);
				} catch (NumberFormatException e) {
					// treat as never imported
				}
			}

			ParsedRepo parsed = parseRepoUrlStatic(repoUrl);
			if (parsed == null) return false;

			if (branch.isEmpty()) branch = "main";
			if (path.isEmpty() || path.equals("/")) path = "";
			if (path.startsWith("/")) path = path.substring(1);

			String resolvedToken = token.isEmpty() ? resolveGitCredentialStatic(parsed.host) : token;

			// Use Commits API to get latest commit time for the path
			String commitsUrl = parsed.apiBase + "/repos/" + parsed.ownerRepo
					+ "/commits?sha=" + branch
					+ "&path=" + (path.isEmpty() ? "/" : path)
					+ "&per_page=1";

			RequestConfig config = RequestConfig.custom()
					.setRedirectsEnabled(true)
					.setConnectTimeout(10000)
					.setSocketTimeout(15000)
					.build();
			HttpClient httpClient = HttpClientBuilder.create()
					.setDefaultRequestConfig(config)
					.build();
			HttpGet httpGet = new HttpGet(commitsUrl);
			httpGet.addHeader("Accept", "application/vnd.github.v3+json");
			httpGet.addHeader("User-Agent", "Scouter-Client");
			if (resolvedToken != null) {
				httpGet.addHeader("Authorization", "token " + resolvedToken);
			}

			HttpResponse response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() != 200) {
				EntityUtils.consumeQuietly(response.getEntity());
				return false;
			}

			String json = EntityUtils.toString(response.getEntity(), "UTF-8").trim();
			if (!json.startsWith("[")) return false;

			// Extract the latest commit date (ISO 8601)
			// Path: [0].commit.committer.date
			int objStart = json.indexOf('{');
			if (objStart < 0) return false;
			int objEnd = findMatchingBraceStatic(json, objStart);
			if (objEnd < 0) return false;
			String commitObj = json.substring(objStart, objEnd + 1);

			String dateStr = extractNestedDateFromCommit(commitObj);
			if (dateStr == null) return false;

			long commitTime = parseISO8601(dateStr);
			if (commitTime <= 0) return false;

			return commitTime > lastImportTime;

		} catch (Exception e) {
			// check failed, skip silently
			return false;
		}
	}

	private static String extractNestedDateFromCommit(String json) {
		// Find "commit" object, then "committer" inside it, then "date"
		String commitKey = "\"commit\"";
		int idx = json.indexOf(commitKey);
		if (idx < 0) return null;
		int braceStart = json.indexOf('{', idx + commitKey.length());
		if (braceStart < 0) return null;
		int braceEnd = findMatchingBraceStatic(json, braceStart);
		if (braceEnd < 0) return null;
		String commitInner = json.substring(braceStart, braceEnd + 1);

		String committerKey = "\"committer\"";
		int cIdx = commitInner.indexOf(committerKey);
		if (cIdx < 0) return null;
		int cBraceStart = commitInner.indexOf('{', cIdx + committerKey.length());
		if (cBraceStart < 0) return null;
		int cBraceEnd = findMatchingBraceStatic(commitInner, cBraceStart);
		if (cBraceEnd < 0) return null;
		String committerObj = commitInner.substring(cBraceStart, cBraceEnd + 1);

		return extractJsonStringValueStatic(committerObj, "date");
	}

	private static long parseISO8601(String dateStr) {
		// Parse "2025-01-15T10:30:00Z" format
		try {
			dateStr = dateStr.trim();
			if (dateStr.endsWith("Z")) {
				dateStr = dateStr.substring(0, dateStr.length() - 1);
			}
			// Handle timezone offset like +09:00
			int tzIdx = dateStr.lastIndexOf('+');
			if (tzIdx < 10) tzIdx = dateStr.lastIndexOf('-', dateStr.length() - 1);
			if (tzIdx > 10) {
				dateStr = dateStr.substring(0, tzIdx);
			}
			String[] parts = dateStr.split("T");
			if (parts.length != 2) return 0;
			String[] dateParts = parts[0].split("-");
			String[] timeParts = parts[1].split(":");
			if (dateParts.length != 3 || timeParts.length < 2) return 0;

			java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
			cal.set(Integer.parseInt(dateParts[0]),
					Integer.parseInt(dateParts[1]) - 1,
					Integer.parseInt(dateParts[2]),
					Integer.parseInt(timeParts[0]),
					Integer.parseInt(timeParts[1]),
					timeParts.length > 2 ? Integer.parseInt(timeParts[2].split("\\.")[0]) : 0);
			cal.set(java.util.Calendar.MILLISECOND, 0);
			return cal.getTimeInMillis();
		} catch (Exception e) {
			return 0;
		}
	}

	private static ParsedRepo parseRepoUrlStatic(String repoUrl) {
		try {
			if (!repoUrl.startsWith("http://") && !repoUrl.startsWith("https://")) {
				repoUrl = "https://" + repoUrl;
			}
			URI uri = new URI(repoUrl);
			String host = uri.getHost();
			if (host == null) return null;
			String uriPath = uri.getPath();
			if (uriPath == null || uriPath.isEmpty()) return null;
			if (uriPath.startsWith("/")) uriPath = uriPath.substring(1);
			if (uriPath.endsWith("/")) uriPath = uriPath.substring(0, uriPath.length() - 1);
			String[] segments = uriPath.split("/");
			if (segments.length < 2) return null;
			String repo = segments[1];
			if (repo.endsWith(".git")) repo = repo.substring(0, repo.length() - 4);
			ParsedRepo result = new ParsedRepo();
			result.host = host;
			result.ownerRepo = segments[0] + "/" + repo;
			result.apiBase = "github.com".equalsIgnoreCase(host) ? "https://api.github.com" : "https://" + host + "/api/v3";
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	private static String resolveGitCredentialStatic(String host) {
		String gitPath = findGitPathStatic();
		if (gitPath != null) {
			try {
				ProcessBuilder pb = new ProcessBuilder(gitPath, "credential", "fill");
				pb.redirectErrorStream(false);
				Process process = pb.start();
				OutputStream os = process.getOutputStream();
				os.write(("protocol=https\nhost=" + host + "\n\n").getBytes("UTF-8"));
				os.flush();
				os.close();
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
				String password = null;
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("password=")) {
						password = line.substring("password=".length());
					}
				}
				reader.close();
				boolean finished = process.waitFor(5, TimeUnit.SECONDS);
				if (!finished) {
					process.destroyForcibly();
				} else if (password != null && !password.trim().isEmpty()) {
					return password.trim();
				}
			} catch (Exception e) {
				// ignore
			}
		}
		if ("github.com".equalsIgnoreCase(host)) {
			String envToken = System.getenv("GH_TOKEN");
			if (envToken == null || envToken.isEmpty()) envToken = System.getenv("GITHUB_TOKEN");
			if (envToken != null && !envToken.isEmpty()) return envToken;
		}
		return null;
	}

	private static String findGitPathStatic() {
		String[] candidates = { "/usr/bin/git", "/usr/local/bin/git", "/opt/homebrew/bin/git", "/opt/local/bin/git", "git" };
		for (String candidate : candidates) {
			try {
				File f = new File(candidate);
				if (f.isAbsolute() && f.canExecute()) return candidate;
				if (!f.isAbsolute()) {
					ProcessBuilder pb = new ProcessBuilder(candidate, "--version");
					pb.redirectErrorStream(true);
					Process p = pb.start();
					boolean done = p.waitFor(3, TimeUnit.SECONDS);
					if (done && p.exitValue() == 0) return candidate;
					if (!done) p.destroyForcibly();
				}
			} catch (Exception e) {
				// try next
			}
		}
		return null;
	}

	private static int findMatchingBraceStatic(String json, int start) {
		int depth = 0;
		boolean inString = false;
		boolean escaped = false;
		for (int i = start; i < json.length(); i++) {
			char c = json.charAt(i);
			if (escaped) { escaped = false; continue; }
			if (c == '\\') { escaped = true; continue; }
			if (c == '"') { inString = !inString; continue; }
			if (inString) continue;
			if (c == '{') depth++;
			else if (c == '}') { depth--; if (depth == 0) return i; }
		}
		return -1;
	}

	private static String extractJsonStringValueStatic(String json, String key) {
		String searchKey = "\"" + key + "\"";
		int keyIndex = json.indexOf(searchKey);
		if (keyIndex < 0) return null;
		int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
		if (colonIndex < 0) return null;
		int valueStart = colonIndex + 1;
		while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;
		if (valueStart >= json.length()) return null;
		if (json.charAt(valueStart) == 'n' && json.startsWith("null", valueStart)) return null;
		if (json.charAt(valueStart) != '"') return null;
		int valueEnd = valueStart + 1;
		boolean escaped = false;
		StringBuilder sb = new StringBuilder();
		while (valueEnd < json.length()) {
			char c = json.charAt(valueEnd);
			if (escaped) { sb.append(c); escaped = false; }
			else if (c == '\\') { escaped = true; }
			else if (c == '"') { return sb.toString(); }
			else { sb.append(c); }
			valueEnd++;
		}
		return null;
	}
}
