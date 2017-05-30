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

package scouter.server.db.io.zip;

import java.io.File;

import scouter.server.db.DBCtr;
import scouter.server.db.XLogWR;

public class GZipCtr {
	
	public static final int MAX_QUE_SIZE = 20000;
	
	public final static int BLOCK_MAX_SIZE = 32 * 1024 * 1024;



	public static String getDataPath(String date) {
		StringBuffer sb = new StringBuffer();
		sb.append(DBCtr.getRootPath());
		sb.append("/").append(date);
		sb.append(XLogWR.dir());
		sb.append("/data");
		return sb.toString();
	}
	public static String createPath(String date) {
		String path =  getDataPath(date);
		new File(path).mkdirs();
		return path;
	}
}