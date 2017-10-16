/*
 *  Copyright 2015 the original author or authors.
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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import scouter.util.FileUtil;
import scouter.util.IClose;
import scouter.util.LongKeyLinkedMap;


public class BufferedRandomAccessX implements IClose {

	final private RandomAccessFile file;
	final private LongKeyLinkedMap<byte[]> buffer;
	final private int blockSize;
	private long fileLength = 0;

	public long readAccessCount;
	
	private FileChannel channel;

	public BufferedRandomAccessX(RandomAccessFile file) throws IOException {
		this(file, 8192, 1, false);
	}

	public BufferedRandomAccessX(RandomAccessFile file, boolean useNio) throws IOException {
		this(file, 8192, 1, useNio);
	}

	public BufferedRandomAccessX(RandomAccessFile file, int blockSize, int numberOfBlock) throws IOException {
		this(file, blockSize, numberOfBlock, false);
	}

	public BufferedRandomAccessX(RandomAccessFile file, int blockSize, int numberOfBlock, boolean useNio)
			throws IOException {
		this.file = file;
		this.buffer = new LongKeyLinkedMap<byte[]>().setMax(numberOfBlock);
		this.blockSize = blockSize;
		this.fileLength = file.length();
		if (useNio) {
			this.channel = this.file.getChannel();
		}

	}

	private long offset=0;;
	public synchronized byte[] read(long pos, int length) throws IOException {
	    if (pos >= this.fileLength)
		 return null;

	    this.offset = pos + length;

	  	DataOutputX out = new DataOutputX();
		int posOnBlock = (int) (pos % blockSize);
		if (posOnBlock > 0) {
			//long blockNum = (pos / blockSize);
			byte[] blockBytes = getReadBlock(pos / blockSize);
			if(blockBytes==null){
				return null;
			}
			if (blockBytes.length < blockSize) {
				int readLen = Math.min(blockBytes.length - posOnBlock, length);
				return DataInputX.get(blockBytes, posOnBlock, readLen);
			}

			int readLen = Math.min(blockSize - posOnBlock, length);
			out.write(DataInputX.get(blockBytes, posOnBlock, readLen));

			length -= readLen;
			pos += readLen;
		}

		int blockCount = length / blockSize;

		for (int i = 0; i < blockCount; i++) {
			//long blockNum = (int) (pos / blockSize);
			byte[] blockBytes = getReadBlock(pos / blockSize);
			if (blockBytes == null) {
				return out.toByteArray();
			}
			out.write(blockBytes);

			pos += blockSize;
			length -= blockSize;
		}

		if (length == 0) {
			return out.toByteArray();
		}

		int remainder = length;

	//	long blockNum = (int) (pos / blockSize);
		byte[] block = getReadBlock(pos / blockSize);
		if (block != null) {
			remainder = Math.min(block.length, remainder);
			out.write(DataInputX.get(block, 0, remainder));
		}

		return out.toByteArray();
	}

	public long getLength() throws IOException {
		if (this.channel == null) {
			return this.file.length();
		} else {
			return this.channel.size();
		}
	}

	public void append(byte[] data) throws IOException {
		this.write(this.fileLength, data);
	}

	public synchronized void write(long pos, byte[] data) throws IOException {
		if (data == null || data.length == 0)
			return;
		if (pos >= this.fileLength) {
			this.offset = this.fileLength + data.length;
			this.seek(this.fileLength);
			writeReal(data);
			return;
		}
		
		this.offset = pos + data.length;
		
		seek(pos);
		writeReal(data);
		// /////////////////

		int length = data.length;
		int offset = 0;

		int posOnBlock = (int) (pos % blockSize);
		if (posOnBlock > 0) {
			long blockNumber = (pos / blockSize);
			byte[] blockBytes = this.buffer.get(blockNumber);
			int writeLen = Math.min(blockSize - posOnBlock, length);
			if (blockBytes != null) {
				if (blockSize == blockBytes.length) {
					System.arraycopy(data, offset, blockBytes, posOnBlock, writeLen);
				} else {
					this.buffer.remove(blockNumber);
				}
			}
			length -= writeLen;
			pos += writeLen;
			offset += writeLen;
		}
		if (length == 0)
			return;
		int blockCount = length / blockSize;
		for (int i = 0; i < blockCount; i++) {
			long blockNumber = pos / blockSize;
			byte[] blockBytes = this.buffer.get(blockNumber);
			if (blockBytes != null) {
				if (blockSize == blockBytes.length) {
					System.arraycopy(data, offset, blockBytes, 0, blockSize);
				} else {
					this.buffer.remove(blockNumber);
				}
			}
			length -= blockSize;
			pos += blockSize;
			offset += blockSize;
		}
		int remainder = length;
		if (remainder == 0)
			return;
		if (remainder < 0) {
			throw new IOException("Write fail remainder=" + remainder + " pos=" + pos);
		}
		long blockNumber = pos / blockSize;
		byte[] blockBytes = this.buffer.get(blockNumber);
		if (blockBytes == null)
			return;
		if (blockSize == blockBytes.length) {
			System.arraycopy(data, offset, blockBytes, 0, remainder);
		} else {
			this.buffer.remove(blockNumber);
		}

	}

	private void writeReal(byte[] data) throws IOException {
		if (this.channel == null) {
			this.file.write(data);
		} else {
			ByteBuffer dst = ByteBuffer.wrap(data);
			channel.write(dst);
		}
		this.fileLength = this.getLength();
	}

	private byte[] getReadBlock(long blockNumber) throws IOException {

		byte[] blockBytes = this.buffer.get(blockNumber);
		if (blockBytes != null) {
			if (blockBytes.length == blockSize)
				return blockBytes;
		}
		long pos = blockNumber * blockSize;
		if(pos >=this.fileLength)
			return null;
		
		this.readAccessCount++;
		
		this.seek(pos);
		long len = Math.min((this.fileLength - pos), blockSize);
	
		blockBytes = new byte[(int) len];
		readReal(blockBytes);
		this.buffer.put(blockNumber, blockBytes);
		return blockBytes;
	}

	private void seek(long pos) throws IOException {
		if (this.channel == null) {
			this.file.seek(pos);
		} else {
			this.channel.position(pos);
		}
	}

	private void readReal(byte[] block) throws IOException {
		if (this.channel == null) {
			this.file.readFully(block);
		} else {
			ByteBuffer dst = ByteBuffer.wrap(block);
			channel.read(dst);
		}

	}

	public void close() {
		if (this.channel != null) {
			FileUtil.close(this.channel);
		}
		FileUtil.close(this.file);
	}

	public int readInt(long pos) throws IOException {
		byte[] buf = read(pos, 4);
		return DataInputX.toInt(buf, 0);
	}

	public void writeInt(long pos, int i) throws IOException {
		this.write(pos, DataOutputX.toBytes(i));
	}

	public byte readByte(long pos) throws IOException {
		byte[] buf = read(pos, 1);
		return buf[0];
	}
	public long getOffset(){
		return this.offset;
	}

	public short readShort(long pos) throws IOException {
		byte[] buf = read(pos, 2);
		return DataInputX.toShort(buf, 0);
	}



}