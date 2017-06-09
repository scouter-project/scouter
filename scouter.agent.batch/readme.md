## Java Stand-alone Batch Agent
The Scouter Java Stand-alone Batch Agent is monitoring stand-alone batch's performance.
If you install Scouter Agent in the java batch process, you will understand what happens inside it.

Scouter show you that which SQL is slow, how many records are processed 
how many batch processes are running

##  Service Trace
Scouter is profiling  batch process's SQL processing.
Periodically process's Stack are collected and stored on the Scouter server.

1. Batch process: start time & end time
2. Method : Periodic collection of batch process's stack 
3. DB Access : JDBC/SQL (SQL Text, SQL processed rows, SQL execution time, SQL runs)

## Resource & Service Performance 
Resource & Service Performance's data is collected by regular interval.
The collected data is called  performance counter(or just counter) that includes running batch process count.


