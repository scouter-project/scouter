package scouter.server.core.sqltable;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import scouter.server.core.SqlTables;

public class Test {

	/**
	 * @param args
	 * @throws JSQLParserException
	 */
	public static void main(String[] args) throws JSQLParserException {
		CCJSqlParserManager pm = new CCJSqlParserManager();
//		String sql = "update XXX set x=10 where x in( SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "
//		String sql = "update XXX set x=10 where x in( SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "
//		String sql = "update XXX set x=10 where x in( SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "
//		String sql = "update XXX set x=10 where x in( SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "
//				+ " WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6) and x= ? )";
//		String sql2 = "update XXX set x =10 where aa like '%55' ";
//		String sql3 = "select now ";
//		String sql4 = "delete from xxx ";
//		String sql5 = "insert into xxx values (10,20)";

//		String file ="d:/tmp/sample-query2.out";
//		String sql=new String(FileUtil.readAll(new File(file)));

		//String sql="select * from sss where is1='a' ";
		//String sql="call USER() ";
		String sql = "SELECT /* line.b612.domain.sticker.StickerCategoryStickerRepository.findStickerIdByCategoryIdOrderByOrderz */\n" +
				"                        stickerId\n" +
				"                FROM\n" +
				"                        sticker_category_sticker\n" +
				"                WHERE\n" +
				"                        categoryId = @{1}\n" +
				"                ORDER BY\n" +
				"                        orderz DESC";
		System.out.println(sql);
//		EscapeLiteralSQL esql =new EscapeLiteralSQL(sql);
//		esql.process();
//		System.out.println(esql.getParsedSql());
//
		System.out.println(SqlTables.parseTable(sql));
//		System.out.println(SqlTables.getInstance().doAction(sql2));
//		System.out.println(SqlTables.getInstance().doAction(sql3));
//		System.out.println(SqlTables.getInstance().doAction(sql4));
//		System.out.println(SqlTables.getInstance().doAction(sql5));
		}

}

