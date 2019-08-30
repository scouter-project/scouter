# Scouter Plugin Guide
[![English](https://img.shields.io/badge/language-English-orange.svg)](Plugin-Guide.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Plugin-Guide_kr.md)

이 글에서는 Scouter를 확장 가능하게 만들어 주는 Plugin 기능에 대해 설명한다. 
Scouter collector sever의 Plugin 기능을 통하여 scouter의 수집 데이터를 선처리 하거나 타 소프트웨어로 전송할 수 있으며 agent plugin을 통하여 특정 데이터를 선처리하거나 업무적으로 의미있는 데이터를 XLog나 프로파일에 추가할 수 있다. 

> Scouter plugin을 통해 다른 open source들과 쉽게 통합이 가능한다. 

Scouter의 프로파일은 collector server에 적용 가능한 **server plugin**, 그리고 Java agent에 적용할 수 있는 **agent Plugin**으로 구분이 되며 server plugin은 **scripting plugin**과 **built-in plugin**으로 나누어 진다. 
현재 agent plugin은 java agent용의 scripting plugin만 제공된다. 

## 제공되는 plugin 목록
아래 항목들은 scouter project에서 공식 제공되거나 contributor에 의해 작성된 plugin들이다. 

#### 1. server plugins
- **Sample**
  - **[scouter-plugin-server-null](https://github.com/scouter-project/scouter-plugin-server-null)** : 수집데이터를 단순히 출력해 주는 sample plugin

- **Alert**
  - **[scouter-plugin-server-email](https://github.com/scouter-contrib/scouter-plugin-server-alert-email)** : Scouter에서 발생하는 alert를 email로 전송하는 plugin
  - **[scouter-plugin-server-telegram](https://github.com/scouter-contrib/scouter-plugin-server-alert-telegram)** : Scouter에서 발생하는 alert를 telegram으로 전송하는 plugin
  - **[scouter-plugin-server-slack](https://github.com/scouter-contrib/scouter-plugin-server-alert-slack)** : Scouter에서 발생하는 alert를 slack으로 전송하는 plugin
  - **[scouter-plugin-server-line](https://github.com/scouter-contrib/scouter-plugin-server-alert-line)** : Scouter에서 발생하는 alert를 line으로 전송하는 plugin
  - **[scouter-plugin-server-dingtalk](https://github.com/scouter-contrib/scouter-plugin-server-alert-dingtalk)** : Scouter에서 발생하는 alert를 dingtalk으로 전송하는 plugin
    
- **Counter**
  - **[scouter-plugin-server-influxdb](https://github.com/scouter-contrib/scouter-plugin-server-influxdb)** : Scouter의 성능 counter 데이터를 시계열 DB인 influxDB로 연동하는 plugin

#### 2. agent plugins
* TBD


## Server Plugin 설명
**Scripting plugin은 코드 변경이 동적으로 load 및 compile되어 runtime에 즉시 반영되므로 debugging등에도 활용될 수 있다.**

### 1. Scripting Plugin
Scripting plugin은 scouter DB에 수집데이터가 저장되기전 호출된다. 
간단히 java 문법을 사용한 script를 사용하여 처리가 가능하며 특별한 옵션을 지정하지 않은 경우는 server를 실행한 directory의 **./plugin** 디렉토리에 plugin 파일을 작성할 수 있다. 
Scouter의 설치본에는 이에 대한 샘플이 포함되어 있으며 해당 파일명으로 로드되므로 파일명을 수정할 수는 없다. 
현재 제공되는 server plugin은 7가지 종류이며 각 파일명은 아래와 같다. 
* **alert.plug** - alert에 대한 전처리 plugin
* **counter.plug** - 성능카운터 정보에 대한 전처리 plugin
* **object.plug** - object 정보에 대한 전처리 plugin
* **summary.plug** - 성능 summary 정보에 대한 전처리 plugin
* **xlog.plug** - xlog data에 대한 전처리 plugin
* **xlogdb.plug** - xlog를 db에 저장히기 직전 상태에 호출되는 plugin
* **xlogprofile.plug** - 상세 profile 정보에 대한 전처리 plugin

이에 대한 상세 내용은 **[Scripting plugin Server API 설명 페이지](Server-Plugin-Scripting_kr.md)**를 참고한다.

### 2. Built-in Plugin
Scripting plugin은 간편하게 작성하고 바로 반영해 볼 수 있다는 장점이 있는 반면 영구적으로 사용해야 하는 Plugin이 필요한 경우는 오히려 쉽게 수정 가능한 text 파일 형태로 유지하는 것이 불편할 수 도 있다. 
이를 위해 특정한 방식으로 미리 제작해 놓은 plugin을 삽입할 수 있는 방식을 제공하며 이를 **Built-in Plugin**이라고 부른다. 
Scouter에서 미리 제공하는 **annotation**을 사용하여 개발한 후 해당 library를 scouter server의 library 경로에 넣어 놓기만 하면 자동으로 로딩되어 실행된다. 
(기본 설정으로는 **./lib** 디렉토리이며 동적로딩이 되지 않으므로 library 변경시 재시작이 필요하다.) 

#### 2.1 Server Built-in Plugin 개발 가이드
##### 1. dependency
 * scouter.common
 * scouter.server

##### 2. Annotation
```scouter.lang.plugin.annotation.ServerPlugin ```

* Example
```java
package scouter.plugin.server.none;
public class NullPlugin {
    Configure conf = Configure.getInstance();

	@ServerPlugin(PluginConstants.PLUGIN_SERVER_ALERT)
    public void alert(AlertPack pack){
        if(conf.getBoolean("ext_plugin_null_alert_enabled", true)) {
            println("[NullPlugin-alert] " + pack);
        }
    }
```

Plugin 개발시 아래 두가지 사항을 준수하여야 한다.
> 1. Annotation scan은 scouter.plugin.server 패키지 하위에서 수행하므로 **scouter.plugin.server** 하위에 작성되어야 한다. 
> 2. Scouter server의 configuration을 이용할 수 있으며 이때 **ext_plugin_xxx** 로 시작되어야 한다. 

```ServerPlugin``` annotation에 들어가는 value는 아래와 같이 6가지를 제공하며 기본값은 ```PLUGIN_SERVER_COUNTER```이다.

* **```PLUGIN_SERVER_COUNTER```**
* **```PLUGIN_SERVER_ALERT```**
* **```PLUGIN_SERVER_OBJECT```**
* **```PLUGIN_SERVER_SUMMARY```**
* **```PLUGIN_SERVER_XLOG```**
* **```PLUGIN_SERVER_PROFILE```**

#### 2.3. plugin sample
단순히 수집된 데이터를 출력하는 간단한 plugin 샘플을 제공한다. 
 * Sample plugin : [https://github.com/scouter-project/scouter-plugin-server-null](https://github.com/scouter-project/scouter-plugin-server-null)
 * Download : [scouter-plugin-server-null.jar](https://github.com/scouter-project/scouter-plugin-server-null/releases/download/v1.0/scouter-plugin-server-null.jar)

### 3. Alert scripting (type) plugin
Alert scripting plugin 을 통해 기본으로 제공하는 알람 조건외에  
사용자가 다양한 성능 counter의 값들을 자유롭게 조합하여 알람을 설정할 수 있는 기능을 제공한다. 
  * [Alert Plugin Guide](./Alert-Plugin-Guide_kr.md)

## Agent Plugin 설명 - Scripting Plugin

#### Java agent plugin
**Scripting Plugin은 코드 변경이 동적으로 Load 및 Compile되어 Runtime에 즉시 반영되므로 Debugging등에도 활용될 수 있다.**  
특히 특정 method의 파라미터 정보를 확인하거나 해당 위치에서 StackTrace를 유발시켜 호출 경로를 확인해 본다던지 다양한 방식으로 활용이 가능하다. 

Scripting Pluging은 응용 어플리케이션의 중요한 몇가지 point에서 각 plugin이 호출이 된다. 
간단히 Java 문법을 사용한 Script를 사용하여 처리가 가능하며 특별한 옵션을 지정하지 않은 경우는 **./plugin** 디렉토리에 plugin 파일을 작성할 수 있다. 
Scouter의 설치본에는 이에 대한 샘플이 포함되어 있으며 해당 파일명으로 로드되므로 파일명을 수정할 수는 없다. 
현재 제공되는 server plugin은 5가지 종류이며 각 파일명은 아래와 같다.

|파일명               |    설명                  |
|-------------------|-------------------------|
|**httpservice.plug**    | Http Service 시작점, Http Service이 종료시점에 호출됨. 특정 Service는 사용자가 정의한 조건에 따라 reject할 수 있는 기능을 제공 |
|**service.plug**        | Service의 시작점, 종료 시점에 호출됨 (```hook_service_patterns```를 통해 설정된 서비스가 대상이 됨) |
|**httpcall.plug**       | HttpClient등을 통해 http call을 사용하는 시점에 호출됨   |
|**capture.plug**        | ```hook_args_patterns```나 ```hook_constructor_patterns```등을 통해 설정됨 method에 대해 해당 method의 시작 시점, 종료시점, Constructor 생성 시점에 호출됨 |
|**jdbcpoolplug**        | DB connection URL 요청 시점에 호출됨 |

이에 대한 상세 내용은 **[Scripting plugin java agent API 설명 페이지](JavaAgent-Plugin-Scripting_kr.md)**를 참고한다.
