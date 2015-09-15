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

package scouter.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import scouter.lang.pack.Pack;
import scouter.lang.step.Step;
import scouter.lang.value.NullValue;
import scouter.lang.value.Value;

public class DataOutputX {
	public final static int INT3_MIN_VALUE = 0xff800000;
	public final static int INT3_MAX_VALUE = 0x007fffff;
	public final static long LONG5_MIN_VALUE = 0xffffff8000000000L;
	public final static long LONG5_MAX_VALUE = 0x0000007fffffffffL;

	private int written;
	private DataOutput inner;
	private ByteArrayOutputStream bout;

	public DataOutputX(int size) {
		this(new ByteArrayOutputStream(size));
	}

	public DataOutputX() {
		this(new ByteArrayOutputStream());
	}

	public byte[] toByteArray() {
		if (this.bout != null)
			return bout.toByteArray();
		else
			return null;
	}

	public DataOutputX(ByteArrayOutputStream byteout) {
		this.bout = byteout;
		this.inner = new DataOutputStream(byteout);
	}

	public DataOutputX(BufferedOutputStream anyout) {
		this.inner = new DataOutputStream(anyout);
	}

	public DataOutputX(DataOutputStream out) {
		this.inner = out;
	}

	public DataOutputX(RandomAccessFile out) {
		this.inner = out;
	}
	public DataOutputX writeIntBytes(byte[] b) throws IOException {
		this.writeInt(b.length);
		this.write(b);
		return this;

	}
	public DataOutputX writeShortBytes(byte[] b) throws IOException {
		this.writeShort(b.length);
		this.write(b);
		return this;

	}

	// public DataOutputX flushShortBytes(byte[] b) throws IOException {
	// this.writeShortBytes(b);
	// this.flush();
	// return this;
	// }
	public DataOutputX writeBlob(byte[] value, int offset, int length) throws IOException {
		if (value == null || value.length == 0) {
			writeByte((byte) 0);
		} else {
			int len = Math.min(length, value.length - offset);
			if (len <= 253) {
				writeByte((byte) len);
				write(value, offset, len);
			} else if (len <= 65535) {
				writeByte((byte) 255);
				writeShort((short) len);
				write(value, offset, len);
			} else {
				writeByte((byte) 254);
				writeInt(len);
				write(value, offset, len);
			}
		}
		return this;
	}

	public DataOutputX writeBlob(byte[] value) throws IOException {
		if (value == null || value.length == 0) {
			writeByte((byte) 0);
		} else {
			int len = value.length;
			if (len <= 253) {
				writeByte((byte) len);
				write(value);
			} else if (len <= 65535) {
				writeByte((byte) 255);
				writeShort((short) len);
				write(value);
			} else {
				writeByte((byte) 254);
				writeInt(len);
				write(value);
			}
		}
		return this;
	}

	public DataOutputX writeText(String s) throws IOException {
		if (s == null) {
			writeByte((byte) 0);
		} else {
			writeBlob(s.getBytes("UTF8"));
		}
		return this;
	}

	public DataOutputX writeText(StringBuffer s) throws IOException {
		if (s == null) {
			writeByte((byte) 0);
		} else {
			writeBlob(s.toString().getBytes("UTF8"));
		}
		return this;
	}

	public DataOutputX writeInt3(int v) throws IOException {
		write(toBytes3(v), 0, 3);
		return this;
	}

	public DataOutputX writeLong5(long v) throws IOException {
		write(toBytes5(v), 0, 5);
		return this;
	}

	public DataOutputX writeDecimal(long v) throws IOException {
		if (v == 0) {
			writeByte(0);
		} else if (Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE) {
			writeByte(1);
			writeByte((byte) v);
		} else if (Short.MIN_VALUE <= v && v <= Short.MAX_VALUE) {
			writeByte(2);
			writeShort((short) v);
		} else if (INT3_MIN_VALUE <= v && v <= INT3_MAX_VALUE) {
			writeByte(3);
			writeInt3((int) v);
		} else if (Integer.MIN_VALUE <= v && v <= Integer.MAX_VALUE) {
			writeByte(4);
			writeInt((int) v);
		} else if (LONG5_MIN_VALUE <= v && v <= LONG5_MAX_VALUE) {
			writeByte(5);
			writeLong5(v);
		} else if (Long.MIN_VALUE <= v && v <= Long.MAX_VALUE) {
			writeByte(8);
			writeLong(v);
		}
		return this;
	}

	public DataOutputX wrttenDecimal(long v) throws IOException {
		this.writeDecimal(v);
		this.flush();
		return this;
	}

	public static byte[] toBytes(short v) {
		byte buf[] = new byte[2];
		buf[0] = (byte) ((v >>> 8) & 0xFF);
		buf[1] = (byte) ((v >>> 0) & 0xFF);
		return buf;
	}

	public static byte[] toBytes(int v) {
		byte buf[] = new byte[4];
		buf[0] = (byte) ((v >>> 24) & 0xFF);
		buf[1] = (byte) ((v >>> 16) & 0xFF);
		buf[2] = (byte) ((v >>> 8) & 0xFF);
		buf[3] = (byte) ((v >>> 0) & 0xFF);
		return buf;
	}

	public static byte[] toBytes3(int v) {
		byte buf[] = new byte[3];
		buf[0] = (byte) ((v >>> 16) & 0xFF);
		buf[1] = (byte) ((v >>> 8) & 0xFF);
		buf[2] = (byte) ((v >>> 0) & 0xFF);
		return buf;
	}

