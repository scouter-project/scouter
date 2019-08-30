# Scouter Plugin Guide
[![English](https://img.shields.io/badge/language-English-orange.svg)](Plugin-Guide.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Plugin-Guide_kr.md)

This article explains plugin functions which enable scouter's extensibility. 

With the plugin function of Scouter collector sever, data collected by scouter can be pre-handled and can be shared to other softwares.  
With agent plugin, certain types of data can be modified and other business-meaningful data can be added data to XLog or profile. 

> Scouter plugins enable Scouter to collaborate with other open sources.

Scouter has 2 type of plugins - server plugin and an agent plugin.  
**server plug-in** is for to collector server  
and **agent Plugin** is for to Java agent.  
Server plugin has 2 type plugins including **script-type plugin**, **built-in-type plugin**.

## List of available plugins
Below are the list of official plugins from scouter project and from contributors.

#### 1. server plugins (built-in type)
- **Sample**
  - **[scouter-plugin-server-null](https://github.com/scouter-project/scouter-plugin-server-null)** : sample plugin prints out data collected

- **Alert**
  - **[scouter-plugin-server-email](https://github.com/scouter-contrib/scouter-plugin-server-alert-email)** : emails alters from Scouter
  - **[scouter-plugin-server-telegram](https://github.com/scouter-contrib/scouter-plugin-server-alert-telegram)** : transfer altert from Scouter to telegram
  - **[scouter-plugin-server-slack](https://github.com/scouter-contrib/scouter-plugin-server-alert-slack)** : transfer altert from Scouter to slack
  - **[scouter-plugin-server-line](https://github.com/scouter-contrib/scouter-plugin-server-alert-line)** : transfer altert from Scouter to line
  - **[scouter-plugin-server-dingtalk](https://github.com/scouter-contrib/scouter-plugin-server-alert-dingtalk)** : transfer altert from Scouter to dingtalk
  
- **Counter**
  - **[scouter-plugin-server-influxdb](https://github.com/scouter-contrib/scouter-plugin-server-influxdb)** : transfer performance data from Scouter to influxDB(time series DB)

#### 2. agent plugins
* TBD

## Server Plugin - How to

### 1. Script (type) Plugin
A scripting plugin is called right before xlog data is stored in a repository.
You can add simple script for manipulate some data on the plugin files which are located in the directory **[server_running_dir]/plugin** by default.
 
Scouter distribution has the samples and the file name can not be modified.  
Currently 6 types of scripting plugins are supported.
* **alert.plug** - for pre-handling alert
* **counter.plug** - for pre-handling performance metrics(counters)
* **object.plug** - for pre-handling monitoring objects(instances)
* **summary.plug** - for pre-handling performance summary data
* **xlog.plug** - for pre-handling xlog data
* **xlogprofile.plug** - for pre-handling xlog profile

refer to the link for details. 
refer to **[Scripting plugin Server API](Server-Plugin-Scripting.md)**.

### 2. Built-in (type) Plugin
Building scripting plugin is very simple and can be dynamically loaded on runtime environment.  
On the other hand if you need the function permanently, it's too easy to fragile.
So scouter provides another plugin type which allow you can attach pre-built compiled plugin and it's called as **Built-in Plugin**.

Scouter load the plugins on startup, if the plugins are located in scouter server's library directory.(default:**[server_running_dir]/lib**)

#### 2.1 Server Built-in Plugin development guide
##### 1. dependency
 * scouter.common
    ```xml
    <dependency>
        <groupId>io.github.scouter-project</groupId>
        <artifactId>scouter-common</artifactId>
        <version>1.7.0</version>
    </dependency>
    ```
 * scouter.server
    ```xml
    <dependency>
        <groupId>io.github.scouter-project</groupId>
        <artifactId>scouter-server</artifactId>
        <version>1.7.0</version>
    </dependency>
    ```
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

* Check it.
> 1. The file is located sub package of **scouter.plugin.server** because annotation scan scope is from scouter.plugin.server package.    
> 2. The plugin can use scouter server's configuration and the option name must start with **ext_plugin_xxx**.  

* ```ServerPlugin``` annotations

    * **```PLUGIN_SERVER_COUNTER```**
    * **```PLUGIN_SERVER_ALERT```**
    * **```PLUGIN_SERVER_OBJECT```**
    * **```PLUGIN_SERVER_SUMMARY```**
    * **```PLUGIN_SERVER_XLOG```**
    * **```PLUGIN_SERVER_PROFILE```**

#### 2.3. plugin sample
Provided sample plugin that just prints the data collected. 
 * Sample plugin : [https://github.com/scouter-project/scouter-plugin-server-null](https://github.com/scouter-project/scouter-plugin-server-null)
 * Download : [scouter-plugin-server-null.jar](https://github.com/scouter-project/scouter-plugin-server-null/releases/download/v1.0/scouter-plugin-server-null.jar)

### 3. Alert scripting (type) plugin
We can build our own alarm rules by handling alert scripting plugins which are able to compose various performance metrics.
  * [Alert Plugin Guide](./Alert-Plugin-Guide.md)
<br>


## Agent Plugin - Scripting Plugin

#### Java agent plugin
**Scripting plugin can be loaded dynamically on runtime so it is used for debugging also**    
It's very useful to print some method's parameters or stacktrace on specific point and also can add additional (user-defined) profile information to the xlog or xlog profile.     
The scripting plugin is invoked at some important points and the default location of the plugin file is **./plugin**.  
Scouter distribution includes sample plugin files and the file name can not be modified.  
 
|filename               |    desc                  |
|-------------------|-------------------------|
|**httpservice.plug**    | Invoked at begin and end of http service. |
|**service.plug**        | Invoked at begin and end of user defined service. (The services are set by ```hook_service_patterns``` option) |
|**httpcall.plug**       | Invoked at calling to another service using httpclients library |
|**capture.plug**        | Invoked at init, start, end of methods that are set by options ```hook_args_patterns``` or ```hook_constructor_patterns```  |
|**jdbcpoolplug**        | Invoked at calling DB connection URL |

Refer to the link for details **[Scripting plugin java agent API](JavaAgent-Plugin-Scripting.md)**
