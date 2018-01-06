# Build Scouter on Windows
[![English](https://img.shields.io/badge/language-English-orange.svg)](Build-Scouter-Windows.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Build-Scouter-Windows_kr.md)

## Download Scouter
  https://github.com/scouter-project/scouter/archive/master.zip

  Extract the zip file to any directory(ex /app/scouter-master)

## Build Agent & Server
### 1. Setup JDK

### 2.Package Build
mvn clean package
 - output : ./scouter-deploy/target/scouter-{version}.tar.gz

## Client Compile & Execute

### 1. Run Eclipse

### 2. Setup GEF plugins

1. select 
   Help/Install New software  
2. set update site 
   http://download.eclipse.org/tools/gef/updates-pre-3_8/releases/
3. install GEF 3.7.2

### 3. Import project
1. open JavaEE Perspective
2. import java project 
    import all project from c:\scouter\scouter-master

### 4. Execute Client
1. open product
   scouter.client.product/scouter.client.product

2. execute
   click "Launch an Eclipse application"
