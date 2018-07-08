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

import java.util.ArrayList;
import java.util.Arrays;

public class SQLSimpleParser {
	SQLNode sqlNode = null;
	SQLNode currentNode = null;
	boolean inParenthesis = false;
	int parenthesisCount = 0;

	// Stack<SQLNode> sqlStack = new Stack<SQLNode>();
	public class SQLNode {
		SQLTypeEnum type;
		ArrayList<String> tableList;
		int depth;
		SQLNode nextNode = null;

		public SQLNode() {
			tableList = new ArrayList<String>();
			depth = 0;
		}

		public SQLNode(SQLTypeEnum type) {
			tableList = new ArrayList<String>();
			this.type = type;
		}

		public SQLNode(SQLTypeEnum type, int depth) {
			this.type = type;
			tableList = new ArrayList<String>();
			this.depth = depth;
		}
	};

	enum SQLTypeEnum {
		SELECT, DELETE, UPDATE, INSERT, MERGE;

		@Override
		public String toString() {
			switch (this) {
			case SELECT:
				return "R";
			case DELETE:
				return "D";
			case UPDATE:
				return "U";
			case INSERT:
				return "C";
			case MERGE:
				return "C";
			default:
				return "R";
			}

		}
	};

	private ArrayList<Character> spliter = new ArrayList<Character>(
			Arrays.asList('=', '<', '>', '!', ',', ' ', '(', ')','\r','\n'));
	int depth = 0;

	private void createOrAppendNode(SQLTypeEnum type) {
		depth++;
		if (sqlNode == null) {
			sqlNode = new SQLNode(type, depth);
			sqlNode.nextNode = null;
		} else {
			appendNode(sqlNode, new SQLNode(type, depth));
		}
		/*
		 * SQLNode node = new SQLNode(type); node.nextNode = null;
		 * 
		 * sqlStack.push(sqlNode);
		 */
	}

	private void appendNode(SQLNode head, SQLNode newNode) {
		while (head.nextNode != null) {
			head = head.nextNode;
		}
		head.nextNode = newNode;
	}

	private void release() {
		//clearNode(sqlNode);
		clearNode();
		sqlNode = null;
		depth = 0;
		parenthesisCount = 0;
	}
	
	private void clearNode() {
		if(sqlNode == null) {
			return;
		}
		SQLNode currNode = null;
		if(sqlNode.nextNode != null) {
			currNode = sqlNode;
			sqlNode = sqlNode.nextNode;
			currNode = null;
		} else {
			sqlNode = null;
		}
		clearNode();
	}

