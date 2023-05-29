package scouter.agent.extra.java20;

import com.sun.management.HotSpotDiagnosticMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2023/05/28
 */
public class ThreadDumps {
	public static List<String> threadDumpWithVirtualThread(boolean json) {
		HotSpotDiagnosticMXBean platformMXBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
		try {
			String suffix = ".dump";
			HotSpotDiagnosticMXBean.ThreadDumpFormat format = HotSpotDiagnosticMXBean.ThreadDumpFormat.TEXT_PLAIN;
			if (json) {
				suffix = ".jsondump";
				format = HotSpotDiagnosticMXBean.ThreadDumpFormat.JSON;
			}
			Path temp = Files.createTempFile("scouterdump_", "_vthread");
			String dumpFilePath = temp.toString() + suffix;
			platformMXBean.dumpThreads(dumpFilePath, format);
			Path path = Paths.get(dumpFilePath);
			List<String> dumpByLine = Files.readAllLines(path);
			try {
				temp.toFile().deleteOnExit();
				path.toFile().deleteOnExit();
			} catch (Exception e) {
			}
			return dumpByLine;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
