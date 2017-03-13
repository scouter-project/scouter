# Scouter Plugin Guide
![Englsh](https://img.shields.io/badge/language-English-orange.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Plugin-Guide_kr.md)

This article explains plugin funtion which enables scouter's extensibility. 

With the plugin fuction of Scouter collector sever, data collected by scouter can be modified and can be shared to other softwares. With agent plugin, certain types of data can be modified (선 처리의 개념이 모호하여  modifed 로 번역 하였으나 의미에 따라 pre-handled 등으로 변경 해야 할 수 도 있음.) and other business-meaningful data can be added data to XLog or profile. 

> With Scouter plugin, configuration and extension can be done to enable collarboaration with other open source.

Scouter's profiles has two parts. **server plug-in** is for to collector server and **agent Plugin** is for to Java agent. server plugin has two parts including **scripting plugin** and **built-in plugin**.
Currently, scripting plugin is only one available plugin for java agent. 

## List of available plugins
Below are the list of official plugins from scouter project and from contributors.

#### 1. server plugins
* **[scouter-plugin-server-null](https://github.com/scouter-project/scouter-plugin-server-null)** : sample plugin prints out data collected
* **[scouter-plugin-server-email](https://github.com/scouter-project/scouter-plugin-server-alert-email)** : emails alters from Scouter
* **[scouter-plugin-server-telegram](https://github.com/scouter-project/scouter-plugin-server-alert-telegram)** : transfer altert from Scouter to telegram
* **[scouter-plugin-server-slack](https://github.com/scouter-project/scouter-plugin-server-alert-slack)** : transfer altert from Scouter to slack
* **[scouter-plugin-server-influxdb](https://github.com/scouter-project/scouter-plugin-server-influxdb)** : transfer performance data from Scouter to influxDB(time series DB)

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

이에 대한 상세 내용은 **[Scripting plugin Server API 설명 페이지](Server-Plugin-Scripting.md)**를 참고한다.

### 2. Built-in Plugin
Scripting plugin은 간편하게 작성하고 바로 반영해 볼 수 있다는 장점이 있는 반면 영구적으로 사용해야 하는 Plugin이 필요한 경우는 오히려 쉽게 수정 가능한 text 파일 형태로 유지하는 것이 불편할 수 도 있다. 
이를 위해 특정한 방식으로 미리 제작해 놓은 plugin을 삽입할 수 있는 방식을 제공하며 이를 **Built-in Plugin**이라고 부른다. 
Scouter에서 미리 제공하는 **annotation**을 사용하여 개발한 후 해당 library를 scouter server의 library 경로에 넣어 놓기만 하면 자동으로 로딩되어 실행된다. 
(기본 설정으로는 **./lib** 디렉토리이며 동적로딩이 되지 않으므로 library 변경시 재시작이 필요하다.) 

#### 2.1 Server Built-in Plugin 개발 가이드
##### 1. dependecny
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

#### 3. Sample plugin
단순히 수집된 데이터를 출력하는 간단한 plugin 샘플을 제공한다. 
 * Sample plugin : [https://github.com/scouter-project/scouter-plugin-server-null](https://github.com/scouter-project/scouter-plugin-server-null)
 * Download : [scouter-plugin-server-null.jar](https://github.com/scouter-project/scouter-plugin-server-null/releases/download/v1.0/scouter-plugin-server-null.jar)


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
|**capture.plug**        | ```hook_method_patterns```나 ```hook_constructor_patterns```등을 통해 설정됨 method에 대해 해당 method의 시작 시점, 종료시점, Constructor 생성 시점에 호출됨 |
|**jdbcpoolplug**        | DB connection URL 요청 시점에 호출됨 |

이에 대한 상세 내용은 **[Scripting plugin java agent API 설명 페이지](JavaAgent-Plugin-Scripting.md)**를 참고한다.
