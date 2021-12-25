# Scouter Weaver Guide
[![English](https://img.shields.io/badge/language-English-orange.svg)](Weaver-Guide.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Weaver-Guide_kr.md)

You can directly control the scouter's xlog and profile through Scouter Weaver.
* Start tracing and End tracing of XLog trace.
* Add Method Profile.
* Add message profile.
* Link to scouter trace context with custom transaction id.
* Set values for XLog fields.

## Installation
* Maven
```xml
<dependency>
     <groupId>io.github.scouter-project</groupId>
     <artifactId>scouter-weaver</artifactId>
     <version>{scouter_version}</version> <!--2.17.1-->
</dependency>
```

* Gradle
```groovy
implementation 'io.github.scouter-project:scouter-weaver:{scouter_version}' //2.17.1
```

## How to use
Refer to Scouter Weaver class description.
 - [Weaver class](https://github.com/scouter-project/scouter/blob/master/scouter.weaver/src/main/java/scouterx/weaver/Scouter.java)

### start & end trace

```java
TransferCtx tctx = Scouter.startServiceAndGetCtxTransfer("{service_api_name}");
doSomething();
Scouter.endServiceByCtxTransfer(tctx, null); //commonly inside finally staterment
//or
// Scouter.endServiceOnTheSameThread(null);
```
do with custom transaction id
```java
TransferCtx tctx = Scouter.startServiceWithCustomTxidAndGetCtxTransfer("{service_api_name}", "{customTxid}");
doSomething();
Scouter.endServiceByCustomTxid("{customTxid}", null);
```

### add method profile
```java
void doSomething(TransferCtx tctx, ...) {
    MethodCtx mctx = MethodCtxScouter.startMethodOnTheSameThread("doSomething");
	//or Scouter.startMethodByCtxTransfer(), Scouter.startMethodByCustomTxid();
    try {
        doOthers();	
    } finally {
        Scouter.endMethodByMethodTransfer(mctx, null);	
    }
}
```

### add profiles
```java
Scouter.addMessageProfileOnTheSameThread("{simple massage}");
Scouter.addHashedMessageProfileOnTheSameThread("{massage for dictionary encoding in scouter}", 0, 0); //메시지 전체가 사전에 인덱싱 되므로 메시지 종류가 수천개 미만인 경우 사용해야 합니다. (그렇지 않으면 사전 성능이 저하됩니다.)
Scouter.addParameterizedMessageProfileOnTheSameThread("{massage with param %s, param2: %s}", ProfileLevel.INFO, 0, param1, param2); 

```

### get trace info and support methods
```java
boolean scouterActivated = Scouter.isScouterJavaAgentActivated(); //check if scouter agent is activated.

TransferCtx tctx = Scouter.getTransferCtxOnTheSameThread(); //get aleady started scouter trace transfer context. 

Scouter.linkCustomTxidOnTheSameThread("{my custom txid}"); //link my custom trace id onto the scouter trace context.
Scouter.linkCustomTxidByCtxTransfer("{my custom txid}", transferCtx) {}

ScouterTxid stxid = getTxidOnTheSameThread();
ScouterTxid stxid = getTxidByCustomTxid("{my custom txid}");


```


### add xlog column values
```java
Scouter.setXlogServiceDictionaryValue(scouterTxid, "{service api name}");
/*
    see other methods...
    setXlogUaDictionaryValue()
    setXlogErrorDictionaryValue()
    setXlogIpValue()
    setXlogLoginDictionaryValue()
    setXlogDescDictionaryValue()
    setXlogText1Value()
    setXlogText2Value()
    setXlogText3Value()
 */
```
