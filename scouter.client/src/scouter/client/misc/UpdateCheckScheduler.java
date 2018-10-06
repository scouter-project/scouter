package scouter.client.misc;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import scouter.Version;
import scouter.client.server.ServerManager;
import scouter.client.util.ExUtil;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

import java.io.IOException;

public enum UpdateCheckScheduler {
	INSTANCE;

	private final String RELEASE_BASE_URL = "https://github.com/scouter-project/scouter/releases";
	private final String releaseVersionAppendix = "/tag/";

	boolean initialized = false;

	public void initialize() {
		ExUtil.asyncRun(() -> {
			if (!initialized) {
				initialized = true;
				try {
					ThreadUtil.sleep(6000);
					process();
				} catch (Exception e) {
					initialized = false;
					e.printStackTrace();
				}
			}
		});
	}

	private void process() throws IOException {
		final String recommendedVersion = UpdateCheckScheduler.getRecommendedVersion();
		final String clientVersion = Version.getVersion();
		
		if(Version.versionCompare(recommendedVersion, clientVersion) > 0) {
			ExUtil.exec(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
				@Override
				public void run() {
					String message = "You can update your scouter client v" + clientVersion + " to v" + recommendedVersion + "\nDo you want to download it from the release page?";
					boolean ok = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Confirm", message);
					if (ok) {
						org.eclipse.swt.program.Program.launch(RELEASE_BASE_URL + releaseVersionAppendix + "v" + recommendedVersion);
					}
				}
			});
		}
	}

	public static String getRecommendedVersion() {
		return ServerManager.getInstance().getOpenServerList().stream()
				.map(ServerManager.getInstance()::getServer)
				.map(s -> StringUtil.isNotEmpty(s.getRecommendedClientVersion()) ? s.getRecommendedClientVersion() : s.getVersion())
				.map(UpdateCheckScheduler::getVersionOnly)
				.max(Version::versionCompare).orElse("");
	}

	private static String getVersionOnly(String buildIncludedVersion) {
		String[] parts = buildIncludedVersion.split(" ");
		return parts[0];
	}
}
