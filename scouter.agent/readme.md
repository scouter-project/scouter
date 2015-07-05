## Java  Agent
Java Agent is monitoring Tomcat's performance
Tomcat is a Web Application Server which responses from Http Requests of client

Scouter monitors:   

1. Service Trace(Profiling)
2. Resource & Service Processing Performance
   - Resource Usage
   - Service Process Performance, such as TPS, Responce Time etc
   - Service Users

##  Service Trace
Scouter profiles for each Transaction. 
Transaction means one execution of service

1. Service start & end: HttpService
2. Method : any/public
3. External Service Call : apicall
4. DB Access : JDBC/SQL

Some others are also available to monitor such as getConnection and Close Connection

## Resource & Service Performance 
Resource & Service Performance are collected by regular interval
Those are simple counts, for example CPU usage, Transaction per second(TPS), average response time

###  Resource Usage
 Heap Mem, Cpu, Mem, GC Time etc
 
### Service Performance
Response Time, TPS, Page View, Error rate etc

### Users
Recent 5min users, Daily Visit users etc

