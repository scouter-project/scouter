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

package scouter.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

public class Account {
	public String id = "";
	public String password = ""; // encrypted
	public String email = "";
	public String group = "";
	
	public byte[] toBytes() throws IOException {
		ByteArrayOutputStream out =new ByteArrayOutputStream();
		byte[] idBytes = id.getBytes();
		out.write(idBytes.length);
		out.write(idBytes);
		byte[] passBytes = password.getBytes();
		out.write(passBytes.length);
		out.write(passBytes);
		byte[] emailBytes = email.getBytes();
		out.write(emailBytes.length);
		out.write(emailBytes);
		byte[] groupBytes = group.getBytes();
		out.write(groupBytes.length);
		out.write(groupBytes);
		return out.toByteArray();
	}
	
	public void toObject(byte[] bytes) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		int len = in.read();
		byte[] idBytes = new byte[len];
		in.read(idBytes);
		this.id = new String(idBytes);
		len = in.read();
		byte[] passBytes = new byte[len];
		in.read(passBytes);
		this.password = new String(passBytes);
		len = in.read();
		byte[] emailBytes = new byte[len];
		in.read(emailBytes);
		this.email = new String(emailBytes);
		len = in.read();
		byte[] groupBytes = new byte[len];
		in.read(groupBytes);
		this.group = new String(groupBytes);
		in.close();
	}
	
	@Override
	public String toString() {
		return "Account [id=" + id + ", email=" + email + ", group=" + group
				+ "]";
	}

	public static void main(String[] args) throws IOException {
		Account ac = new Account();
		ac.id = "bill23";
		ac.password = "123344";
		ac.email = "bill23@lgcns.com";
		ac.group = "Admin";
		byte[] aa = ac.toBytes();
		DataOutputX out = new DataOutputX();
		out.writeBlob(aa);
		DataInputX in = new DataInputX(out.toByteArray());
		Account ac1 = new Account();
		ac1.toObject(in.readBlob());
		System.out.println(ac1);
	}
}