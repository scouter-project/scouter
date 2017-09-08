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

package scouter.lang.pack;



import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.Hexa32;

import java.io.IOException;



public class TextPack implements Pack {
	public String xtype;
	public int hash;
	public String text;

	public TextPack() {
	}

	public TextPack(String xtype, int hash, String text) {
		this.xtype = xtype;
		this.hash = hash;
		this.text = text;
	}

	public byte getPackType() {
		return PackEnum.TEXT;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(xtype);
		sb.append(" ").append(Hexa32.toString32(hash));
		sb.append(" ").append(text);
		return sb.toString();
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeText(xtype);
		dout.writeInt(hash);
		dout.writeText(text);
	}

	public static void writeDirect(DataOutputX dout, byte packType, String _xtype, int _hash, String _text) throws IOException {
		dout.writeByte(packType);
		dout.writeText(_xtype);
		dout.writeInt(_hash);
		dout.writeText(_text);
	}

	public Pack read(DataInputX din) throws IOException {
		this.xtype = din.readText();
		this.hash = din.readInt();
		this.text = din.readText();
		return this;
	}

}
