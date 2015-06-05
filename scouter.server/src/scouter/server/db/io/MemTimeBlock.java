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

package scouter.server.db.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.io.FlushCtr;
import scouter.io.IFlushable;
import scouter.util.DateUtil;
import scouter.util.FileUtil;

public class MemTimeBlock implements IFlushable {
	private final static int _countPos = 4;
	protected File file;

	protected final static int _memHeadReserved = 1024;
	protected final static int _keyLength = 5;
	protected int memBufferSize = 3600 * 24 * _keyLength;
	protected byte[] memBuffer;

	protected String path;
	protected final int capacity = 3600 * 24;
	private int count;

	public MemTimeBlock(String path) throws IOException {
		open(path);
	}

	private void open(String path) throws FileNotFoundException, IOException {
			this.path = path;
		this.file = new File(this.path + ".hfile");
		boolean isNew = this.file.exists() == false || this.file.length() < _memHeadReserved;

		if (isNew) {
			this.memBuffer = new byte[_memHeadReserved + memBufferSize];
			this.memBuffer[0] = (byte) 0xCA;
			this.memBuffer[1] = (byte) 0xFE;
		} else {
			this.memBufferSize = (int) (this.file.length() - _memHeadReserved);
			this.memBuffer = FileUtil.readAll(this.file);
			this.count = DataInputX.toInt(this.memBuffer, _countPos);
		}
	
		FlushCtr.getInstance().regist(this);
	}

	private int _offset(long time) {
		int seconds = (int) (DateUtil.getDateMillis(time) / 1000);
		int hash = (seconds & Integer.MAX_VALUE) % capacity;
		return _keyLength * hash + _memHeadReserved;
	}

	
	public synchronized void flush() {
		FileUtil.save(this.file, this.memBuffer);
		this.dirty = false;
	}

	public long interval() {
		return 4000;
	}

	private boolean dirty;

	public boolean isDirty() {
		return dirty;
	}
	

	public synchronized long get(long time) throws IOException {
		int pos = _offset(time);
		return DataInputX.toLong5(this.memBuffer, pos);
	}

	public int getCount() {
		return count;
	}

	public int addCount(int n) {
		count += n;
		DataOutputX.set(this.memBuffer, _countPos, DataOutputX.toBytes(count));
		return count;
	}

	public synchronized void put(long time, long value) throws IOException {

		byte[] buffer = DataOutputX.toBytes5(value);
		int pos = _offset(time);

		if ( DataInputX.toLong5(this.memBuffer, pos) == 0) {
			addCount(1);
		}
		
		System.arraycopy(buffer, 0, this.memBuffer, pos, _keyLength);
		
		this.dirty = true;
	}

	public void close() {
		FlushCtr.getInstance().unregist(this);
	}

}