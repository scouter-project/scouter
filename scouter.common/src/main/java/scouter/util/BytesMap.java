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
 *  
 */
package scouter.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Paul S.J. Kim(sjkim@whatap.io)
 */
public class BytesMap {
	private final Map<String, byte[]> table = new Hashtable<String, byte[]>();

	public void put(String name, byte[] data) {
		if (name == null || data == null)
			return;
		table.put(name, data);
	}

	public void remove(String name) {
		if (name == null)
			return;
		table.remove(name);
	}

	public byte[] getBytes(String name) {
		return table.get(name);
	}

	public List<URL> getResourceList() {
		List<URL> urlList = new ArrayList<URL>();
		Iterator<String> itr = table.keySet().iterator();
		while (itr.hasNext()) {
			String key = itr.next();
			urlList.add(getResource(key));
		}
		return urlList;
	}

	public URL getResource(String name) {
		try {
			return new URL(null, "bytes:///" + name, urlStreamHandler);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	protected URLStreamHandler urlStreamHandler = new URLStreamHandler() {
		protected URLConnection openConnection(URL u) throws IOException {
			return new URLConnection(u) {

				public void connect() throws IOException {
				}

				public InputStream getInputStream() throws IOException {
					String name = this.getURL().getPath().substring(1);
					byte[] b = table.get(name);
					if (b == null) {
						throw new IOException("unknown bytes name : " + name);
					}
					return new ByteArrayInputStream(b);
				}

			};
		}
	};
}
