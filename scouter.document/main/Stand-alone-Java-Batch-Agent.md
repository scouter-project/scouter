# Stand-alone Java Batch Agent

Scouter APM provides batch monitoring as well as WAS monitoring.
General APM is not possible to monitor the batch that process the large amounts records, or possible but, performance is reduced. Scouter gather statistical performance data in consideration of the characteristics of the batch and can analyze to Java function level, yet large level without degradation in performance and powerful.

Scouter batch agent provides the following functions.
- Execution elapsed time(CPU usage)
- SQL Profiling data(SQL statements , SQL execution time , SQL processing record count , SQL execution count)
- Periodic gathering process stack  

## Installation Instructions
If Scouter's server and client is installed, it is possible to monitor Java Batch witch three steps below.
   1. Download and install Java Batch Agent(scouter.agent.tar.gz - scouter/agent.batch directory)  
   2. Add scouter option to Java Batch process (Note: Java option)  
   3. Run the Scouter Batch daemon (note: The difference between Java agent and Java batch agent)  

## Java option
The default installation method of the scouter batch agent is the same as the WAS based Java agent.	
Monitoring is possible by adding -javaagent and -Dscouter.config settings to Java batch execution command (Batch starting shell script).

```
JAVA_OPTS=" ${JAVA_OPTS} -javaagent:${SCOUTER_AGENT_DIR}/agent.batch/scouter.agent.batch.jar"
JAVA_OPTS=" ${JAVA_OPTS} -Dscouter.config=${SCOUTER_AGENT_DIR}/agent.batch/conf/scouter.batch.conf"
```

## The difference between Java agent and Java batch agent
Scouter Java batch agent differs from scouter Java agent in that batch agent is installed and there is need to separately run the Scouter batch daemon on the server.
Because batch process is not always running, and dozens of batch processes are executed at the same time, it always needs to be a daemon that resides on batch server and is responsible for batch agent environment setting and integrated information gathering and transfer to a scouter server.
Scouter batch daemon is included in scouter.agent.batch.jar. Execution can be done by running startup.sh in the agent.batch directory.

*startup.sh*
```
nohup java -cp ./scouter.agent.batch.jar -Dscouter.config=./conf/scouter.batch.conf scouter.agent.batch.Main &
```
***

If the scouter batch daemon is not running, functions such as changing environment settings, stack collection, batch performance count collection, etc. are not working properly.


## Environment setting

### Environment setting file
You can change the monitoring options by specifying the environment setting file like `$ {SCOUTER_AGENT_DIR} / agent.batch / scouter.batch.conf` and changing the contents.
> If there is no option, the default value is applied.

### Sample
*${scouter_agent_directory}/batch.agent/conf/scouter.batch.conf*
```
# Stand-Alone mode
scouter_standalone=false

# Batch ID (batch_id_type: class,args,props) (batch_id: args->index number, props->key string)
batch_id_type=args
batch_id=0

# Scouter Server IP Address (Default : 127.0.0.1)
net_collector_ip=127.0.0.1

# Scouter Server Port (Default : 6100)
net_collector_udp_port=6100
net_collector_tcp_port=6100

# Scouter Name(Default : batch)
obj_name=exbatch
```
***

