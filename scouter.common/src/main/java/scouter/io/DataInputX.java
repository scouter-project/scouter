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

package scouter.io;

import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PackEnum;
import scouter.lang.step.Step;
import scouter.lang.step.StepEnum;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class DataInputX {
	public static Long LONG5_MAX_VALUE = Integer.MAX_VALUE * 256L;

	private int offset;
	private DataInput inner;
	private DataInputStream din;

	public DataInputX(byte[] buff) {
		this(new ByteArrayInputStream(buff));
	}

	public DataInputX(byte[] buff, int offset) {
		this(new ByteArrayInputStream(buff, offset, buff.length - offset));
	}

	public DataInputX(ByteArrayInputStream in) {
		this(new DataInputStream(in));
	}

	public DataInputX(BufferedInputStream in) {
		this.din = new DataInputStream(in);
		this.inner = this.din;
	}

	public DataInputX(DataInputStream in) {
		this.inner = in;
		this.din = in;
	}

	public DataInputX(RandomAccessFile in) {
		this.inner = in;
	}

	public byte[] readIntBytes() throws IOException {
		int len = readInt();
		return read(len);
	}
	public byte[] read(int len) throws IOException {
		offset += len;
		byte[] buff = new byte[len];
		this.inner.readFully(buff);
		return buff;
	}

	public byte[] readShortBytes() throws IOException {
		int len = readUnsignedShort();
		offset += len;
		byte[] buff = new byte[len];
		this.inner.readFully(buff);
		return buff;
	}

	public byte[] readBlob() throws IOException {
		int baselen = readUnsignedByte();
		switch (baselen) {
		case 255: {
			int len = readUnsignedShort();
			byte[] buffer = read(len);
			return buffer;
		}
		case 254: {
			int len = this.readInt();
			byte[] buffer = read(len);
			return buffer;
		}
		case 0: {
			return new byte[0];
		}
		default:
			byte[] buffer = read(baselen);
			return buffer;
		}
	}
	
	public int readInt3() throws IOException {
		byte[] readBuffer = read(3);
		return toInt3(readBuffer, 0);
	}

	public long readLong5() throws IOException {
		byte[] readBuffer =read(5);
		return toLong5(readBuffer, 0);
	}

	public long readDecimal() throws IOException {
		byte len = readByte();
		switch (len) {
		case 0:
			return 0;
		case 1:
			return readByte();
		case 2:
			return readShort();
		case 3:
			return readInt3();
		case 4:
			return readInt();
		case 5:
			return readLong5();
		default:
			return readLong();
		}
	}

	public String readText() throws IOException {
		byte[] buffer = readBlob();
		return new String(buffer, "UTF8");
	}

	public static boolean toBoolean(byte[] buf, int pos) {
		return buf[pos]!=0;	
	}
	
	public static short toShort(byte[] buf, int pos) {
		int ch1 = buf[pos] & 0xff;
		int ch2 = buf[pos + 1] & 0xff;
		return (short) ((ch1 << 8) + (ch2 << 0));
	}

	public static int toInt3(byte[] buf, int pos) {
		int ch1 = buf[pos] & 0xff;
		int ch2 = buf[pos + 1] & 0xff;
		int ch3 = buf[pos + 2] & 0xff;

		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8)) >> 8;
	}
	public static int toInt(byte[] buf, int pos) {
		int ch1 = buf[pos] & 0xff;
		int ch2 = buf[pos + 1] & 0xff;
		int ch3 = buf[pos + 2] & 0xff;
		int ch4 = buf[pos + 3] & 0xff;
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	public static long toLong(byte[] buf, int pos) {
		return (((long) buf[pos] << 56)//
				+ ((long) (buf[pos + 1] & 255) << 48) //
				+ ((long) (buf[pos + 2] & 255) << 40) //
				+ ((long) (buf[pos + 3] & 255) << 32) //
				+ ((long) (buf[pos + 4] & 255) << 24) + ((buf[pos + 5] & 255) << 16) //
				+ ((buf[pos + 6] & 255) << 8) //
		+ ((buf[pos + 7] & 255) << 0));
	}

	public static long toLong5(byte[] buf, int pos) {
		return (((long) buf[pos] << 32) + //
				((long) (buf[pos + 1] & 255) << 24) + //
				((buf[pos + 2] & 255) << 16) + //
				((buf[pos + 3] & 255) << 8) + //
		((buf[pos + 4] & 255) << 0));
	}

	public static float toFloat(byte[] buf, int pos) {
		return Float.intBitsToFloat(toInt(buf, pos));
	}

	public static double toDouble(byte[] buf, int pos) {
		return Double.longBitsToDouble(toLong(buf, pos));
	}

	public static byte[] get(byte[] buf, int pos, int length) {
		byte[] out = new byte[length];
		System.arraycopy(buf, pos, out, 0, length);
		return out;
	}

	public int[] readDecimalArray(int[] data) throws IOException {
		int length = (int) readDecimal();
		data = new int[length];
		for (int i = 0; i < length; i++) {
			data[i] = (int) readDecimal();
		}
		return data;
	}

	public long[] readDecimalArray() throws IOException {
		int length = (int) readDecimal();
		long[] data = new long[length];
		for (int i = 0; i < length; i++) {
			data[i] = readDecimal();
		}
		return data;
	}

	public long[] readArray() throws IOException {
		return readArray(new long[0]);
	}

	public long[] readArray(long[] data) throws IOException {
		int length = readShort();
		data = new long[length];
		for (int i = 0; i < length; i++) {
			data[i] = readLong();
		}
		return data;
	}

	public int[] readArray(int[] data) throws IOException {
		int length = readShort();
		data = new int[length];
		for (int i = 0; i < length; i++) {
			data[i] = readInt();
		}
		return data;
	}

	public float[] readArray(float[] data) throws IOException {
		int length = readShort();
		data = new float[length];
		for (int i = 0; i < length; i++) {
			data[i] = readFloat();
		}
		return data;
	}

	public Value readValue() throws IOException {
		this.offset++;
		byte type = this.inner.readByte();
		return ValueEnum.create(type).read(this);
	}

	public Step readStep() throws IOException {
		this.offset++;
		byte type = this.inner.readByte();
		return StepEnum.create(type).read(this);
	}

	public Pack readPack() throws IOException {
		this.offset++;
		byte type = this.inner.readByte();
		return PackEnum.create(type).read(this);
	}
	public MapPack readMapPack() throws IOException {
		return (MapPack)readPack();
	}
	public void readFully(byte[] b) throws IOException {
		this.offset += b.length;
		this.inner.readFully(b);
	}

	public void readFully(byte[] b, int off, int len) throws IOException {
		this.offset += len;
		this.inner.readFully(b, off, len);
	}

	public int skipBytes(int n) throws IOException {
		this.offset += n;
		return this.inner.skipBytes(n);
	}

	public boolean readBoolean() throws IOException {
		this.offset += 1;
		return this.inner.readBoolean();
	}

	public byte readByte() throws IOException {
		this.offset += 1;
		return this.inner.readByte();
	}

	public int readUnsignedByte() throws IOException {
		this.offset += 1;
		return this.inner.readUnsignedByte();
	}

	public short readShort() throws IOException {
		this.offset += 2;
		return this.inner.readShort();
	}

	public int readUnsignedShort() throws IOException {
		this.offset += 2;
		return this.inner.readUnsignedShort();
	}

	public char readChar() throws IOException {
		this.offset += 2;
		return this.inner.readChar();
	}

	public int readInt() throws IOException {
		this.offset += 4;
		return this.inner.readInt();
	}

	public long readLong() throws IOException {
		this.offset += 8;
		return this.inner.readLong();
	}

	public float readFloat() throws IOException {
		this.offset += 4;
		return this.inner.readFloat();
	}

	public double readDouble() throws IOException {
		this.offset += 8;
		return this.inner.readDouble();
	}

	public int available() throws IOException {
		return this.din == null ? 0 : this.din.available();
	}

	public void close() throws IOException {
		if (this.inner instanceof RandomAccessFile) {
			((RandomAccessFile) this.inner).close();
		} else if (this.inner instanceof InputStream) {
			((InputStream) this.inner).close();
		}
	}

	public int getOffset() {
		return this.offset;
	}

	public static byte[] read(FileChannel channel, int len) throws IOException {
		byte[] buf = new byte[len];
		ByteBuffer dst = ByteBuffer.wrap(buf);
		channel.read(dst);
		return buf;
	}

	
}