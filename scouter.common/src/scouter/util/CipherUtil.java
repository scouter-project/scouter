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

package scouter.util;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

public class CipherUtil {
	public static String md5(String plainText) {
		String md5Text = null;

		if (plainText != null) {
			try {
				byte[] byteArray = plainText.getBytes();
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				md5.update(byteArray);

				byte[] md5Bytes = md5.digest();
				StringBuffer buf = new StringBuffer();

				for (int i = 0; i < md5Bytes.length; i++) {
					if ((md5Bytes[i] & 0xff) < 0x10) {
						buf.append("0");
					}
					buf.append(Long.toString(md5Bytes[i] & 0xff, 16));
				}

				md5Text = buf.toString();
			} catch (Throwable t) {
				return plainText;
			}
		}

		return md5Text;
	}

	private static Key genKey() throws GeneralSecurityException {
		String nm = CipherUtil.class.getSimpleName();	
		byte[] host = (nm + "012345678").getBytes();
		byte[] key = new byte[8];
		System.arraycopy(host, 0, key, 0, 8);
		return SecretKeyFactory.getInstance("DES") //
				.generateSecret(new DESKeySpec(key));
	}

	public static String decode(String encoded) {
		try {
			Key key = genKey();
			Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] cyper = Base64.decode(encoded);
			byte[] decoded = cipher.doFinal(cyper);

			return new DataInputX(decoded).readText();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String encode(String plain) {
		try {
			Key key = genKey();
			Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] plainBytes = padding(new DataOutputX().writeText(plain).toByteArray());
			byte[] encoded = cipher.doFinal(plainBytes);
			return Base64.encode(encoded);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	private static byte[] padding(byte[] src) {
		int destlen = (src.length / 8 + 1) * 8;
		byte[] dest = new byte[destlen];
		System.arraycopy(src, 0, dest, 0, src.length);
		return dest;
	}

	public static void main(String[] args) {
		String pwd = "admin";
		String md5 = md5(pwd);
		String cnd = encode(pwd);
		String rtn = decode(cnd);
		System.out.println("'" + pwd + "'");
		System.out.println("'" + md5 + "'");
		System.out.println("'" + cnd + "'");
		System.out.println("'" + rtn + "'");
	}
}