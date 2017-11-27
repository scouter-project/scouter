# Scouter Web API Guide
[![English](https://img.shields.io/badge/language-English-orange.svg)](Web-API-Guide.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Web-API-Guide_kr.md)

## Run Scouter API server

### Embedded Mode Run - Run with scouter collector server
 - set configurations below (Collector configuration - need to restart collector server)
    - `net_http_server_enabled` : set `true`
    - `net_http_api_enabled` : set `true`
    - `net_http_port` : default value `6180`
    - `net_http_api_allow_ips` : default value `localhost,127.0.0.1,0:0:0:0:0:0:0:1,::1`;

### StandAlone Mode Run
 - Web api needs much more memory and cpu usage than scouter collector server because of servlet processing, JSON parsing and another workings.
   So It needs to be separate this web api server from the collector server if your collector server has performance issue.
 - Run standAlone webapp (Scouter full packaging version includes scouter.webapp)
   ```bash
   cd scouter.webapp
   ./startup.sh
   ```
 - configure before run scouter webapp
   - ```net_collector_ip_port_id_pws``` : default value `127.0.0.1:6100:admin:admin`
     - format : `{host}:{port}:{id}:{pw},{host}:{port}:{id}:{pw}`
   - ```net_http_port``` : default value `6188`
   - `net_http_api_allow_ips` : default value `localhost,127.0.0.1,0:0:0:0:0:0:0:1,::1`;

### Testing
 - access the url : http://{http-server-ip}:{port}/scouter/v1/info/server
 - expected response
   ```javascript
   {
   	"status": 200,
   	"requestId": "#xxxx",
   	"resultCode": 0,
   	"message": "success",
   	"result": [
   		{
   			"id": -1234512345,
   			"name": "MyCollectorName",
   			"connected": true,
   			"serverTime": 1507878605943
   		}
   	]
   }
   ```

### Swagger
 - **SWAGGER uri : /swagger/index.html**
    - if set the option `net_http_api_swagger_enabled` true
 - **demo**
    - [http://demo.scouterapm.com:6180/swagger/index.html](http://demo.scouterapm.com:6180/swagger/index.html)

## Configuration
```java

@ConfigDesc("Collector connection infos - eg) host:6100:id:pw,host2:6100:id2:pw2")
@ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
public String net_collector_ip_port_id_pws = "127.0.0.1:6100:admin:admin";

@ConfigDesc("size of webapp connection pool to collector")
public int net_webapp_tcp_client_pool_size = 12;
@ConfigDesc("timeout of web app connection pool to collector(It depends on net_tcp_client_so_timeout_ms)")
public int net_webapp_tcp_client_pool_timeout = 15000;

@ConfigDesc("Enable api access control by client ip")
public boolean net_http_api_auth_ip_enabled = true;
@ConfigDesc("If get api caller's ip from http header.")
public String net_http_api_auth_ip_header_key;

@ConfigDesc("Enable api access control by JSESSIONID of Cookie")
public boolean net_http_api_auth_session_enabled = true;
@ConfigDesc("api http session timeout")
public int net_http_api_session_timeout = 3600*24;

@ConfigDesc("api access allow ip addresses")
@ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
public String net_http_api_allow_ips = "localhost,127.0.0.1,0:0:0:0:0:0:0:1,::1";

@ConfigDesc("HTTP service port")
public int net_http_port = NetConstants.WEBAPP_HTTP_PORT;

@ConfigDesc("HTTP API swagger enable option")
public boolean net_http_api_swagger_enabled = false;
@ConfigDesc("Swagger option of host's ip or domain to call APIs.")
public String net_http_api_swagger_host_ip = "";
@ConfigDesc("API CORS support for Access-Control-Allow-Origin")
public String net_http_api_cors_allow_origin = "";
@ConfigDesc("Access-Control-Allow-Credentials")
public String net_http_api_cors_allow_credentials = "false";

@ConfigDesc("Log directory")
public String log_dir = "./logs";
@ConfigDesc("Keeping period of log")
public int log_keep_days = 30;
```

## APIs
- **Context root : /scouter**
  - if the api url is `/v1/info/server` then `/scouter/v1/info/server`

- **SWAGGER uri : /swagger/index.html**
  - if set the option `net_http_api_swagger_enabled` true

#### - `GET /v1/info/server`
 - get connected collector server info.
 - Auth : None

#### - `GET /v1/object`
 - get monitoring object list
 - **Auth** : required - register api client's ip to `net_http_api_allow_ips` configuration.
 - **Query params**
    - `serverId` : If the webapp connect to single collector then it's optional.(optional if single server)

#### - `GET /v1/counter/realTime/{counters}/ofType/{objType}`
 - get real time counter value by object type
 - **Auth** : required
 - **Path params**
   - `counters` : counter names comma separated (required)
     - refer to [counter names](https://github.com/scouter-project/scouter/blob/master/scouter.common/src/main/resources/scouter/lang/counters/counters.xml)
     - eg) Cpu,Mem,PageIn
   - `objType` : object type (required)
 - **Query params**
   - `serverId` : If the webapp connect to single collector then it's optional.(optional if single server)

#### - `GET /v1/counter/realTime/{counters}/ofObject/{objHash}`
 - get real time counter value by object
 - **Auth** : required
 - **Path params**
   - `counters` : (required)
   - `objHash` : object id (required)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `GET /v1/counter/realTime/{counters}`
 - get real time counter value by objects
 - **Auth** : required
 - **Path params**
   - `counters` : counter names comma separated (required)
 - **Query params**
   - `serverId` : If the webapp connect to single collector then it's optional.(optional if single server)
   - `objHashes` : object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]

#### - `GET /v1/counter/{counter}/ofType/{objType}`
 - get counter values of specific time range by object type
   - uri pattern : /counter/{counter}/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
   - uri pattern : /counter/{counter}/ofType/{objType}?startYmdHms={startYmdHms}&endYmdHms={endYmdHms}&serverId={serverId}
 - **Auth** : required
 - **Path params**
   - `counter` : (required)
   - `objType` : object type (required)
 - **Query params**
   - `serverId` : (optional if single server)
   - `startYmdHms` : yyyymmddhhmiss (exclusive required with startTimeMillis)
   - `endYmdHms` : yyyymmddhhmiss (exclusive required with endTimeMillis)
   - `startTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with startYmdHms)
   - `endTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with endYmdHms)

#### - `GET /v1/counter/{counter}`
 - get counter values of specific time range by objects
   - uri pattern : /counter/{counter}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&objHashes=100,200&serverId={serverId}
   - uri pattern : /counter/{counter}?startYmdHms={startYmdHms}&endYmdHms={endYmdHms}&objHashes=100,200&serverId={serverId}
 - **Auth** : required
 - **Path params**
   - `counter` : (required)
 - **Query params**
   - `serverId` : (optional if single server)
   - `objHashes` : object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]
   - `startYmdHms` : yyyymmddhhmiss (exclusive required with startTimeMillis)
   - `endYmdHms` : yyyymmddhhmiss (exclusive required with endTimeMillis)
   - `startTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with startYmdHms)
   - `endTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with endYmdHms)

