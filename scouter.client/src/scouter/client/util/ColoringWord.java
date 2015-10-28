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
package scouter.client.util;

public class ColoringWord{
	private String word;
	private int color;
	private boolean isBold;
	private boolean endofline;
	public ColoringWord(String word, int color, boolean isBold) {
		super();
		this.word = word;
		this.color = color;
		this.isBold = isBold;
		this.endofline = false;
	}
	public ColoringWord(String word, int color, boolean isBold, boolean endofline) {
		super();
		this.word = word;
		this.color = color;
		this.isBold = isBold;
		this.endofline = endofline;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public int getColor() {
		return color;
	}
	public void setColor(int color) {
		this.color = color;
	}
	public boolean isBold() {
		return isBold;
	}
	public void setBold(boolean isBold) {
		this.isBold = isBold;
	}
	public boolean isEndofline() {
		return endofline;
	}
	public void setEndofline(boolean endofline) {
		this.endofline = endofline;
	}
	
}
