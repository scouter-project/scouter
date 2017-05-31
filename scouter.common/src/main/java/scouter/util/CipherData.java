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

package scouter.util;

import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

public class CipherData {
	private Cipher enc;
	private Cipher dec;

	public CipherData(String k) {
		try {
			Key key = genKey(k);
			enc = Cipher.getInstance("DES/ECB/NoPadding");
			enc.init(Cipher.ENCRYPT_MODE, key);
			dec = Cipher.getInstance("DES/ECB/NoPadding");
			dec.init(Cipher.DECRYPT_MODE, key);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Key genKey(String k) throws GeneralSecurityException {
		byte[] host = (k + "012345678").getBytes();
		byte[] key = new byte[8];
		System.arraycopy(host, 0, key, 0, 8);
		return SecretKeyFactory.getInstance("DES") //
				.generateSecret(new DESKeySpec(key));
	}

	public byte[] decode(byte[] data) {
		try {
			data = dec.doFinal(data);
			data = new DataInputX(data).readBlob();
			return data;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public byte[] encode(byte[] data) {
		try {
			data = new DataOutputX().writeBlob(data).toByteArray();
			byte[] plainBytes = padding(data);
			return enc.doFinal(plainBytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static byte[] padding(byte[] src) {
		if(src.length%8==0)
			return src;
		int destlen = (src.length / 8 + 1) * 8;	
		byte[] dest = new byte[destlen];
		System.arraycopy(src, 0, dest, 0, src.length);
		return dest;
	}

	public static void main(String[] args) throws Exception {
		CipherData cb = new CipherData("admin");
		String pwd = "www1234";
		for (int i = 0; i < 100; i++) {
			byte[] org =pwd.getBytes();
			byte[] cnd = cb.encode(org);
			String rtn = new String(cb.decode(cnd));
			System.out.println("'" + pwd + "'");
			System.out.println("'" + org.length + "'");
			System.out.println("'" + cnd.length + "'");
				System.out.println("'" + rtn + "'");
		}
	}
}