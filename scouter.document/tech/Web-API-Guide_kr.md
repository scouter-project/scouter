# Scouter Web API Guide
[![English](https://img.shields.io/badge/language-English-orange.svg)](Web-API-Guide.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Web-API-Guide_kr.md)

## Scouter API Server 실행

### Embedded Mode로 실행 - 수집서버(Collector)에서 실행
 - 수집서버에 아래 옵션을 설정하면 수집서버와 같이 실행된다.(재기동 필요)
    - `net_http_server_enabled` : set `true`
    - `net_http_api_enabled` : set `true`
    - `net_http_port` : default value `6180`
    - `net_http_api_allow_ips` : default value `localhost,127.0.0.1,0:0:0:0:0:0:0:1,::1`
    - `net_collector_ip_port_id_pws` : default value 127.0.0.1:6100:admin:admin
       - format : {host}:{port}:{id}:{pw},{host}:{port}:{id}:{pw}

### StandAlone Mode로 실행
 - Web API는 Servlet을 통해 서비스 되면 HTTP 프로토콜이나 JSON 파싱 등 부가적인 작업이 필요하기 때문에 기본 수집서버에 비해 자원 사용량이 높다.
   따라서 모니터링하는 시스템의 규모가 크다면 API 서버를 분리하여 실행할 필요가 있다.
 - standAlone webapp 실행 (Scouter Full 패키지에 포함되어 있음)
   ```bash
   cd scouter.webapp
   ./startup.sh
   ```
 - scouter web app 설정 (실행전에 설정 필요)
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
    - `net_http_api_swagger_enabled` 옵션을 true로 지정시 사용 가능
 - **demo**
    - [http://demo.scouterapm.com:6180/swagger/index.html](http://demo.scouterapm.com:6180/swagger/index.html)

## Counter widget
Provides a widget that simply charts the performance counter API's response.
Passing the (url encoded) url of the counter api to be looked up in this widget, the result is charted.
If you include this widget path when setting up a custom alarm, you can use it more easily.
 - [alarm sample](../main/Alert-Plugin-Guide_kr.md)

 - **URL : `/widget/simple/counter.html`**
   - **parameters**
     - **source** : url encoded counter(or counter stat) api
       - if the source value doesn't start with `slash(/)`, then append `/scouter/v1/counter/` ahead of it by defaults.
 - **usage example**
   - belows are equivalent. (**counter api** : `/scouter/v1/counter/HeapUsed/latest/300/ofType/tomcat`)
     - `http://127.0.0.1:6180/widget/simple/counter.html?source=%2Fscouter%2Fv1%2Fcounter%2FHeapUsed%2Flatest%2F300%2FofType%2Ftomcat`
     - `http://127.0.0.1:6180/widget/simple/counter.html?source=HeapUsed%2Flatest%2F300%2FofType%2Ftomcat`

## Configuration
```java

//Network
@ConfigDesc("Collector connection infos - eg) host:6100:id:pw,host2:6100:id2:pw2")
public String net_collector_ip_port_id_pws = "127.0.0.1:6100:admin:admin";

@ConfigDesc("size of webapp connection pool to collector")
public int net_webapp_tcp_client_pool_size = 100;
@ConfigDesc("timeout of web app connection pool to collector")
public int net_webapp_tcp_client_pool_timeout = 60000;
@ConfigDesc("So timeout of web app to collector")
public int net_webapp_tcp_client_so_timeout = 30000;

@ConfigDesc("Enable api access control by client ip")
public boolean net_http_api_auth_ip_enabled = false;
@ConfigDesc("If get api caller's ip from http header.")
public String net_http_api_auth_ip_header_key;

@ConfigDesc("Enable api access control by JSESSIONID of Cookie - get session from /user/login.")
public boolean net_http_api_auth_session_enabled = false;
@ConfigDesc("api http session timeout(sec)")
public int net_http_api_session_timeout = 1*3600*24;
@ConfigDesc("Enable api access control by Bearer token(of Authorization http header) - get access token from /user/loginGetToken.")
public boolean net_http_api_auth_bearer_token_enabled = false;
@ConfigDesc("Enable gzip response on api call")
public boolean net_http_api_gzip_enabled = true;

@ConfigDesc("api access allow ip addresses")
public String net_http_api_allow_ips = "localhost,127.0.0.1,0:0:0:0:0:0:0:1,::1";

@ConfigDesc("HTTP service port")
public int net_http_port = NetConstants.WEBAPP_HTTP_PORT;

@ConfigDesc("user extension web root")
public String net_http_extweb_dir = "./extweb";

@ConfigDesc("HTTP API swagger enable option")
public boolean net_http_api_swagger_enabled = false;

@ConfigDesc("Swagger option of host's ip or domain to call APIs.")
public String net_http_api_swagger_host_ip = "";
@ConfigDesc("API CORS support for Access-Control-Allow-Origin")
public String net_http_api_cors_allow_origin = "*";
@ConfigDesc("Access-Control-Allow-Credentials")
public String net_http_api_cors_allow_credentials = "true";

@ConfigDesc("Log directory")
public String log_dir = "./logs";
@ConfigDesc("Keeping period of log")
public int log_keep_days = 30;

@ConfigDesc("temp dir")
public String temp_dir = "./tempdata";

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

#### - `GET /v1/object/remove/inactive`
 - remove inactive object.
 - **Auth** : required
 
#### - `GET /v1/object/threadList/{objHash}`
 - get agent thread list
 - **Auth** : required - register api client's ip to `net_http_api_allow_ips` configuration.
 - **Path params**
    - `objHash` : object id (required)
 - **Query params**
    - `serverId` : If the webapp connect to single collector then it's optional.(optional if single server)
 
#### - `GET /v1/object/threadDump/{objHash}`
 - get agent thread dump info
 - **Auth** : required - register api client's ip to `net_http_api_allow_ips` configuration.
 - **Path params**
    - `objHash` : object id (required)
 - **Query params**
    - `serverId` : If the webapp connect to single collector then it's optional.(optional if single server)
 
#### - `GET /v1/object/heapHistogram/{objHash}`
 - get agent heap histogram info
 - **Auth** : required - register api client's ip to `net_http_api_allow_ips` configuration.
 - **Path params**
    - `objHash` : object id (required)
 - **Query params**
    - `serverId` : If the webapp connect to single collector then it's optional.(optional if single server)
 
#### - `GET /v1/object/env/{objHash}`
 - get agent environment info
 - **Auth** : required - register api client's ip to `net_http_api_allow_ips` configuration.
 - **Path params**
    - `objHash` : object id (required)
 - **Query params**
    - `serverId` : If the webapp connect to single collector then it's optional.(optional if single server)

#### - `GET /v1/object/socket/{objHash}`
 - get agent socket info
 - **Auth** : required - register api client's ip to `net_http_api_allow_ips` configuration.
 - **Path params**
    - `objHash` : object id (required)
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

#### - `GET /v1/xlog-data/{yyyymmdd}/multi/{txidList}`
 - request xlogs by txids
 - **Auth** : required
 - **Path params**
   - `yyyymmdd` : date to search xlogs
   - `txidList` : xlog txid list(by comma separator)
 - **Query params**
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

#### - `GET /v1/object/host/realTime/disk/ofObject/{objHash}`
- retrieve all disk usage of the given object
- **Auth** : required
- **Path params**
  - `objHash` : object id (required)
- **Query params**
  - `serverId` : server id (optional if single server)
   
#### - `GET /v1/kv-private/{key}`
 - get value of given key from the scouter server's key-value store. (in user-scope private key space for logon user)
 - **Auth** : required
 - **Path params**
   - `key` : (required)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `PUT /v1/kv-private`
 - store it to the scouter server's key-value store. (in user-scope private key space for logon user)
 - **Auth** : required
 - **Request body (type : application/json)**
   - `key` : (required)
   - `value` : (required)
   - `ttl` : (optional) time to live as seconds
   - `serverId` : (required)

#### - `PUT /v1/kv-private/{key}/:ttl`
 - store it to the scouter server's key-value store. (in user-scope private key space for logon user)
 - **Auth** : required
 - **Path params**
    - `key` : (required)
 - **Request body (type : application/json)**
   - `ttl` : (required) time to live as seconds
   - `serverId` : (required)

#### - `GET /v1/kv-private/{keys}/:bulk`
 - get values of given keys from the scouter server's key-value store. (in user-scope private key space for logon user)
 - **Auth** : required
 - **Path params**
   - `keys` : (required) keys by comma separator. also allowed with bracket. eg) mykey-1,mykey2 or [mykey-1,mykey2]
 - **Query params**
   - `serverId` : (optional if single server)

#### - `PUT /v1/kv-private/:bulk`
 - store key&values to the scouter server's key-value store. (in user-scope private key space for logon user)
 - **Auth** : required
 - **Request body (type : application/json)**
   - `ttl` : (optional) time to live as seconds
   - `kvList` : (required) array of key & value
      - `key` : (required)
      - `value` : (required)
   - `serverId` : (required)

#### - `GET /v1/kv/{key}`
 - get value of given key from the scouter server's key-value store. (in the global key space)
 - **Auth** : required
 - **Path params**
   - `key` : (required)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `PUT /v1/kv`
 - store it to the scouter server's key-value store. (in the global key space)
 - **Auth** : required
 - **Request body (type : application/json)**
   - `key` : (required)
   - `value` : (required)
   - `ttl` : (optional) time to live as seconds
   - `serverId` : (required)

#### - `PUT /v1/kv/{key}/:ttl`
 - store it to the scouter server's key-value store. (in the global key space)
 - **Auth** : required
 - **Path params**
    - `key` : (required)
 - **Request body (type : application/json)**
   - `ttl` : (required) time to live as seconds
   - `serverId` : (required)

#### - `GET /v1/kv/{keys}/:bulk`
 - get values of given keys from the scouter server's key-value store. (in the global key space)
 - **Auth** : required
 - **Path params**
   - `keys` : (required) keys by comma separator. also allowed with bracket. eg) mykey-1,mykey2 or [mykey-1,mykey2]
 - **Query params**
   - `serverId` : (optional if single server)

#### - `PUT /v1/kv/:bulk`
 - store key&values to the scouter server's key-value store. (in the global key space)
 - **Auth** : required
 - **Request body (type : application/json)**
   - `ttl` : (optional) time to live as seconds
   - `kvList` : (required) array of key & value
      - `key` : (required)
      - `value` : (required)
   - `serverId` : (required)

#### - `GET /v1/kv/space/{keySpace}/{key}`
 - get value of given key from the key space of scouter server's key-value store.
 - **Auth** : required
 - **Path params**
   - `keySpace` : (required)
   - `key` : (required)
 - **Query params**
   - `serverId` : (optional if single server)

#### - `PUT /v1/kv/space/{keySpace}`
 - store it to the key space of scouter server's key-value store
 - **Auth** : required
 - **Path params**
    - `keySpace` : (required)
 - **Request body (type : application/json)**
   - `key` : (required)
   - `value` : (required)
   - `ttl` : (optional) time to live as seconds
   - `serverId` : (required)

#### - `GET /v1/kv/space/{keySpace}/{keys}/:bulk`
 - get values of given keys from the key space of scouter server's key-value store
 - **Auth** : required
 - **Path params**
   - `keySpace` : (required)
   - `keys` : (required) keys by comma separator. also allowed with bracket. eg) mykey-1,mykey2 or [mykey-1,mykey2]
 - **Query params**
   - `serverId` : (optional if single server)

#### - `PUT /v1/kv/space/{keySpace}/:bulk`
 - store key&values to the key space of scouter server's key-value store
 - **Auth** : required
 - **Path params**
    - `keySpace` : (required)
 - **Request body (type : application/json)**
   - `ttl` : (optional) time to live as seconds
   - `kvList` : (required) array of key & value
      - `key` : (required)
      - `value` : (required)
   - `serverId` : (required)

#### - `POST /v1/user/loginGetToken`
 - login with id & password, and get bearer token.
   - this token required on Authorization header for authorized request.
     - auth header example : `Authorization: Bearer V1.B3R4FSGEF3POJ.me`
 - **Auth** : none
 - **Request body (type : application/json)**
   - `user` : (required)
     - `id` : (required)
     - `password` : (required)
   - `serverId` : (required)

#### - `POST /v1/user/login`
 - login with id & password for traditional web application.
   - this api is answered including with SET-COOKIE response header.
 - **Auth** : none
 - **Request body (type : application/json)**
   - `user` : (required)
     - `id` : (required)
     - `password` : (required)
   - `serverId` : (required)

#### - `POST /v1/shortener`
 - make shorten url. 
   - You can make the url with a lot of parameters shorten in scouter web api.  
 - **Auth** : required
 - **Query params**
    - `url` : url to shorten
    - `serverId` : (optional if single server)

#### - `GET /v1/interactionCounter/realTime`
 - get interaction counter values. 
   - 여러 jvm과 db, redis, 외부 호출등 연관된 시스템간의 성능 정보를 가져올 수 있다.
   - 예를 들면 각 구성 요소간 단위 시간당 호출 건수, 응답시간의 합, 에러의 총합 등의 정보를 제공해준다.
   - this counter is activated when the option `counter_interaction_enabled` is set `true` from java agent.     
 - **Auth** : required
 - **Query params**
    - `objHashes` : object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]
    - `serverId` : (optional if single server)

#### - `GET /v1/interactionCounter/realTime/ofType/{objType}`
 - Same as above  
 - **Auth** : required
 - **Path params**
    - `objType` : (required) 
 - **Query params**
    - `serverId` : (optional if single server)
