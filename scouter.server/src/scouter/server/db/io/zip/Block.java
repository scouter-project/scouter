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


public class Block {

	public String date;

	public int blockNum;

	private byte buf[];

	public final int START;
	public int END;
	public final int MAX;

	public boolean dirty;

	public long lastAccessTime;

	public Block(String date) {
		this(date, GZipCtr.BLOCK_MAX_SIZE);
	}

	public Block(String date, int max) {
		this(date, new byte[128], 0, 0, max);
	}

	public Block(String date, byte[] buf, int max) {
		this(date, buf, 0, buf.length, max);
	}

	public Block(String date, byte[] buf, int start, int end, int max) {
		this.date = date;
		this.buf = buf;
		this.START = start;
		this.END = end;
		this.MAX = max;
	}

	private void ensureCapacity(int minCapacity) {
		if (minCapacity > buf.length) {
			int oldCapacity = buf.length;
			int newCapacity = oldCapacity << 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			newCapacity = Math.min(newCapacity, MAX);
			buf = copyOf(buf, newCapacity);
		}
	}
	
	private static byte[] copyOf(byte[] org, int length) {
		byte[] copy = new byte[length];
		System.arraycopy(org, 0, copy, 0, Math.min(org.length, length));
		return copy;
	 }

	public boolean write(byte[] b) {
		return write(b, 0, b.length);
	}

	public synchronized boolean write(byte[] b, int offset, int len) {
		if (END + len > MAX)
			return false;
		ensureCapacity(END - START + len);
		System.arraycopy(b, offset, buf, END - START, len);
		END += len;
		this.dirty=true;
		return true;
	}

	public synchronized byte[] read(long pos, int len) {
		if(len<=0)
			return null;
		int bpos = (int) (pos - (blockNum * GZipCtr.BLOCK_MAX_SIZE));

		if (len + bpos > END)
			return null;
		if (bpos < START)
			return null;

		byte[] out = new byte[len];

		System.arraycopy(this.buf, bpos - START, out, 0, len);
		return out;
	}

	public long getOffset() {
		return (long) END + ((long) blockNum) * GZipCtr.BLOCK_MAX_SIZE;
	}

	public byte[] getBlockBytes() {
		byte[] out = new byte[this.END-this.START];
		System.arraycopy(this.buf, 0, out, 0, this.END-this.START);
		return out;
	}

	public Block createNextBlock() {
		Block bk = new Block(date, MAX);
		bk.blockNum = this.blockNum + 1;
		return bk;
	}

	public boolean readable(long pos) {
		int bpos = (int) (pos - (blockNum * GZipCtr.BLOCK_MAX_SIZE));

		if (8 + bpos > END)
			return false;
		if (bpos < START)
			return false;
		return true;
	}

	public Block merge(Block old) {
		int start = Math.min(this.START, old.START);
		int end = Math.max(this.END, old.END);
		byte[] block = new byte[end - start];
		System.arraycopy(this.buf, 0, block, this.START -start, this.END - this.START);
		System.arraycopy(old.buf, 0, block, old.START - start, old.END - old.START);
		
		Block b= new Block(this.date, block, start, end, this.MAX);
		b.blockNum=this.blockNum;
		return b;
	}

	@Override
	public String toString() {
		return "Block [date=" + date + ", blockNum=" + blockNum + ", START=" + START
				+ ", END=" + END + ", MAX=" + MAX + ", dirty=" + dirty + "]";
	}
	
}