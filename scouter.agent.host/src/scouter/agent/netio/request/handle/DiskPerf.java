package scouter.agent.netio.request.handle;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemMap;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.NfsFileSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;

import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.FormatUtil;

public class DiskPerf {

	public void output(FileSystem fs) throws SigarException {
		//
		// ArrayList items = new ArrayList();
		//
		// items.add(fs.getDevName());
		// items.add(formatSize(total));
		// items.add(formatSize(used));
		// items.add(formatSize(avail));
		// items.add(usePct);
		// items.add(fs.getDirName());
		// items.add(fs.getSysTypeName() + "/" + fs.getTypeName());

	}

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
			Sigar sigar = new Sigar();
			SigarProxy proxy = SigarProxyCache.newInstance(sigar);
			//FileSystemMap mounts = proxy.getFileSystemMap();
			FileSystem[] fslist = proxy.getFileSystemList();
			for (int i = 0; i < fslist.length; i++) {

				long used, avail, total;
				float pct;

				FileSystem fs = fslist[i];
				try {
					FileSystemUsage usage;
					if (fs instanceof NfsFileSystem) {
						NfsFileSystem nfs = (NfsFileSystem) fs;
						if (!nfs.ping()) {
							continue;
						}
					}
					usage = sigar.getFileSystemUsage(fs.getDirName());
					used = usage.getTotal() - usage.getFree();
					avail = usage.getAvail();
					total = usage.getTotal();
					pct = (float) (usage.getUsePercent() * 100);

					totalList.add(total*1024);
					usedList.add(used*1024);
					freeList.add(avail*1024);
					pctList.add(pct);
					typeList.add(fs.getSysTypeName() + "/" + fs.getTypeName());
					deviceList.add(fs.getDevName());
					mountList.add(fs.getDirName());
				} catch (SigarException e) {
					used = avail = total = 0;
					pct = 0;
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
