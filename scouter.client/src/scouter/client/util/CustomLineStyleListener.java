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

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class CustomLineStyleListener implements LineStyleListener{
	
	String searchString;
	
	boolean isConfig;
	Display display;
	ArrayList<ColoringWord> keywordArray;
	boolean keywordBold;
	
	boolean searchBold;
	Color highlightingColor;
	
	public CustomLineStyleListener(boolean isConfig, ArrayList<ColoringWord> keywordArray, boolean keywordBold) {
		super();
		this.isConfig = isConfig;
		this.keywordArray = keywordArray;
		this.keywordBold = keywordBold;
	}
	
	public CustomLineStyleListener(Display display, boolean isConfig, ArrayList<ColoringWord> keywordArray, boolean keywordBold) {
		super();
		this.display = display;
		this.isConfig = isConfig;
		this.keywordArray = keywordArray;
		this.keywordBold = keywordBold;
	}

	public CustomLineStyleListener(boolean isConfig, ArrayList<ColoringWord> keywordMap, boolean keywordBold, boolean searchBold, int searchedTextColor) {
		super();
		this.isConfig = isConfig;
		this.keywordArray = keywordMap;
		this.keywordBold = keywordBold;
		this.searchBold = searchBold;
		highlightingColor = Display.getCurrent().getSystemColor(searchedTextColor);
	}
	
	public void setKeywordArray(ArrayList<ColoringWord> keywordArray) {
		this.keywordArray = keywordArray;
	}

	public void setSearchString(String searchString){
		this.searchString = searchString;
	}

	public void lineGetStyle(LineStyleEvent event) {
		if(keywordArray == null)
			return;
		
		String line = event.lineText;
		LinkedList<StyleRange> list = new LinkedList<StyleRange>();
		
		for(int inx = 0 ; inx < keywordArray.size() ; inx++){
			ColoringWord word = keywordArray.get(inx);
			int cursor = -1;
			while ((cursor = line.toLowerCase().indexOf(word.getWord().toLowerCase(), cursor + 1)) >= 0) {
				list.add(getDefaultHighlightStyle(event.lineOffset + cursor, word.getWord().length(), Display.getCurrent().getSystemColor(word.getColor()), word.isBold()));
			}
		}
		
		if(!isConfig){
			if (searchString != null && searchString.length() > 0) {
				int cursor = -1;
				while ((cursor = line.toLowerCase().indexOf(searchString.toLowerCase(), cursor + 1)) >= 0) {
					list.add(getHighlightStyle(event.lineOffset + cursor, searchString.length(), highlightingColor));
				}
			}
		}
		if(isConfig){
			if(line.startsWith("#")){
				list.add(getDefaultHighlightStyle(event.lineOffset, line.length(), Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN), false));
			}
		}
		
		if(line.startsWith("<?")){
			list.add(getDefaultHighlightStyle(event.lineOffset, line.length(), Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED), false));
		}
		
		event.styles = (StyleRange[]) list.toArray(new StyleRange[list.size()]);
	}
	
	private StyleRange getHighlightStyle(int startOffset, int length, Color color) {
		StyleRange styleRange = new StyleRange();
		styleRange.start = startOffset;
		styleRange.length = length;
		styleRange.background = color;
		if(searchBold)
			styleRange.fontStyle = SWT.BOLD;
		return styleRange;
	}
	private StyleRange getDefaultHighlightStyle(int startOffset, int length, Color color, boolean isBold) {
		StyleRange styleRange = new StyleRange();
		styleRange.start = startOffset;
		styleRange.length = length;
		styleRange.foreground = color;
		if(keywordBold || isBold)
			styleRange.fontStyle = SWT.BOLD;
		return styleRange;
	}
	
}
