## Java  Agent
Tomcat is an web application server that executes Java servlets and Java Server Page(JSP). 
Described as a "reference implementation" of the Java Servlet and the Java Server Page specifications, 

The Scouter Java Agent is monitoring Tomcat's performance.
If you install Scouter Agent in the Tomcat, you will understand what happens inside it.

Scouter show you that which SQL is slow, how many transactions(http request) are concurrently running 
how many transactions per second(TPS) etc

##  Service Trace
Scouter is profiling  transactions of http requests.
Every steps of transactions are collected and stored on the Scouter server.

1. Service start & end: HttpService
2. Method : any/public
3. External Service Call : apicall
4. DB Access : JDBC/SQL
5. Misc : Open/Close Connection, parameter/return of methods

## Resource & Service Performance 
Resource & Service Performance's data is collected by regular interval.
The collected data is called  performance counter(or just counter) that includes CPU usage, 
Transaction per second(TPS), average response time.

###  Resource Usage
- OS Counter : CPU, MEM, DISK, NET
- JVM Counter : Heap, GC, Thread CPU Time
 
### Service Performance
Response Time, TPS, Page View, Error rate etc

### Users
Recent 5min users, Daily Visit users etc

