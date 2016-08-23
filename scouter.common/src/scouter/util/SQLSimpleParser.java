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
			Arrays.asList('=', '<', '>', '!', ',', ' ', '(', ')'));
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
		clearNode(sqlNode);
		depth = 0;
		parenthesisCount = 0;
	}
	
	private void clearNode(SQLNode node) {
		if(node == null) {
			return;
		}
		SQLNode nextNode = null;
		if(node.nextNode != null) {
			nextNode = node.nextNode;
			node = null;
		} else {
			node = null;
		}
		clearNode(nextNode);
	}

	public String getCrudInfo(String value) {
		try {
			String[] lines = value.split("[\r\n]");
			for (int i = 0; i < lines.length; i++) {
				String[] tokens = tokenize(lines[i], false);
				crudInfo(tokens);
			}
		} catch (Exception ex) {
			return "";
		}

		StringBuffer sb = new StringBuffer();
		ArrayList<String> tempList = new ArrayList<String>();
		SQLNode node = sqlNode;
		do {
			for (int i = 0; i < node.tableList.size(); i++) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				StringBuffer s = new StringBuffer();
				String tblInfo = s.append(node.tableList.get(i)).append("(").append(node.type.toString())
						.append(")").toString();
				if (!tempList.contains(tblInfo)) {
					sb.append(tblInfo);
				}
			}
			node = node.nextNode;
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

	public void crudInfo(String[] tokens) {
		try {
			for (int i = 0; i < tokens.length; i++) {
				switch (tokens[i].toUpperCase()) { 
				case "(": {
					parenthesisCount++;
					inParenthesis = true;
					break;
				}
				case ")": {
					parenthesisCount--;
					if (parenthesisCount == 0) {
						inParenthesis = false;
					}
					break;
				}
				case "SELECT": {
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
					break;
				}
				case "DELETE": {
					createOrAppendNode(SQLTypeEnum.DELETE);
					break;
				}
				case "INSERT": {
					createOrAppendNode(SQLTypeEnum.INSERT);
					break;
				}
				case "UPDATE": {
					createOrAppendNode(SQLTypeEnum.UPDATE);
					i = applyNode(i, tokens);
					break;
				}
				case "MERGE": {
					if (tokens[i + 1].toUpperCase().equals("INTO")) {
						createOrAppendNode(SQLTypeEnum.MERGE);
					}
					break;
				}
				case "FROM": {
					i = applyNode(i, tokens);
					break;
				}
				case "INTO": {
					i = applyNode(i, tokens);
					break;
				}
				case "JOIN": {
					createOrAppendNode(SQLTypeEnum.SELECT);
					i = applyNode(i, tokens);
					break;
				}
				}
			}
		} catch (Exception e) {
			throw e;
		}

	}

	private int applyNode(int index, String[] tokens) {
		int returnIndex = index;
		try {
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
		boolean hasComments = false;
		int start = 0;

		ArrayList<String> tokenList = new ArrayList<String>();
		int len = arrays.length;
		for (int i = 0; i < len; i++) {
			if (i == (len - 1)) {
				if (spliter.contains(arrays[i])) {
					char[] token = Arrays.copyOfRange(arrays, start, len - 1);
					tokenList.add(new String(token));
					tokenList.add(String.valueOf(arrays[i]));
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
						hasComments = true;
					}
				}
				continue;
			}

			if (arrays[i] == '#') {
				if (!hasQuotation) { // sql comments for mysql
					hasComments = true;
				}
				continue;
			}

			if (arrays[i] == '/') {
				if (i < len - 1 && arrays[i + 1] == '*') { // sql comments or hint
					hasComments = true;
				}
				if (i > 0 && arrays[i - 1] == '*' && hasComments) {
					hasComments = false;
					if (!keepComments) {
						start = i + 1;
					}
				}
				continue;
			}

			if (i > 0 && spliter.contains(arrays[i])) {
				if (hasQuotation || hasComments) { // ignore comments, literal
					continue;
				}
				if (i >= start) {
					char[] token = Arrays.copyOfRange(arrays, start, i);
					/*
					 * 구분자가 연속으로 있는 경우 -> ex: 공백 + ( select * from id in (...)
					 * start 와 index 는 동일값. 이러한 경우 token.length = 0
					 */
					if (token.length > 0) {
						tokenList.add(new String(token));
					}
					if (arrays[i] != ' ') {
						if (spliter.contains(arrays[i + 1])) { // ex) >=, <=,
																// <>...
							String temp = "" + arrays[i] + arrays[i + 1];
							if (temp.equals("<>") || temp.equals("!=") || temp.equals("<=") || temp.equals(">=")) {
								tokenList.add(temp);
							} else {
								tokenList.add(String.valueOf(arrays[i]));
							}
						} else {
							tokenList.add(String.valueOf(arrays[i]));
						}
						start = i + 1;
					}
				}
				start = i + 1;
				continue;
			}
		}
		return tokenList.toArray(new String[tokenList.size()]);
	}

	public static void main(String[] args) {
		try {
			testCrudInfo();
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	private static void testCrudInfo() throws Exception {
		SQLSimpleParser parser = new SQLSimpleParser();
		String sql = "select * , (select 1 ) from test,test1";
		printInfo(parser,sql);
		
		sql = "select * from test";
		printInfo(parser,sql);
		
		sql = "update tbl2 set id = 1 where neme='aaa'";
		printInfo(parser,sql);
		
		sql= "select t1.id , t2.name from table1 t1 join table2 t2 on t1.id = t2.id";
		printInfo(parser,sql);
		
		sql = "select *   from tabl3 where id4 in (1,2,3) and id=   'abde' "
				+ "and id2 != -1234 and id3= 45.2 and id4 in (1, 3, 4,5) "
				+ " and id5 IN (1, 3, 4, 5 ) and id5='aaaa' "
				+ "and id4 between   'a' and 'b';";
		printInfo(parser,sql);
		
		sql="SELECT /*+ INDEX(E EMP_N1) */ * FROM EMP E, DEPT D WHERE E.DEPTNO = D.DEPTNO ND   D.DEPTNO = :B1; ";
		printInfo(parser,sql);
		
		sql = "select * from table where id = 'P'";
		printInfo(parser,sql);
		
		sql = "select * from tabl3 where id= 'abde' and id2 = 123456 and id3= 45.2 and id4 in (1,3,4,5) and id5='aaaa' and id6 like '%aa';";
		printInfo(parser,sql);
		
		sql = "SELECT PK_ID||'_'||DATE_FORMAT(TRANSACTION_DATE,'%Y%m%d%H%i%s') as DocumentID, --comments   \r\n          " +
					"CONTAINER_NO as BusinessID, '688298116' as EDI_SENDER_ID,                                   " +
					"'687822338' as EDI_RECEIVER_ID,                                                             " +
					"INTERFACE_ID as XML_DOCUMENT_NO,                                                            " +
					"'' as DOCUMENT_SEQ_NO,                                                                      " +
					"CORP_TYPE as LOU_CODE,                                                                      " +
					"ATTRIBUTE9 as D_PLACE_NAME,                                                                 " +
					"CORP_TYPE, DC_CD, SHIPPING_LINE1, SHIPPING_LINE2,                                           " +
					"CARRIER_CODE1, CARRIER_CODE2, ORDER_NO, LINE_NO,                                            " +
					"ALLOCATION_NO, CC_NO, TOTAL_SHP_QTY, ORDER_QTY,                                             " +
					"INVOICE_NO, AFFILIATE_FLAG, SHIP_METHOD, TRANSPORT_TYPE,                                    " +
					"ACCOUNT_UNIT, WEIGHT, UNIT_CBM, CBM_SUM,                                                    " +
					"DATE_FORMAT(PICK_RELEASE_DATE,'%Y%m%d%H%i%s') as PICK_RELEASE_DATE,                         " +
					"DATE_FORMAT(PG_YMD,'%Y%m%d%H%i%s') as PG_YMD,                                               " +
					"DATE_FORMAT(IOD_DATE,'%Y%m%d%H%i%s') as IOD_DATE,                                           " +
					"CURRENCY_CODE, ORDER_PRICE, ORDER_TYPE, CONTAINER_TYPE,                                     " +
					"LCL_FLAG, CONTAINER_NO, MODEL_CD, PRODUCT_TYPE,                                             " +
					"BILL_TO_CODE, BILL_TO_NAME, SHIP_TO_CODE, SHIP_TO_NAME,                                     " +
					"L_PORT, D_PORT, F_DEST, NATION,                                                             " +
					"DATE_FORMAT(CREATION_DATE,'%Y%m%d%H%i%s') as CREATION_DATE,                                 " +
					"DATE_FORMAT(LAST_UPDATE_DATE,'%Y%m%d%H%i%s') as LAST_UPDATE_DATE,                           " +
					"PRCS_MESSAGE, ATTRIBUTE1, ATTRIBUTE2, ATTRIBUTE3,                                           " +
					"ATTRIBUTE4, ATTRIBUTE5, ATTRIBUTE6, ATTRIBUTE7,                                             " +
					"ATTRIBUTE8, ATTRIBUTE9, ATTRIBUTE10, ATTRIBUTE11,                                           " +
					"ATTRIBUTE12, ATTRIBUTE13, ATTRIBUTE14, ATTRIBUTE15,                                         " +
					"ATTRIBUTE16, ATTRIBUTE17, ATTRIBUTE18, ATTRIBUTE19,                                         " +
					"ATTRIBUTE20, LAST_UPDATE_USER_ID, PRCS_STATUS, TRANSFER_MESSAGE,                            " +
					"PRICE_TERMS, PAYMENT_TERM, CUSTOMER_PO_NO, INFO_CHANGE_FLAG,                                " +
					"ORIGIN_ALLOCATION_NO, DROP_QTY, INTERFACE_ID, OPTION_NO,                                    " +
					"SERVICE_FLAG, LOADING_TYPE_CODE, SOURCE_SYSTEM_CODE, CONTAINER_TXN_ID,                      " +
					"DATE_FORMAT(LOAD_CONFIRM_DATE,'%Y%m%d%H%i%s') as LOAD_CONFIRM_DATE,                         " +
					"DATE_FORMAT(RETURN_ORDER_DATE,'%Y%m%d%H%i%s') as RETURN_ORDER_DATE,                         " +
					"SHUTTLE_QTY, EXPENSE_ID, COMPANY_CODE, SEAL_NO,                                             " +
					"DATE_FORMAT(EDI_INTERFACE_DATE,'%Y%m%d%H%i%s') as EDI_INTERFACE_DATE,                       " +
					"ERROR_EDI_INTERFACE_FLAG,                                                                   " +
					"DATE_FORMAT(ERROR_EDI_INTERFACE_DATE,'%Y%m%d%H%i%s') as ERROR_EDI_INTERFACE_DATE,           " +
					"STEP_CODE, STEP_STATUS_CODE,                                                                " +
					"EDI_FUNC_ACKNOWLDG_NO,                                                                      " +
					"DATE_FORMAT(EDI_FUNC_ACKNOWLDG_DATE,'%Y%m%d%H%i%s') as EDI_FUNC_ACKNOWLDG_DATE,             " +
					"EDI_FUNC_ACKNOWLDG_STATUS_CODE,                                                             " +
					"EDI_ERROR_CODE, EDI_ERROR_MESSAGE_TEXT, EDI_SENDER_ID, EDI_RECEIVER_ID,                     " +
					"XML_DOCUMENT_NO, DOCUMENT_SEQ_NO, EDI_ENVELOPE_NO, EDI_GROUP_NO,                            " +
					"EDI_TRANSACTION_NO,                                                                         " +
					"DATE_FORMAT(TRANSACTION_DATE,'%Y%m%d%H%i%s') as TRANSACTION_DATE                            " +
					"FROM TB_LI_GERP_SEW_SHIP                                                                    " +
					"WHERE  TRANSFER_FLAG1 = 'P'; ";
		printInfo(parser,sql);
		
		sql = "select * from table1 t1 join table2 t2 on t1.id = t2.id and t2.name = 4 where t1.name='kkk'";
		printInfo(parser,sql);
		
		sql = "select * from table1 t1 join table2 t2 on t1.id = t2.id and t2.name = 4 where t1.name= (select id from tbl) ";
		printInfo(parser,sql);
		
		sql= "SELECT start_time,user_host,query_time,lock_time, rows_sent,rows_examined,db,sql_text,thread_id FROM mysql.slow_log WHERE start_time > '2016-04-19 17:18:06.097729'";
		printInfo(parser,sql);
		
		sql = "update table tbl set flag_1='Y', date1= NOW(), code='A1' where flag02='P'";
		printInfo(parser,sql);
		
		sql = "DELETE FROM TB_LI_GLA_OERDERMOU D_AMT  WHERE D_AMT.CORP_TYPE= 46 and D_AMT.DELIV_TYPE=262 " +
			  "AND D_AMT.ORD_NO LIKE 'SWWZ1DDDDD' || '%' ";
		printInfo(parser,sql);
		
		sql = "SELECT itemid, (SELECT MAX(catid) FROM category),(SELECT 1)  FROM inventory";
		printInfo(parser,sql);
		
		sql = "update tbl set name = 'test' where id = (select max(id) from test2)";
		printInfo(parser,sql);
		
	}
	
	static void printInfo(SQLSimpleParser parser, String sql) {
		System.out.println(sql);
		System.out.println(parser.getCrudInfo(sql));
		System.out.println();
		
	}
	
	
	
}
