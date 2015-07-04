<%@ page session="true"%>
<%@ page import="javax.servlet.*" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="javax.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="javax.naming.*" %>

<html>
<body>
<%!
public Connection getConnection() throws Exception {
	DataSource datasource = (DataSource) new InitialContext().lookup("java:/comp/env/jdbc/hsql");
	return datasource.getConnection();
}
%>
<%
	Connection conn = getConnection();
	Statement stmt = conn.createStatement();
	try{ stmt.executeUpdate("drop table scouter "); } catch(Exception e){}
    stmt.executeUpdate("CREATE TABLE scouter ( id varchar(40) ,name varchar(40) )");
    for(int  i = 0 ; i<10000; i++){
       stmt.executeUpdate("insert into scouter(id,name) values('id"+i+"','name"+i+"')" );
    }
    stmt.close();
    conn.close();
%>	
create ok
</body>
</html>
