/*
 *  Copyright 2015 LG CNS.
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
 */
package scouter.server.db.io.zip;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import scouter.server.ConfObserver;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.util.CacheTable;
import scouter.util.CompressUtil;
import scouter.util.FileUtil;
import scouter.util.IShutdown;
import scouter.util.LinkedMap;
import scouter.util.StopWatch;
public class IOChannel implements IShutdown {
	private static IOChannel instance = null;
	public final static synchronized IOChannel getInstance() {
		if (instance == null) {
			instance = new IOChannel();
		}
		return instance;
	}
	public IOChannel() {
		ConfObserver.put(IOChannel.class.getName(), new Runnable() {
			public void run() {
					readCache.setMaxRow(conf.gzip_read_cache_block);
			}
		});
	}
	private Configure conf = Configure.getInstance();
	private LinkedMap<String, CountBoard> headers = new LinkedMap<String, CountBoard>();
	public Block getLastWriteBlock(String date) throws IOException {
		CountBoard uc = headers.get(date);
		if (uc == null) {
			check();
			uc = new CountBoard(date);
			headers.put(date, uc);
		}
		long n = uc.getCount();
		int start = (int) (n % BKUtil.BLOCK_MAX_SIZE);
		Block bk = new Block(date, new byte[128], start, start, BKUtil.BLOCK_MAX_SIZE);
		bk.blockNum = (int) (n / BKUtil.BLOCK_MAX_SIZE);
		return bk;
	}
	private void check() {
		while (headers.size() >= conf.gzip_unitcount_header_cache - 1) {
			try {
				headers.removeFirst().close();
			} catch (Exception e) {
			}
		}
	}
	public CountBoard getCountBoard(String date) {
		CountBoard uc = headers.get(date);
		if (uc == null) {
			check();
			try {
				uc = new CountBoard(date);
			} catch (IOException e) {
				e.printStackTrace();
			}
			headers.put(date, uc);
		}
		return uc;
	}
	public synchronized void store(Block bk) {
		if (bk.dirty == false)
			return;
		// Logger.println("S129","Store " + bk);
		bk.dirty = false;
		int mgtime = 0;
		StopWatch w = new StopWatch();
		if (bk.START > 0) {
			StopWatch w2 = new StopWatch();
			Block old = getReadBlock(bk.date, bk.blockNum);
			if (old != null) {
				bk = bk.merge(old);
				readCache.put(new BKey(bk.date, bk.blockNum), bk, conf.gzip_read_cache_time);
			}
			mgtime = (int) w2.getTime();
		}
		getCountBoard(bk.date).set(bk.getOffset());
		try {
			byte[] org = bk.getBlockBytes();
			String date = bk.date;
			int blockNum = bk.blockNum;
			byte[] out = CompressUtil.doZip(org);
			FileUtil.save(getFile(date, blockNum), out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		long tm = w.getTime();
		if (tm > 1000) {
			Logger.println("S130", "Store " + tm + " ms " + (mgtime > 0 ? " old-load=" + mgtime + "ms" : ""));
		}
	}
	private File getFile(String date, int blockNum) {
		String filename = (BKUtil.createPath(date) + "/xlog." + blockNum);
		return new File(filename);
	}
	private CacheTable<BKey, Block> readCache = new CacheTable<BKey, Block>().setMaxRow(conf.gzip_read_cache_block);
 
	public Block getReadBlock(String date, int blockNum) {
		Block b = readCache.get(new BKey(date, blockNum));
		if (b != null)
			return b;
		File f = getFile(date, blockNum);
		if (f.exists() == false)
			return null;
		try {
			byte[] gz = FileUtil.readAll(f);
			gz = CompressUtil.unZip(gz);
			Block bk = new Block(date, gz, 0, gz.length, BKUtil.BLOCK_MAX_SIZE);
			bk.blockNum = blockNum;
			readCache.put(new BKey(date, blockNum), bk, conf.gzip_read_cache_time);
			return bk;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	public void shutdown() {
	}
	public void close(String date) {
		try {
			Enumeration<BKey> en = readCache.keys();
			while (en.hasMoreElements()) {
				BKey k = en.nextElement();
				if (date.equals(k.date)) {
					readCache.remove(k);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
