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

public class DigestKey {
	
	final int objHash;
	final int digestHash;
	
	public DigestKey(int objHash, int digest) {
		this.objHash = objHash;
		this.digestHash = digest;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + digestHash;
		result = prime * result + objHash;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DigestKey other = (DigestKey) obj;
		if (digestHash != other.digestHash)
			return false;
		if (objHash != other.objHash)
			return false;
		return true;
	}
}
