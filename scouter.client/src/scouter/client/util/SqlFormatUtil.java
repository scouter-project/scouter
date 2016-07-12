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

import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;

import scouter.util.StringUtil;

public class SqlFormatUtil {
	public static String[] key = { "select", "update", "insert", "delete",
			"from", "where", "between", "commit", "set", "join", "having",
			"group", "by", "create", "default", "use", "desc", "alter",
			"fetch", "order", "and", "or", "as", "round", "decode", "nvl",
			"instr", "sysdate", "sum", "rownum", "in", "left", "outer",
			"close", "continue", "into", "values", "now()" };
	
	public static void applyStyledFormat(StyledText text, String sql) {
		if (StringUtil.isEmpty(sql)) return;
		text.setText(sql);
		text.addLineStyleListener(new LineStyleListener() {
			public void lineGetStyle(LineStyleEvent event) {
				String line = event.lineText;
				LinkedList<StyleRange> list = new LinkedList<StyleRange>();
				line = line.toLowerCase();
				String[] tokens = StringUtil.tokenizer(line, " \n\r\f\t()+*/-=<>'`\"[],");
				if (tokens == null) return;
				HashSet<String> set = new HashSet<String>();
				for (int i = 0; i < tokens.length; i++) {
					set.add(tokens[i]);
				}
				for (int i = 0; i < key.length; i++) {
					if (set.contains(key[i])) {
						int cursor = -1;
						while ((cursor = line.indexOf(key[i], cursor + 1)) > -1) {
							StyleRange sr = new StyleRange();
							sr.start = event.lineOffset + cursor;
							sr.length = key[i].length();
							sr.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
							list.add(sr);
						}
					}
				}
				event.styles = list.toArray(new StyleRange[list.size()]);
			}
		});
	}
}