#### - `GET /v1/counter/{counter}/latest/{latestSec}/ofType/{objType}`
 - get latest counter values by object type
   - uri pattern : /counter/{counter}/latest/{latestSec}/ofType/{objType}?serverId={serverId}
 - **Auth** : required
 - **Path params**
   - `counter` : (required)
   - `latestSec` : (required) seconds to retrieve counter values from now.
   - `objType` : (required)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `GET /v1/counter/{counter}/latest/{latestSec}`
 - get latest counter values by objects
   - uri pattern : /counter/{counter}/latest/{latestSec}?serverId={serverId}&objHashes=100,200
 - **Auth** : required
 - **Path params**
   - `counter` : (required)
   - `latestSec` : (required) seconds to retrieve counter values from now.
 - **Query params**
   - `serverId` : (optional if single server)
   - `objHashes` : object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]

#### - `GET /v1/counter/stat/{counters}/ofType/{objType}`
 - get 5min counter statistics by object type
 - **Auth** : required
 - **Path params**
   - `counters` : (required)
   - `objType` : (required)
 - **Query params**
   - `startYmd` : yyyymmdd (required)
   - `endYmd` : yyyymmdd (required)
   - `serverId` : (optional if single server)

#### - `GET /v1/counter/stat/{counters}`
 - get 5min counter statistics by object type
 - **Auth** : required
 - **Path params**
   - `counters` : (required)
 - **Query params**
   - `startYmd` : yyyymmdd (required)
   - `endYmd` : yyyymmdd (required)
   - `objHashes` : object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]
   - `serverId` : (optional if single server)

#### - `GET /v1/summary/{summaryCategory}/ofType/{objType}`
 - get summary of given category
 - **Auth** : required
 - **Path params**
   - `summaryCategory` : service, sql, apiCall, ip, userAgent, error, alert (required)
   - `objType` : (required)
 - **Query params**
   - `startYmdHm` : yyyymmddhhmi (exclusive required with startTimeMillis)
   - `endYmdHm` : yyyymmddhhmi (exclusive required with endTimeMillis)
   - `startTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with startYmdHm)
   - `endTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with endYmdHm)
   - `serverId` : (optional if single server)

#### - `GET /v1/summary/{summaryCategory}/ofObject/{objHash}`
 - get summary of given category
 - **Auth** : required
 - **Path params**
   - `summaryCategory` : service, sql, apiCall, ip, userAgent, error, alert (required)
   - `objHash` : object id (required)
 - **Query params**
   - `startYmdHm` : yyyymmddhhmi (exclusive required with startTimeMillis)
   - `endYmdHm` : yyyymmddhhmi (exclusive required with endTimeMillis)
   - `startTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with startYmdHm)
   - `endTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with endYmdHm)
   - `serverId` : (optional if single server)

