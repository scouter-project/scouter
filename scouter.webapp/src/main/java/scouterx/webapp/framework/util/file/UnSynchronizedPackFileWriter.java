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

package scouterx.webapp.framework.util.file;

import lombok.AccessLevel;
import lombok.Getter;
import scouter.io.DataOutputX;
import scouter.lang.pack.Pack;
import scouter.net.TcpFlag;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2017. 8. 30.
 * Byte File
 */
@Getter
public class UnSynchronizedPackFileWriter implements Closeable, Flushable {
	private String fileName;
	private long offset;

	@Getter(AccessLevel.NONE)
	private DataOutputX out;

	public UnSynchronizedPackFileWriter(String fileName) throws FileNotFoundException {
		this.fileName = fileName;
		out = new DataOutputX(new BufferedOutputStream(new FileOutputStream(this.fileName), 8192));
	}

	public long writePack(Pack pack) throws IOException {
		out.writeByte(TcpFlag.HasNEXT);
		pack.write(out);
		return offset;
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.flush();
		out.close();
	}
}