	public static byte[] toBytes(long v) {
		byte buf[] = new byte[8];
		buf[0] = (byte) (v >>> 56);
		buf[1] = (byte) (v >>> 48);
		buf[2] = (byte) (v >>> 40);
		buf[3] = (byte) (v >>> 32);
		buf[4] = (byte) (v >>> 24);
		buf[5] = (byte) (v >>> 16);
		buf[6] = (byte) (v >>> 8);
		buf[7] = (byte) (v >>> 0);
		return buf;
	}

	public static byte[] toBytes5(long v) {
		byte writeBuffer[] = new byte[5];
		writeBuffer[0] = (byte) (v >>> 32);
		writeBuffer[1] = (byte) (v >>> 24);
		writeBuffer[2] = (byte) (v >>> 16);
		writeBuffer[3] = (byte) (v >>> 8);
		writeBuffer[4] = (byte) (v >>> 0);
		return writeBuffer;
	}

	public static byte[] toBytes(boolean b) {
		if (b)
			return new byte[] { 1 };
		else
			return new byte[] { 0 };
	}

	public static byte[] toBytes(float v) {
		return toBytes(Float.floatToIntBits(v));
	}

	public static byte[] toBytes(double v) {
		return toBytes(Double.doubleToLongBits(v));
	}

	public static byte[] set(byte[] dest, int pos, byte[] src) {
		System.arraycopy(src, 0, dest, pos, src.length);
		return dest;
	}

	public DataOutputX writeDecimalArray(int[] v) throws IOException {
		if (v == null) {
			writeDecimal(0);
		} else {
			writeDecimal(v.length);
			for (int i = 0; i < v.length; i++) {
				writeDecimal(v[i]);
			}
		}
		return this;
	}

	public DataOutputX writeDecimalArray(long[] v) throws IOException {
		if (v == null) {
			writeDecimal(0);
		} else {
			writeDecimal(v.length);
			for (int i = 0; i < v.length; i++) {
				writeDecimal(v[i]);
			}
		}
		return this;
	}

	public DataOutputX writeArray(long[] v) throws IOException {
		if (v == null) {
			writeShort(0);
		} else {
			writeShort(v.length);
			for (int i = 0; i < v.length; i++) {
				writeLong(v[i]);
			}
		}
		return this;
	}

	public DataOutputX writeArray(int[] v) throws IOException {
		if (v == null) {
			writeShort(0);
		} else {
			writeShort(v.length);
			for (int i = 0; i < v.length; i++) {
				writeInt(v[i]);
			}
		}
		return this;
	}

	public DataOutputX writeArray(float[] v) throws IOException {
		if (v == null) {
			writeShort(0);
		} else {
			writeShort(v.length);
			for (int i = 0; i < v.length; i++) {
				writeFloat(v[i]);
			}
		}
		return this;
	}

	

	public DataOutputX writeValue(Value value) throws IOException {
		if (value == null)
			value = NullValue.value;
		this.writeByte(value.getValueType());
		value.write(this);
		return this;
	}

	

	public DataOutputX writeStep(Step step) throws IOException {
		this.writeByte(step.getStepType());
		step.write(this);
		return this;
	}

	
	public DataOutputX writePack(Pack packet) throws IOException {
		this.writeByte(packet.getPackType());
		packet.write(this);
		return this;
	}

	public DataOutputX write(byte[] b) throws IOException {
		this.written += b.length;
		this.inner.write(b);
		return this;
	}

	public DataOutputX write(byte[] b, int off, int len) throws IOException {
		this.written += len;
		this.inner.write(b, off, len);
		return this;
	}

	public DataOutputX writeBoolean(boolean v) throws IOException {
		this.written++;
		this.inner.writeBoolean(v);
		return this;
	}

	public DataOutputX writeByte(int v) throws IOException {
		this.written++;
		this.inner.writeByte(v);
		return this;
	}

	public DataOutputX writeShort(int v) throws IOException {
		this.written += 2;
		this.inner.writeShort(v);
		return this;
	}

	public DataOutputX writeChar(int v) throws IOException {
		this.written += 2;
		this.inner.writeChar(v);
		return this;
	}

	public DataOutputX writeInt(int v) throws IOException {
		this.written += 4;
		this.inner.writeInt(v);
		return this;
	}

	public DataOutputX writeLong(long v) throws IOException {
		this.written += 8;
		this.inner.writeLong(v);
		return this;
	}

	public DataOutputX writeFloat(float v) throws IOException {
		this.written += 4;
		this.inner.writeFloat(v);
		return this;
	}

	public DataOutputX writeDouble(double v) throws IOException {
		this.written += 8;
		this.inner.writeDouble(v);
		return this;
	}

	public int size() {
		return this.written;
	}

	public int getWriteSize() {
		return this.written;
	}

	public void close() throws IOException {
		if (this.inner instanceof RandomAccessFile) {
			((RandomAccessFile) this.inner).close();
		} else if (this.inner instanceof OutputStream) {
			((OutputStream) this.inner).close();
		}
	}

	public void flush() throws IOException {
		if (this.inner instanceof OutputStream) {
			((OutputStream) this.inner).flush();
		}
	}

	public static void main(String[] args) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputX dout = new DataOutputX(out);
		dout.writeInt(100);
		// dout.flush();
		byte[] b = ((ByteArrayOutputStream) out).toByteArray();
		DataInputX din = new DataInputX(b);
		System.out.println(din.readInt());
	}
}