	public String getCrudInfo(String value) {
		try {
			/*String[] lines = value.split("[\r\n]");
			for (int i = 0; i < lines.length; i++) {
				String[] tokens = tokenize(lines[i], false);
				crudInfo(tokens);
			}*/
			
			String[] tokens = tokenize(value, false);
			crudInfo(tokens);
		} catch (Exception ex) {
			return "";
		}

		StringBuffer sb = new StringBuffer();
		ArrayList<String> tempList = new ArrayList<String>();
		SQLNode node = sqlNode;
		do {
		    if (node != null && node.tableList != null) {
                for (int i = 0; i < node.tableList.size(); i++) {
                    StringBuffer s = new StringBuffer();
                    String tblInfo = s.append(node.tableList.get(i)).append("(").append(node.type.toString())
                            .append(")").toString();
                    if (!tempList.contains(tblInfo)) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(tblInfo);
                        tempList.add(tblInfo);
                    }
                }
                node = node.nextNode;
            }
		} while (node != null);
		// printCRUD() ;
		release();
		return sb.toString();
	}

	public void printCRUD() {
		do {
			System.out.print("type:" + sqlNode.type.toString() + "-->");
			for (int i = 0; i < sqlNode.tableList.size(); i++) {
				System.out.print(sqlNode.tableList.get(i) + " ");
			}
			System.out.println();
			sqlNode = sqlNode.nextNode;
		} while (sqlNode != null);
	}

	public void crudInfo(String[] tokens) throws Exception {
		try {
			for (int i = 0; i < tokens.length; i++) {
				String token = tokens[i].toUpperCase();
				if(token.equals("(")){
					parenthesisCount++;
					inParenthesis = true;
				} else if(token.equals(")")) {
					parenthesisCount--;
					if (parenthesisCount == 0) {
						inParenthesis = false;
					}
				} else if(token.equals("SELECT")) {
					if (inParenthesis) {
						for (int j = i + 1; j < tokens.length; j++) {
							if (tokens[j].toLowerCase().equals("from")) {
								createOrAppendNode(SQLTypeEnum.SELECT);
								j = applyNode(j, tokens);
							}
							if (tokens[j].equals("(")) {
								parenthesisCount++;
								inParenthesis = true;
							}
							if (tokens[j].equals(")")) {
								parenthesisCount--;
								if (parenthesisCount == 0) {
									inParenthesis = false;
									i = j;
									break;
								}
							}
						}
					} else {
						createOrAppendNode(SQLTypeEnum.SELECT);
					}
				} else if(token.equals("DELETE")) {
					createOrAppendNode(SQLTypeEnum.DELETE);
				} else if(token.equals("INSERT")) {
					createOrAppendNode(SQLTypeEnum.INSERT);
				} else if(token.equals("UPDATE")) {
					createOrAppendNode(SQLTypeEnum.UPDATE);
					i = applyNode(i, tokens);
				} else if(token.equals("MERGE")) {
					if (tokens[i + 1].toUpperCase().equals("INTO")) {
						createOrAppendNode(SQLTypeEnum.MERGE);
					}
				} else if(token.equals("FROM")) {
					i = applyNode(i, tokens);
				} else if(token.equals("INTO")) {
					i = applyNode(i, tokens);
				} else if(token.equals("JOIN")) {
					createOrAppendNode(SQLTypeEnum.SELECT);
					i = applyNode(i, tokens);
				}
			}
		} catch (Exception e) {
			throw e;
		}

	}

	private int applyNode(int index, String[] tokens) throws Exception {
		int returnIndex = index;
		try {
			if(index == tokens.length -1 ) {
				throw new RuntimeException(index + " is the last index of tokens.");
			}
			if(depth == 0) {
				return index;
			}
			
			SQLNode node = findNode(depth);
			if(node == null) {
				throw new RuntimeException("Can't find node which has proper depth.");
			}
			if (node.type == SQLTypeEnum.SELECT) {
				if (!tokens[index + 1].equals("(")) {
					node.tableList.add((tokens[index + 1]));
					returnIndex = index + 1;
					int step = 0;
					// select
					// from
					// table1, table2
					if (tokens.length > (index + 2) && tokens[index + 2].equals(",")) { 
						step = 2;
					}
					// select
					// from
					// table1 a, table2 b
					if (tokens.length > (index + 3) && tokens[index + 3].equals(",")) { 
						step = 3;
					}
					if (step > 0) {
						for (int i = index + 1; i < tokens.length; i += step) {
							if (tokens.length >= i + step) {
								if (tokens[i + step - 1].equals(",")) {
									if (!tokens[i + step].equals("(")) {
										node.tableList.add(tokens[i + step]);
										returnIndex = i + step;
									}
								} else {
									break;
								}
							} else {
								break;
							}
						}
					}
				}
			} else { // except select clause.
				node.tableList.add((tokens[index + 1]));
				returnIndex = index + 1;
			}
			node.depth = -1; // set node.depth = -1 if node used.
			depth--;
		} catch (Exception ex) {
			throw ex;
		}
		return returnIndex;

	}

	private SQLNode findNode(int depth) {
		SQLNode node = sqlNode;
		if (node.nextNode == null) {
			return node;
		} else {
			while (node.nextNode != null) {
				node = node.nextNode;
				if (node.depth == depth) {
					break;
				}
			}
		}
		return node;
	}

	/**
	 * tokenize sql with given spliter.
	 * 
	 * @param value
	 * @param keepComments
	 * @return string array
	 * @throws Exception
	 */
	private String[] tokenize(String value, boolean keepComments) throws Exception {
		char[] arrays = value.toCharArray();
		boolean hasQuotation = false;
		boolean hasSingleLineComments = false;
		boolean hasMultiLineComments = false;
		int start = 0;

		ArrayList<String> tokenList = new ArrayList<String>();
		int len = arrays.length;
		for (int i = 0; i < len; i++) {
			if (i == (len - 1)) {
				if (spliter.contains(arrays[i])) {
					char[] token = Arrays.copyOfRange(arrays, start, len - 1);
					tokenList.add(new String(token));
					if(arrays[i] != ' ') {
						tokenList.add(String.valueOf(arrays[i]));
					}
				} else {
					char[] token = Arrays.copyOfRange(arrays, start, len);
					tokenList.add(new String(token));
				}
				break;
			}
			if (arrays[i] == '\'') { // single quotation(')
				if (hasQuotation == false) {
					hasQuotation = true;
				} else {
					hasQuotation = false;
				}
				continue;
			}

			if (arrays[i] == '-') {
				if (!hasQuotation) {
					if (i < len - 1 && arrays[i + 1] == '-') { // sql comment
						hasSingleLineComments = true;
					}
				}
				continue;
			}

			if (arrays[i] == '#') {
				if (!hasQuotation) { // sql comments for mysql
					hasSingleLineComments = true;
				}
				continue;
			}
			
			if(arrays[i] == '\n') {
				if(hasSingleLineComments) {
					hasSingleLineComments = false;
				}

			}

			if (arrays[i] == '/') {
				if (i < len - 1 && arrays[i + 1] == '*') { // sql comments or hint
					hasMultiLineComments = true;
				}
				if (i > 0 && arrays[i - 1] == '*' && hasMultiLineComments) {
					hasMultiLineComments = false;
					if (!keepComments) {
						start = i + 1;
					}
				}
				continue;
			}

			if (i > 0 && spliter.contains(arrays[i])) {
				if (hasQuotation || hasSingleLineComments || hasMultiLineComments) { // ignore comments, literal
					continue;
				}
				if (i >= start) {
					char[] token = Arrays.copyOfRange(arrays, start, i);
					/*
					 * 구분자가 연속으로 있는 경우 -> ex: 공백 + ( ==> select * from id in (...)
					 * start 와 index 는 동일값. 이러한 경우 token.length = 0
					 */
					if (token.length > 0) {
						tokenList.add(new String(token));
					}
					
					if(arrays[i] ==' ') {
						start = i+1;
						continue;
					}
					
					if(arrays[i] == '\r') {
						if(arrays[i+1] == '\n') {
							start = i + 2;
							i++;
							continue;
						} else {
							start = i+1;
						}
						continue;
					}
					
					if(arrays[i] == '\n') {
						start = i+1;
						continue;
					}
					//other case =>  '=' '<' '>'  '!'  ','  '('   ')'
					if (spliter.contains(arrays[i+1])) { // ex) >=, <=, <>...
						String temp = "" + arrays[i] + arrays[i + 1];
						if (temp.equals("<>") || temp.equals("!=") || temp.equals("<=") || temp.equals(">=")) {
							tokenList.add(temp);
							start = i + 1;
							i++;
						} else {
							tokenList.add(String.valueOf(arrays[i]));
						}
					} else {
						tokenList.add(String.valueOf(arrays[i]));
					}
				}
				start = i + 1;
				continue;
			}
		}
		return tokenList.toArray(new String[tokenList.size()]);
	}
}
