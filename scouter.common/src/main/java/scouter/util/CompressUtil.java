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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressUtil {

	public static byte[] doZip(byte[] data) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		java.util.zip.GZIPOutputStream gout = new GZIPOutputStream(out);
		gout.write(data);
		gout.close();
		return out.toByteArray();
	}

	public static byte[] unZip(byte[] data) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		java.util.zip.GZIPInputStream gin = new GZIPInputStream(in);
		data = FileUtil.readAll(gin);
		gin.close();
		return data;
	}
}