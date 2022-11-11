/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.lang.counters;

public class CounterConstants {
	public final static String COMMON_OBJHASH = "objHash";
	public final static String COMMON_TIME = "time";

	public final static String FAMILY_HOST = "host";
	public final static String FAMILY_JAVAEE = "javaee";
	public final static String FAMILY_GOLANG = "golang";
	public final static String FAMILY_DATABASE = "database";
	public final static String FAMILY_DATASOURCE = "datasource";
	public final static String FAMILY_TRACING = "tracing";
	public final static String FAMILY_REQUEST_PROCESS = "reqproc";
	public final static String FAMILY_MARIA = "maria";
	public final static String FAMILY_BATCH = "batch";
	public final static String FAMILY_CUBRID = "cubrid";

	public final static String HOST = "host";
	public final static String WINDOWS = "windows";
	public final static String LINUX = "linux";
	public final static String OSX = "osx";
	public static final String AIX = "aix";
	public static final String HPUX = "hpux";

	public final static String JAVA = "java";
	public final static String TOMCAT = "tomcat";
	public final static String VERTX = "vertx";
	public final static String JBOSS = "jboss";
	public final static String JETTY = "jetty";
	public final static String RESIN = "resin";
    public final static String WEBSPHERE = "websphere";
    public final static String WEBLOGIC = "weblogic";
    public final static String GLASSFISH = "glassfish";
	
	public final static String BATCH = "batch";
	public final static String ZIPKIN = "zipkin";
	public final static String ZIPKIN_TYPE_PREFIX = "z$";

	public final static String DATASOURCE = "datasource";
	public final static String CONTEXT = "context";
	public final static String REQUESTPROCESS = "reqproc";

	public final static String MARIA_DB = "mariadb";
	public final static String MARIA_PLUGIN = "mariaplugin";

	public final static String CUBRID_AGENT = "cubridagent";
	
	public final static String WAS_TPS = "TPS";
	public final static String WAS_ACTIVE_SPEED = "ActiveSpeed";
	public final static String WAS_ELAPSED_TIME = "ElapsedTime";
	public final static String WAS_ELAPSED_90PCT = "Elapsed90%";
	public final static String WAS_SERVICE_COUNT = "ServiceCount";
	public final static String WAS_ERROR_RATE = "ErrorRate";
	public final static String WAS_ACTIVE_SERVICE = "ActiveService";
	public final static String WAS_RECENT_USER = "RecentUser";
	public final static String WAS_SQL_ELAPSED_TIME_BY_SERVICE = "SqlTimeByService";
	public final static String WAS_APICALL_ELAPSED_TIME_BY_SERVICE	 = "ApiTimeByService";
	public final static String WAS_QUEUING_TIME = "QueuingTime";

	public final static String JAVA_GC_COUNT = "GcCount";
	public final static String JAVA_GC_TIME = "GcTime";
	public final static String JAVA_HEAP_TOT_USAGE = "HeapTotUsage";
	public final static String JAVA_HEAP_USED = "HeapUsed";
	public final static String JAVA_HEAP_TOTAL = "HeapTotal";
	public final static String JAVA_CPU_TIME = "CpuTime";
	public final static String JAVA_PERM_USED = "PermUsed";
	public final static String JAVA_PERM_PERCENT = "PermPercent";
	public final static String JAVA_PROCESS_CPU = "ProcCpu";
	public final static String JAVA_FD_USAGE = "FdUsage";

	public final static String GO_GOROUTINE = "Goroutine";
	public final static String GO_CGO_CALL = "GoCgoCall";

	public final static String GO_GC_COUNT = "GoGcCount";
	public final static String GO_GC_PAUSE = "GoGcPause";
	public final static String GO_HEAP_USED = "GoHeapUsed";

	public final static String REQUESTPROCESS_BYTES_RECEIVED = "BytesReceived";
	public final static String REQUESTPROCESS_BYTES_SENT = "BytesSent";
	public final static String REQUESTPROCESS_ERROR_COUNT = "ErrorCount";
	public final static String REQUESTPROCESS_PROCESSING_TIME = "ProcessingTime";
	public final static String REQUESTPROCESS_REQUEST_COUNT = "RequestCount";

	public final static String CONTEXT_ACTIVE_SESSIONS = "ActiveSessions";
	public final static String CONTEXT_SESSION_CREATE_RATE = "SessionCreateRate";
	public final static String CONTEXT_SESSION_EXPIRED_RATE = "SessionExpiredRate";

	public final static String DATASOURCE_CONN_ACTIVE = "ConnActive";
	public final static String DATASOURCE_CONN_IDLE = "ConnIdle";
	public final static String DATASOURCE_CONN_MAX = "ConnMax";

