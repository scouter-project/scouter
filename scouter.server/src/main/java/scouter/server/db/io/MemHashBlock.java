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

package scouter.server.db.io;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.io.FlushCtr;
import scouter.io.IFlushable;
import scouter.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MemHashBlock implements IFlushable {
	private final static int _countPos = 4;
	private final static int _memHeadReserved = 1024;
	private final static int _keyLength = 5;
	
	protected File file;
	protected byte[] memBuffer;
	protected int memBufferSize;

	protected String path;
	protected int count;
	protected int capacity;

	public MemHashBlock(String path, int memSize) throws IOException {
		open(path, memSize);
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


	private void open(String path, int memSize) throws FileNotFoundException, IOException {
		this.path = path;
		this.memBufferSize = memSize;
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

		this.capacity = (int) (memBufferSize / _keyLength);
		
		FlushCtr.getInstance().regist(this);
	}

	private int _offset(int keyHash) {
		int bucketPos = (keyHash & Integer.MAX_VALUE) % capacity;
		return _keyLength * bucketPos + _memHeadReserved;
	}

	public synchronized long get(int keyHash) throws IOException {
		int pos = _offset(keyHash);
		return DataInputX.toLong5(this.memBuffer, pos);
	}

	public int getCount() {
		return count;
	}
	
	private int addCount(int n) throws IOException {
		count += n;
		DataOutputX.set(this.memBuffer, _countPos, DataOutputX.toBytes(count));
		return count;
	}

	public synchronized void put(int keyHash, long value) throws IOException {

		byte[] buffer = DataOutputX.toBytes5(value);
		int pos = _offset(keyHash);

		if ( DataInputX.toLong5(this.memBuffer, pos) == 0) {
			addCount(1);
		}
		
		System.arraycopy(buffer, 0, this.memBuffer, pos, _keyLength);
		
		this.dirty=true;
	}

	public void close() {
		FlushCtr.getInstance().unregist(this);
	}
}