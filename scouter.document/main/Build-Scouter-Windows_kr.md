# Build Scouter on Windows
[![Englsh](https://img.shields.io/badge/language-English-orange.svg)](Build-Scouter-Windows_kr.md) ![Korean](https://img.shields.io/badge/language-Korean-blue.svg)

## Download Scouter
  https://github.com/scouter-project/scouter/archive/master.zip

  Extract the zip file to any directory(ex /app/scouter-master)

## Build Agent & Server
### 1. Setup JDK1.7
   http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase7-521261.html

### 2.Package Build
mvn clean package
 - output : ./scouter-deploy/target/scouter-{version}.tar.gz

## Client Compile & Execute

### 1. Eclipse Indigo setup
https://eclipse.org/downloads/packages/release/indigo/sr2

### 2.Extract to
C:\scouter\eclipse

### 3. Start eclipse 
  workspace : C:\scouter\workspace

### 4. Setup GEF plugins

1. select 
   Help/Install New software  
2. set update site 
   http://download.eclipse.org/tools/gef/updates-pre-3_8/releases/
3. install GEF 3.7.2

### 5. Make sure JDK 7 in eclipse
1. open  Window/Preferences 
2. check Java/Installed JRES
   c:\scouter\java7    
   
### 6. Import project
1. open JavaEE Perspective
2. import java project 
    import all project from c:\scouter\scouter-master

### 7. Execute Client
1. open product
  
   scouter.client.product/scouter.client.product

2. execute
   click "Launch an Eclipse application"

if error below:
```
  Error occurred during initialization of VM
  Could not reserve enough space for object heap
```
then change Launch options
```
-Xms256m
-Xmx512m
-XX:+UseParallelGC
-XX:PermSize=64M
-XX:MaxPermSize=256M       
``` 