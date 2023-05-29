package scouter.agent.netio.request.handle;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;

import java.util.List;

public class DiskPerf {
	Configure conf = Configure.getInstance();
	SystemInfo si = new SystemInfo();
	OperatingSystem os = si.getOperatingSystem();
	HardwareAbstractionLayer hal = si.getHardware();
	@RequestHandler(RequestCmd.HOST_DISK_USAGE)
	public Pack usage(Pack param) {
		MapPack pack = new MapPack();
		ListValue deviceList = pack.newList("Device");
		ListValue totalList = pack.newList("Total");
		ListValue usedList = pack.newList("Used");
		ListValue freeList = pack.newList("Free");
		ListValue pctList = pack.newList("Pct");
		ListValue typeList = pack.newList("Type");
		ListValue mountList = pack.newList("Mount");
		try {
			FileSystem fsm = os.getFileSystem();
			List<OSFileStore> fst = fsm.getFileStores();
			for (int i = 0; i < fst.size(); i++) {
				long used = 0, free = 0, total = 0;
				float usage = 0;

				try {
					String mount = fst.get(i).getMount();
					try {
						total = fst.get(i).getTotalSpace();
						free = fst.get(i).getFreeSpace();
						used = total - free;
						usage = ((float) total - free) / total * 100.0f;
					} catch (Exception e) {
						Logger.println("A160", 300, "disk:" + mount + ", err:" + e.getMessage());
					}

					if (conf.disk_ignore_names.hasKey(mount))
						continue;

					if (conf.disk_ignore_size_gb < total / 1024 / 1024 / 1024)
						continue;

					totalList.add(total*1024);
					usedList.add(used*1024);
					freeList.add(free*1024);
					pctList.add(usage);
					typeList.add(fst.get(i).getType());
					deviceList.add(fst.get(i).getVolume());
					mountList.add(mount);

				} catch (Exception e) {
					used = used = total = 0;
					usage = 0;
				}
			}

		} catch (Exception e) {
		}
		return pack;
	}
	public static void main(String[] args) {
	  new DiskPerf().usage(null);
	}
}
