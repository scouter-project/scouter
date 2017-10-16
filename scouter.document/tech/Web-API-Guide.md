# Scouter Web API Guide
![English](https://img.shields.io/badge/language-English-orange.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Web-API-Guide_kr.md)

## Run API Server

### Embedded Mode Run - Run with scouter collector server
 - set configurations below (Collector configuration)
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

@ConfigDesc("Log directory")
public String log_dir = "./logs";
@ConfigDesc("Keeping period of log")
public int log_keep_days = 30;
```

## APIs
- **Context root : /scouter**
  - if the api url is ```/v1/info/server``` then ```/scouter/v1/info/server```

#### - `GET /v1/info/server`
 - get connected collector server info.
 - Auth : None

#### - `GET /v1/object`
 - get monitoring object list
 - **Auth** : required - register api client's ip to ```net_http_api_allow_ips``` configuration.
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

#### - `GET /v1/counter/stat/{counters}/ofType/{objType}`
 - get 5min counter statistics by object type
 - **Auth** : required
 - **Path params**
   - `counters` : (required)
   - `objType` : (required)
 - **Query params**
   - `fromYmd` : yyyymmdd (required)
   - `toYmd` : yyyymmdd (required)
   - `serverId` : (optional if single server)

#### - `GET /v1/summary/service/ofType/{objType}`
 - get service summary
 - **Auth** : required
 - **Path params**
   - `objType` : (required)
 - **Query params**
   - `startYmdHm` : yyyymmddhhmi (exclusive required with startTimeMillis)
   - `endYmdHm` : yyyymmddhhmi (exclusive required with endTimeMillis)
   - `startTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with startYmdHm)
   - `endTimeMillis` : timestamp(long) - yyyymmddhhmi (exclusive required with endYmdHm)
   - `serverId` : (optional if single server)


