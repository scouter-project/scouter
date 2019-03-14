/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.lang.pack;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.Hexa32;

import java.io.IOException;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 27/10/2018
 */
public class SpanContainerPack implements Pack {

	public long gxid;
	public int spanCount;
	public long timestamp;

	public byte[] spans;

	public byte getPackType() {
		return PackEnum.SPAN_CONTAINER;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SpanContainerPack ");
		sb.append(" gxid=").append(Hexa32.toString32(gxid));
		sb.append(" spanCount=").append(Hexa32.toString32(spanCount));
		return sb.toString();
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeLong(gxid);
		dout.writeDecimal(spanCount);
		dout.writeLong(timestamp);
		dout.writeBlob(spans);
	}

	public Pack read(DataInputX din) throws IOException {
		this.gxid = din.readLong();
		this.spanCount = (int) din.readDecimal();
		this.timestamp = din.readLong();
		this.spans = din.readBlob();

		return this;
	}

}