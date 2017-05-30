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
 */
package scouter.server.db.io.zip;
import java.io.IOException;
import java.util.Enumeration;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.ShutdownManager;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.IClose;
import scouter.util.IShutdown;
import scouter.util.LinkedMap;
import scouter.util.ThreadUtil;
public class GZipStore extends Thread implements IClose, IShutdown {
	private static GZipStore instance = null;
	public final static synchronized GZipStore getInstance() {
		if (instance == null) {
			instance = new GZipStore();
			ShutdownManager.add(instance);
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}
	public GZipStore() {
		Logger.println("S128", "COMPRESS MODE ENABLED");
	}
	public static class Key {
		public String date;
		public int unitNum;
		public Key(String date, int unitNum) {
			this.date = date;
			this.unitNum = unitNum;
		}
		@Override
		public int hashCode() {
			return unitNum ^ date.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Key) {
				Key o = (Key) obj;
				return this.unitNum == o.unitNum && this.date.equals(o.date);
			}
			return false;
		}
	}
	LinkedMap<String, Block> writingBlockTable = new LinkedMap();
	private boolean brun = true;
	@Override
	public void run() {
		while (brun) {
			long now = System.currentTimeMillis();
			Enumeration<String> en = writingBlockTable.keys();
			while (en.hasMoreElements()) {
				String key = en.nextElement();
				Block bk = writingBlockTable.get(key);
				if (bk == null)
					continue;
				if (now > bk.lastAccessTime + 10000 && bk.dirty) {
					IOChannel.getInstance().store(bk);
					bk.lastAccessTime = System.currentTimeMillis();
				}
			}
			for (int i = 0; i < 100 && brun; i++) {
				ThreadUtil.sleep(100);
			}
		}
	}
	private Configure conf = Configure.getInstance();
	public synchronized long write(String date, byte[] data) throws IOException {
		return write(date, data, 0);
	}
	public synchronized long write(String date, byte[] data, long next) throws IOException {
		DataOutputX dout = new DataOutputX();
		dout.writeLong5(next);
		dout.writeInt3(data.length);
		dout.write(data);
		byte[] saveData = dout.toByteArray();
		Block bk = (Block) writingBlockTable.get(date);
		if (bk == null) {
			bk = IOChannel.getInstance().getLastWriteBlock(date);
			if (bk != null) {
				while (writingBlockTable.size() >= conf._compress_write_buffer_block_count) {
					Block bb = writingBlockTable.removeFirst();
					IOChannel.getInstance().store(bb);
				}
				writingBlockTable.put(date, bk);
			} else {
				Logger.println("ERROR -1 : write main data");
				return -1;
			}
		}
		bk.lastAccessTime = System.currentTimeMillis();
		try {
			long pos = bk.getOffset();
			boolean ok = bk.write(saveData);
			if (ok) {
				return pos;
			}
			IOChannel.getInstance().store(bk);
			bk = bk.createNextBlock();
			writingBlockTable.put(date, bk);
			pos = bk.getOffset();
			ok = bk.write(saveData);
			return ok ? pos : -1;
		} catch (Throwable ee) {
			Logger.println("S201", 5, ee.toString() + " => " + bk, ee);
			return -1;
		}
	}
	private Block getWritingBlock(String date, int blockNum, long pos) {
		Block b = writingBlockTable.get(date);
		if (b == null)
			return null;
		if (b.blockNum == blockNum && b.readable(pos))
			return b;
		return null;
	}
	public byte[] read(String date, long pos) {
		if (pos < 0)
			return null;
		DataOutputX out = new DataOutputX();
		while (true) {
			int blockNum = (int) (pos / GZipCtr.BLOCK_MAX_SIZE);
			Block bk = getWritingBlock(date, blockNum, pos);
			if (bk == null) {
				bk = IOChannel.getInstance().getReadBlock(date, blockNum);
			}
			if (bk == null) {
				return out.toByteArray();
			}
			try {
				long next = DataInputX.toLong5(bk.read(pos, 5), 0);
				int len = DataInputX.toInt3(bk.read(pos + 5, 3), 0);
				byte[] record = bk.read(pos + 8, len);
				out.write(record);
				if (next <= 0)
					return out.toByteArray();
				else
					pos = next;
			} catch (NullPointerException ne) {
				// bk가 end 도달시 null
				return out.toByteArray();
			} catch (Throwable t) {
				t.printStackTrace();
				return out.toByteArray();
			}
		}
	}
	public void flush() {
		if (writingBlockTable.size() == 0)
			return;
		LinkedMap<String, Block> blocks = writingBlockTable;
		writingBlockTable = new LinkedMap();
		while (blocks.size() > 0) {
			Block bk = blocks.removeFirst();
			IOChannel.getInstance().store(bk);
		}
	}
	public void shutdown() {
		this.brun = false;
		close();
	}
	public void close() {
		this.flush();
	}
	public void close(String date) {
		Block bb = writingBlockTable.remove(date);
		IOChannel.getInstance().store(bb);
		IOChannel.getInstance().close(date);
	}
	public static void main(String[] args) {
		LinkedMap<String, String> m = new LinkedMap<String, String>();
		m.put("a", "a");
		m.put("b", "b");
		m.put("c", "c");
		Enumeration<String> en = m.keys();
		while (en.hasMoreElements()) {
			String n = en.nextElement();
			if (n.equals("b")) {
				m.remove(n);
			}
			System.out.println(m);
		}
	}
}
