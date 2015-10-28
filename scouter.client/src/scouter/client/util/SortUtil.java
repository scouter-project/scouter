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
import java.util.List;

import org.eclipse.swt.widgets.TableItem;

import scouter.util.StringUtil;
import scouter.util.CastUtil;


public class SortUtil {
	public SortUtil() {

	}

	public SortUtil(boolean asc) {
		DESC_ORDER = !asc;
	}

	boolean DESC_ORDER = true;

	public void swap(TableItem a, TableItem b, int len) {
		for (int i = 0; i < len; i++) {
			String tm = a.getText(i);
			a.setText(i, b.getText(i));
			b.setText(i, tm);
		}
	}

	public List<String> itemToArray(TableItem item, int cols) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < cols; i++) {
			list.add(StringUtil.trimToEmpty(item.getText(i)));
		}
		return list;
	}

	public void arrayToList(List<String> list, TableItem item) {
		for (int i = 0; i < list.size(); i++) {
			item.setText(i, list.get(i));
		}
	}

	public void sort_num(TableItem[] items, int idx, int cols) {
		List<Double> _key = new ArrayList<Double>();
		List<List<String>> _value = new ArrayList<List<String>>();

		for (int i = 0; i < items.length; i++) {
			String t = items[i].getText(idx);
			t = numonly(t);
			double key = CastUtil.cdouble(t);
			List<String> value = itemToArray(items[i], cols);
			boolean flag = false;
			for (int j = 0; j < _value.size(); j++) {
				double k = _key.get(j);
				if ((DESC_ORDER && k < key) || (DESC_ORDER == false && k > key)) {
					_key.add(j, key);
					_value.add(j, value);
					flag = true;
					break;
				}
			}
			if (flag == false) {
				_key.add(key);
				_value.add(value);
			}
		}
		for (int i = 0; i < items.length && i < _value.size(); i++) {
			arrayToList(_value.get(i), items[i]);
		}
	}

	private String numonly(String t) {
		char[] c = t.toCharArray();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < c.length; i++) {
			switch (c[i]) {
			case '-':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case '.':
				sb.append(c[i]);
			}
		}
		return sb.toString();
	}

	public void sort_str(TableItem[] items, int idx, int cols) {
		List<String> _key = new ArrayList<String>();
		List<List<String>> _value = new ArrayList<List<String>>();

		//Collator collator = Collator.getInstance(Locale.getDefault());
		for (int i = 0; i < items.length; i++) {
			String key = items[i].getText(idx);
			List<String> value = itemToArray(items[i], cols);
			boolean flag = false;
			for (int j = 0; j < _value.size(); j++) {
				String k = (String) _key.get(j);
//				if ((DESC_ORDER && collator.compare(k, key) > 0) || (DESC_ORDER == false && collator.compare(k, key) < 0)) {
				if ((DESC_ORDER && k.compareTo(key) > 0) || (DESC_ORDER == false && k.compareTo(key) < 0)) {
					_key.add(j, key);
					_value.add(j, value);
					flag = true;
					break;
				}
			}
			if (flag == false) {
				_key.add(key);
				_value.add(value);
			}
		}
		for (int i = 0; i < items.length && i < _value.size(); i++) {
			arrayToList(_value.get(i), items[i]);
		}
	}
}