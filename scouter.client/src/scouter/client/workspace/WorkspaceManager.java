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
package scouter.client.workspace;

import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WorkspaceManager {

	private static WorkspaceManager instance;

	private static final String CONFIG_FILE = System.getProperty("user.home")
			+ File.separator + ".scouter" + File.separator + ".scouter-workspaces.properties";
	private static final String KEY_COUNT = "workspace.count";
	private static final String KEY_PATH_PREFIX = "workspace.path.";
	private static final String KEY_NAME_PREFIX = "workspace.name.";
	private static final String KEY_LAST_USED_PREFIX = "workspace.lastUsed.";

	public static synchronized WorkspaceManager getInstance() {
		if (instance == null) {
			instance = new WorkspaceManager();
		}
		return instance;
	}

	private WorkspaceManager() {
	}

	public List<WorkspaceInfo> getWorkspaceList() {
		List<WorkspaceInfo> list = new ArrayList<>();
		Properties props = loadProperties();
		int count = Integer.parseInt(props.getProperty(KEY_COUNT, "0"));
		for (int i = 0; i < count; i++) {
			String path = props.getProperty(KEY_PATH_PREFIX + i);
			String name = props.getProperty(KEY_NAME_PREFIX + i, "");
			long lastUsed = Long.parseLong(props.getProperty(KEY_LAST_USED_PREFIX + i, "0"));
			if (path != null && !path.isEmpty()) {
				list.add(new WorkspaceInfo(path, name, lastUsed));
			}
		}
		return list;
	}

	public void addWorkspace(String path, String displayName) {
		List<WorkspaceInfo> list = getWorkspaceList();
		for (WorkspaceInfo info : list) {
			if (normalizePath(info.getPath()).equals(normalizePath(path))) {
				return;
			}
		}
		list.add(new WorkspaceInfo(path, displayName, System.currentTimeMillis()));
		saveWorkspaceList(list);
	}

	public void removeWorkspace(String path) {
		List<WorkspaceInfo> list = getWorkspaceList();
		list.removeIf(info -> normalizePath(info.getPath()).equals(normalizePath(path)));
		saveWorkspaceList(list);
	}

	public void deleteWorkspace(String path) {
		removeWorkspace(path);
		deleteDirectory(new File(path));
	}

	public void setLastUsed(String path) {
		List<WorkspaceInfo> list = getWorkspaceList();
		for (WorkspaceInfo info : list) {
			if (normalizePath(info.getPath()).equals(normalizePath(path))) {
				info.setLastUsed(System.currentTimeMillis());
				break;
			}
		}
		saveWorkspaceList(list);
	}

	public String getLastUsedWorkspacePath() {
		List<WorkspaceInfo> list = getWorkspaceList();
		if (list.isEmpty()) {
			return null;
		}
		WorkspaceInfo lastUsed = null;
		for (WorkspaceInfo info : list) {
			if (lastUsed == null || info.getLastUsed() > lastUsed.getLastUsed()) {
				lastUsed = info;
			}
		}
		return lastUsed != null ? lastUsed.getPath() : null;
	}

	public String getCurrentWorkspacePath() {
		try {
			return Platform.getInstanceLocation().getURL().getFile();
		} catch (Exception e) {
			return "";
		}
	}

	public void registerCurrentWorkspace(String workspacePath) {
		String path = normalizePath(workspacePath);
		List<WorkspaceInfo> list = getWorkspaceList();
		boolean found = false;
		for (WorkspaceInfo info : list) {
			if (normalizePath(info.getPath()).equals(path)) {
				info.setLastUsed(System.currentTimeMillis());
				found = true;
				break;
			}
		}
		if (!found) {
			String name = new File(path).getName();
			if (name.isEmpty()) {
				name = "Default";
			}
			list.add(new WorkspaceInfo(path, name, System.currentTimeMillis()));
		}
		saveWorkspaceList(list);
	}

	public String getDisplayName(String path) {
		List<WorkspaceInfo> list = getWorkspaceList();
		for (WorkspaceInfo info : list) {
			if (normalizePath(info.getPath()).equals(normalizePath(path))) {
				return info.getDisplayName();
			}
		}
		String name = new File(path).getName();
		return name.isEmpty() ? "Default" : name;
	}

	private void saveWorkspaceList(List<WorkspaceInfo> list) {
		Properties props = new Properties();
		props.setProperty(KEY_COUNT, String.valueOf(list.size()));
		for (int i = 0; i < list.size(); i++) {
			WorkspaceInfo info = list.get(i);
			props.setProperty(KEY_PATH_PREFIX + i, info.getPath());
			props.setProperty(KEY_NAME_PREFIX + i, info.getDisplayName());
			props.setProperty(KEY_LAST_USED_PREFIX + i, String.valueOf(info.getLastUsed()));
		}
		File configFile = new File(CONFIG_FILE);
		configFile.getParentFile().mkdirs();
		try (FileOutputStream fos = new FileOutputStream(configFile)) {
			props.store(fos, "Scouter Workspace List");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Properties loadProperties() {
		Properties props = new Properties();
		File file = new File(CONFIG_FILE);
		if (file.exists()) {
			try (FileInputStream fis = new FileInputStream(file)) {
				props.load(fis);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return props;
	}

	private String normalizePath(String path) {
		if (path == null) return "";
		if (path.endsWith("/") || path.endsWith(File.separator)) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	private void deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] children = dir.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteDirectory(child);
				}
			}
		}
		dir.delete();
	}
}