	public final static String HOST_CPU = "Cpu";
	public final static String HOST_SYSCPU = "SysCpu";
	public final static String HOST_USERCPU = "UserCpu";
	public final static String HOST_MEM = "Mem";
	public final static String HOST_MEM_AVALIABLE = "MemA";
	public final static String HOST_MEM_USED = "MemU";
	public final static String HOST_MEM_TOTAL = "MemT";
	public final static String HOST_SWAP_PAGE_IN = "PageIn";
	public final static String HOST_SWAP_PAGE_OUT = "PageOut";
	public final static String HOST_SWAP = "Swap";
	public final static String HOST_SWAP_USED = "SwapU";
	public final static String HOST_SWAP_TOTAL = "SwapT";
	public final static String PROC_CPU = "ProcCpu";
	
	public final static String DB_WAIT_COUNT = "WAIT_COUNT";
	
	// public final static String HOST_READ_COUNT = "ReadCount";
	// public final static String HOST_WRITE_COUNT = "WriteCount";
	// public final static String HOST_READ_BYTES = "ReadBytes";
	// public final static String HOST_WRITE_BYTES = "WriteBytes";
	// public final static String HOST_READ_TIME = "ReadTime";
	// public final static String HOST_WRITE_TIME = "WriteTime";
	public final static String HOST_NET_IN = "NetInBound";
	public final static String HOST_NET_OUT = "NetOutBound";
	public final static String HOST_TCPSTAT_SS = "TcpStatSynSent";
	public final static String HOST_TCPSTAT_SR = "TcpStatSynReceive";
	public final static String HOST_TCPSTAT_EST = "TcpStatEST";
	public final static String HOST_TCPSTAT_TIM = "TcpStatTIM";
	public final static String HOST_TCPSTAT_FIN = "TcpStatFIN";
	public final static String HOST_TCPSTAT_CLS = "TcpStatCLS";

	public final static String HOST_NET_RX_BYTES = "NetRxBytes";
	public final static String HOST_NET_TX_BYTES = "NetTxBytes";

	public final static String HOST_DISK_READ_BYTES = "DiskReadBytes";
	public final static String HOST_DISK_WRITE_BYTES = "DiskWriteBytes";

	public final static String REAL_TIME_ALL = "rt-all";
	public final static String REAL_TIME_TOTAL = "rt-tot";
	public final static String TODAY_ALL = "td-all";
	public final static String TODAY_TOTAL = "td-tot";

	public final static String PAST_TIME_ALL = "pt-all";
	public final static String PAST_TIME_TOTAL = "pt-tot";
	public final static String PAST_DATE_ALL = "pd-all";
	public final static String PAST_DATE_TOTAL = "pd-tot";

	public final static String REAL_TIME = "rt";
	public final static String TODAY = "td";
	public final static String PAST_TIME = "pt";
	public final static String PAST_DATE = "pd";

	public final static String[] COUNTER_MENU_ARRAY = { REAL_TIME_ALL, REAL_TIME_TOTAL, TODAY_ALL, TODAY_TOTAL,
			PAST_TIME_ALL, PAST_TIME_TOTAL, PAST_DATE_ALL, PAST_DATE_TOTAL };
	
	public static final String WAS_SQL_TIME = "SqlTime";
	public static final String WAS_SQL_TPS = "SqlTPS";
	public static final String WAS_SQL_ERROR_RATE = "SqlErrorRate";

	public static final String WAS_APICALL_TIME = "ApiTime";
	public static final String WAS_APICALL_TPS = "ApiTPS";
	public static final String WAS_APICALL_ERROR_RATE = "ApiErrorRate";
	
	public static final String BATCH_SERVICE = "BatchService";
	public static final String BATCH_START = "BatchStart";
	public static final String BATCH_END = "BatchEnd";
	public static final String BATCH_ENDNOSIGNAL = "BatchEndNoSignal";

	// interaction counters
	public static final String INTR_API_OUTGOING = "INTR_API_OUTGOING";
	public static final String INTR_NORMAL_OUTGOING = "INTR_NORMAL_OUTGOING";
	public static final String INTR_API_INCOMING = "INTR_API_INCOMING";
	public static final String INTR_NORMAL_INCOMING = "INTR_NORMAL_INCOMING";
	public static final String INTR_DB_CALL = "INTR_DB_CALL";
	public static final String INTR_REDIS_CALL = "INTR_REDIS_CALL";
	public static final String INTR_KAFKA_CALL = "INTR_KAFKA_CALL";
	public static final String INTR_RABBITMQ_CALL = "INTR_RABBITMQ_CALL";
	public static final String INTR_ELASTICSEARCH_CALL = "INTR_ELASTICSEARCH_CALL";
	public static final String INTR_MONGODB_CALL = "INTR_MONGODB_CALL";
}
