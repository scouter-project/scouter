# JDBC Connection Leak Trace
[![English](https://img.shields.io/badge/language-English-orange.svg)](JDBC-Connection-Leak-Trace.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](JDBC-Connection-Leak-Trace_kr.md)

JDBC Connection을 추적의 목적은 대표적인 Connection 관련 문제를 감지하기 위한 것이다. 

## Connection Leak 
 Connection을 pool에 반환하지 않는 문제이다. Statement나 ResultSet을 close하지 않는 문제는 크게 문제 되지 않는다.(문제 될 경우도 있다.), JDBC드라이버가 어느정도 해결해 주고 있다.  하지만  Connection미반환 문제는 반드시 해결하는 문제이다.

## getConnection Delay
Connecttion Pool크기가 작아 부족한 경우 자주 발생하며  Connection을 새로 맺는 경우에도 발생한다.

## setAutoCommit True
setAutoCommit가 true로 setting되면 매 SQL만다 자동으로 Commit이 DB에 전달된다. 
당연히 성능에 문제가 된다.

## Too many commits
"setAutoCommit"과 연관된 문제이기는 하지만 setAutoCommit이 False인 경우에서 개발자들이 명시적으로 빈번하게 
      커밋을 호출하는 경우가 있다(보통은 프레임웍에서 중간에 커밋하지 못하도록 막아준다.)

## Commit Delay
너무 많은 데이터를 입력하고 커밋하는 경우에 커밋지연이 발생하는데 
데이터베이스 쪽에서 상세하게 분석해야 한다.

## Close Delay
보통 WAS환경에서는 Close가 호출된다. Connection이 Pool에 반환된다. 
Close에서 delay가 발생하면 Pool의 상태를 분석해야한다


이러한 문제가 clear되었다고 확신한다면 Connection 추적을 off 한다.
```
   profile_connection_open_enabled=false (기본값: true)   
```
톰켓의 경우에는 자동으로  Connection을 추적한다 하지만 MyBatis같은 프레임웍에서 다른 DataSource 를 사용하는 경우에는 명시적으로 설정한다.

아래는 MyBastis설정이다. 
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
이런 경우 org.springframework.jdbc.datasource.SimpleDriverDataSource을 다운받아서 역컴파일 해본다. 
그러면 아래와 같이 AbstractDriverBasedDataSource을 상속하고 있고 자체에는 getConnection이 없는 것을 확인할 수 있다.

```
package org.springframework.jdbc.datasource;
…

public class SimpleDriverDataSource extends AbstractDriverBasedDataSource
{
 …
```
다시 AbstractDriverBasedDataSource을 다운받아서 확인하면 getConnection이 있는 것을 확인할 수 있다.
아래의 옵션을 에이전트에 추가하고 재기동하면 
```
hook_connection_open_patterns=org.springframework.jdbc.datasource.AbstractDriverBasedDataSource.getConnection
```
프로파일에서 아래와 같은 내용을 확인 할 수 있다.

```
   -    [000002] 22:49:09.445        0      0  OPEN-DBC jdbc:fake:
           …    
   -    [000015] 22:49:12.401        0      0  CLOSE 0 ms
```