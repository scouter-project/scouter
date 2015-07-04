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
	ResultSet rs = stmt.executeQuery("select * from scouter");
	while (rs.next()) {
		String id = rs.getString(1);
		String name = rs.getString(2);
		out.println(id + " " + name + "<br>");
	}
	rs.close();
	stmt.close();
	conn.close();
%>

</body>
</html>
