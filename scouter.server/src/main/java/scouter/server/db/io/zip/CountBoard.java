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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import scouter.util.FileUtil;
import scouter.util.IClose;

public class CountBoard implements IClose {

	private RandomAccessFile raf = null;

	public long counts;
	protected File file;

	public CountBoard(String date) throws IOException {
		String filename = (GZipCtr.createPath(date) + "/count.dat");
		this.file = new File(filename);
		this.raf = new RandomAccessFile(file, "rw");

		if (this.raf.length() >=8) {
			try {
				load();
			} catch (IOException e) {
				counts = 0;
				e.printStackTrace();
			}
		} else {
			counts = 0;
		}
	}

	public long add(long cnt) {
		return set(this.counts + cnt);
	}

	public long set(long cnt) {
		this.counts = cnt;
		try {
			raf.seek(0);
			raf.writeLong(counts);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this.counts;
	}

	private void load() throws IOException {
		raf.seek(0);
		counts = raf.readLong();
	}

	public long getCount() {
		return this.counts;
	}

	public void close() {
		FileUtil.close(this.raf);
		this.raf=null;
	}
}