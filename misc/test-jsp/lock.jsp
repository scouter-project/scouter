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
private int getTime(String t){
	if(t==null) return 10000;
	return Integer.parseInt(t);
}
%>

<%
    int t = getTime(request.getParameter("t"));
 
	Connection conn = getConnection();
	conn.setAutoCommit(false);
	Statement stmt = conn.createStatement();
	stmt.executeUpdate("update scouter set name='www'  where id='id10'");
	Thread.sleep(t);
	conn.commit();
	stmt.close();
	conn.close();
%>

</body>
</html>