- get xlog data within the time range
 - **Auth** : required
 - **Path params**
   - `yyyymmdd` : date to search xlogs
 - **Query params**
   - `startTimeMillis` : (required) start time as milliseconds(long)
   - `endTimeMillis` : (required) end time as milliseconds(long)
   - `startHms` : (required-exclusive with starTimeMillis) start time as hhmmss
   - `endHms` : (required-exclusive with endTimeMillis) end time as hhmmss
   - `objHashes` : object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]
   - `pageCount` : count to retrieve in one time. (max limit is 30,000, default 10,000)
   - `lastTxid` : available from previous response for paging support.
   - `lastXLogTime` : available from previous response for paging support.
   - `serverId` : (optional if single server)

#### - `GET /v1/xlog-data/{yyyymmdd}/{txid}`
 - get xlog data by txid
 - **Auth** : required
 - **Path params**
   - `yyyymmdd` : date to search xlogs
   - `txid` : XLog's txid (long)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `GET /v1/xlog-data/{yyyymmdd}/gxid/{gxid}`
 - get xlog datas by gxid
 - **Auth** : required
 - **Path params**
   - `yyyymmdd` : date to search xlogs
   - `gxid` : XLog's gxid (long)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `GET /v1/xlog-data/search/{yyyymmdd}`
 - search xlog data list with various conditions
 - **Auth** : required
 - **Path params**
   - `yyyymmdd` : date to search xlogs
 - **Query params**
   - `serverId` : (optional if single server)
   - `startTimeMillis` : (required) start time as milliseconds(long)
   - `endTimeMillis` : (required) end time as milliseconds(long)
   - `startHms` : (required-exclusive with starTimeMillis) start time as hhmmss
   - `endHms` : (required-exclusive with endTimeMillis) end time as hhmmss
   - `objHash` : object hash
   - `service` : service(pattern)
   - `ip` : ip(pattern)
   - `login` : login(pattern)
   - `desc` : desc(pattern)
   - `text1` : text1(pattern)
   - `text2` : text2(pattern)
   - `text3` : text3(pattern)
   - `text4` : text4(pattern)
   - `text5` : text5(pattern)

#### - `GET /v1/xlog-data/realTime/{offset1}/{offset2}`
 - get current xlog data created after the last searched.
 - **Auth** : required
 - **Path params**
   - `offset1` : the last xlog (loop) offset previously retrieved (initial value is 0)
   - `offset2` : the last xlog offset previously retrieved (initial value is 0)
 - **Query params**
   - `objHashes` : object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]
   - `serverId` : (optional if single server)

#### - `GET /v1/profile-data/{yyyymmdd}/{txid}`
 - get profile data(decoded) from txid
 - **Auth** : required
 - **Path params**
   - `yyyymmdd` : date to search xlogs
   - `txid` : XLog's txid (long)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `GET /v1/activeService/stepCount/ofType/{objType}`
 - current active service count 3-stepped by response time.
 - **Auth** : required
 - **Path params**
   - `objType` : (required)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `GET /v1/activeService/ofType/{objType}`
 - get active service list of given objType
 - **Auth** : required
 - **Path params**
   - `objType` : (required)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `GET /v1/activeService/ofObject/{objHash}`
 - get active service list of given objHash
 - **Auth** : required
 - **Path params**
   - `objHash` : (required)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `GET /v1/activeService/thread/{threadId}/ofObject/{objHash}`
 - get thread detail of the object's threadId
 - **Auth** : required
 - **Path params**
   - `objHash` : (required)
   - `threadId` : thread id gotten from active service list (required)
 - **Query params**
   - `txidName` : This value is for valuable service related information. (like service name & a sql that currently running)
   - `txid` : This value has higher priority than txidName.(txidName is String type from Hexa32.toString32(txid) / txid is long type)
   - `serverId` : (optional if single server)

#### - `GET /v1/alert/realTime/{offset1}/{offset2}`
 - retrieve current alerts unread that is produced newly after the last offsets.
 - **Auth** : required
 - **Path params**
   - `offset1` : (required)
   - `offset2` : (required)
 - **Query params**
   - `objType` : (required)
   - `serverId` : (optional if single server)

#### - `GET /v1/dictionary/{yyyymmdd}`
 - get text values from dictionary keys requested
 - **Auth** : required
  - **Path params**
    - `yyyymmdd` : (required)
  - **Query params**
    - `texts` : group of text types & text hashes (required)
      - eg) texts=[service:10001,service:10002,obj:20001,sql:55555] (bracket is optional)

#### - `GET /v1/visitor/realTime/ofType/{objType}`
 - retrieve today visitor count by objType
 - **Auth** : required
 - **Path params**
   - `objType` : (required)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `GET /v1/visitor/realTime`
 - retrieve today visitor count by objects
 - **Auth** : required
 - **Query params**
   - `objHashes` : object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]
   - `serverId` : (optional if single server)

#### - `GET /v1/object/host/realTime/top/ofObject/{objHash}`
 - retrieve all OS processes cpu, memory usage of the given object
 - **Auth** : required
 - **Path params**
   - `objHash` : (required)
 - **Query params**
   - `serverId` : (optional if single server)




