# Trace of JDBC Connection Leak
[![English](https://img.shields.io/badge/language-English-orange.svg)](JDBC-Connection-Leak-Trace.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](JDBC-Connection-Leak-Trace_kr.md)

The purpose of tracking JDBC connection is most common methodology to detect DB connection problem. 

## Connection Leak 
It is the problem not to return the leased connection to the connection pool. Not closing Statement or ResultSet is not so significant in most of cases;JDBC driver is handling this. But not returning problem must be analyzed and solved.

## getConnection Delay
It tends to occur under the condition of low number of connection pool, and also making new connection.

## setAutoCommit True
Enabling setAutoCommit to true, each SQL execution of statement is firing commit signal to DB automatically. This can be performance problem.

## Too many commits
The explicit calling of commit() function on source code can drop down system performance, though setAutoCommit is false. (Normally framework cut off this explicit commits in the middle)

## Commit Delay
Commit delay is occurring due to the massive data insertion. This should be digging down on DB side.

## Close Delay
On the common environment using connection pool handled by WAS, close() function was called to return leased connection to connection pool. When close() is called frequently you should check the status of connection pool.


Most of problems are solved, you can turn off tracking function.
```
   profile_connection_open_enabled=false (기본값: true)   
```
Tomcat Agent is using tracking connection pool by default. If you are not using WAS pool, add explicit tracking option.

Let's check this case out, below is for MyBatis framework, 
```
<!— MyBatis -->
<bean id="dataSource"
	class="org.springframework.jdbc.datasource.SimpleDriverDataSource">
	<property name="driverClass" value="org.mariadb.jdbc.Driver" />
	<property name="url" value="jdbc:mariadb://localhost:3306/test" />
	<property name="username" value="root" />
	<property name="password" value="" />
</bean>
```
With this case, decompile org.springframework.jdbc.datasource.SimpleDriverDataSource class. As you can see below, this class is extends AbstractDriverBasedDataSource and it doesn't have getConnection() function.

```
package org.springframework.jdbc.datasource;
…

public class SimpleDriverDataSource extends AbstractDriverBasedDataSource
{
 …
```
Let's check AbstractDriverBasedDataSource. It has getConnection() function. This is the case application is responsible for creating and managing connection pool inside of it. JDBC tracking option is needed.
```
hook_connection_open_patterns=org.springframework.jdbc.datasource.AbstractDriverBasedDataSource.getConnection
```
In the profiling data, you can see below logs,

```
   -    [000002] 22:49:09.445        0      0  OPEN-DBC jdbc:fake:
           …    
   -    [000015] 22:49:12.401        0      0  CLOSE 0 ms
```
