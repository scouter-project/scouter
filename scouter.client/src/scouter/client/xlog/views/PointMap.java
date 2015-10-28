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
package scouter.client.xlog.views;

import java.util.Arrays;

public class PointMap {

	private boolean bitmap[] = new boolean[0];
	private int w;
	private int h;

	public void reset(int w, int h) {

		if (this.w == w && this.h == h) {
			Arrays.fill(this.bitmap, false);
		} else {
			this.w = w;
			this.h = h;
			this.bitmap = new boolean[w * h];
		}

	}

	public boolean check(int x, int y) {
		if (x < 0 || w <= x || y < 0 || h <= y){
			return false;
		}
		try {
			if (bitmap[y * w + x]) {  
				return false;
			} else {
				bitmap[y * w + x] = true;
				return true;
			}
		} catch (Throwable t) {
		}
		return false;
	}

}