###  Environment setting items
Item     |    Default value    | Description
------------ | -------------- | --------------
scouter_enabled | true | Set whether to monitor (true - monitoring operation)
scouter_standalone | false | Set whether to send monitoring results to the scouter server (false - send monitoring results to the scouter server). If set to true, Log the monitoring result to log_dir on the batch server with batch comprehensive monitoring result(sbr file - text format) and stack log (log file). If set to false, the result of comprehensive monitoring will not leave a file, the log file of the stack will be deleted after server transfer.
batch_id_type | class | Specify the rule for extracting batch JOB ID (class - specify the start Java class by JOB ID, args - specify one of the parameters added after the execution class by JOB ID, props - JVM setting One of the parameters is set to JOBID)
batch_id |       | The meaning differs depending on the batch_id_type in the detailed setting for extracting the JOB ID (args - the index position of the JOB ID (starting from 0) with the parameter added to the execution command, props - the name of the JVM setting parameter (eg, If DjobID=TEST001, the setting value is jobID))
sql_enabled | true | Whether or not to collect SQL execution statistics information (true - collection of SQL statistical information)
sql_max_count | 100 | You can collect up to the maximum SQL count that can be monitored when collecting SQL execution statistics information. The rest of the SQL statements executed exceeding 100 kinds of default values are caught in the statistics of one SQL statement as a whole SQL statements are recorded in "Others"
hook_jdbc_pstmt_classes |  | When using another PreparedStatement class in addition to the JDBC PreparedStatement class which is internally set basic, add its class name (delimiter is a comma (,))
hook_jdbc_stmt_classes |  | When using other Statement classes other than the JDBC Statement class which is internally set, add the class name (delimiter is a comma (,))
hook_jdbc_rs_classes |  | When using other ResultSet classes other than the JDBC ResultSet class which is internally set, add the class name (delimiter is a comma (,))
sfa_dump_enabled | true | Ability to collect stacks periodically when Java batch executing and to analyze performance up to the function level (true - collect stack). When stack analysis, use Stack Frequency Analyzer
sfa_dump_interval_ms | 10000 | Setting of Java batch stack collection interval (ms)
sfa_dump_filter |  | Set the filter to collect only when the specified character string is on the stack (use a comma (,) as the delimiter between filters). To reduce the amount of stack being collected and to reduce the size of the entire log file.
sfa_dump_dir | dump in the batch agent directory | Specify the directory where stack log files are stored (dump directory of the default Java batch agent directory). When a transmission is made to the scouter server, the log file of the stack is deleted.
sfa_dump_header_exists | true | Set whether to retain header information such as collection time and JVM information when collecting stack (true - Including head information)
sfa_dump_send_elapsed_ms | 30000 | Only when the Java batch execution time is longer than the set time, send the log of the stack collected to the scouter server (ms)
batch_log_send_elapsed_ms | 30000 | Send batch execution information to the scouter server only when the Java batch execution time is longer than the set time (ms)
thread_check_interval_ms | 1000 | The time to periodically check if the thread was terminated in java batch process (Default: 1 second). The java batch agent executes the task of integrating the execution statistics of the thread into the overall statistics when the thread terminates to collect SQL execution statistical information on a thread basis. For this reason, it is checked whether or not the thread is terminated.
net_collector_ip | 127.0.0.1 | Scout server IP address
net_collector_udp_port | 6100 | UDP collection port number of scouter server
net_collector_tcp_port | 6100 | TCP collection port number of scouter server
net_collector_tcp_session_count | 1 | TCP max connections with Scouter server
net_collector_tcp_so_timeout_ms | 60000 | Socket read timeout time (in milliseconds) for TCP connection with scouter server
net_collector_tcp_connection_timeout_ms | 3000 | TCP connection timeout time with the scouter server (ms)
net_local_udp_port | 6101 | UDP communication port number of Java batch agent daemon. UDP port used when each batch process sends status information to Java agent daemon.
net_udp_packet_max_bytes | 60000 | UDP communication maximum packet size
obj_name | | Name of Java batch agent (valid when basic name is changed)
obj_host_type | | Specify the type of server on which Java batch runs
obj_host_name | | Specify the host name of the server that Java batch will run(If you change Obj_name with the scouter host agent, you need to change the host name of each agent inside the server) 
_log_asm_enabled | false | Debug setting (false - debug off) to check whether Scouter performs bytecode engineering for monitoring properly
log_dir | | Directory that stores execution log of Java batch agent(Basically, the log directory under the Java batch agent installation directory) 
log_rotation_enabled | true | Sets whether to generate log files on a daily basis (true - log generation per day)
log_keep_days | 7 | Setting the period to hold the batch agent log files(days). It was deleted when the holding period elapsed.


## Scouter batch monitoring screen example
![Scouter](../img/client/batch_monitor_example1.png)
