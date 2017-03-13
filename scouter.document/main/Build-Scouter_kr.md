# Build Scouter
[![Englsh](https://img.shields.io/badge/language-English-orange.svg)](Build-Scouter.md) ![Korean](https://img.shields.io/badge/language-Korean-blue.svg)

### Download Scouter
  https://github.com/scouter-project/scouter/archive/master.zip

  Extract the zip file to any directory(ex /app/scouter-master)

### Setup JDK1.7
   http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase7-521261.html

### Setup  Ant
1. download ant package

   http://ant.apache.org

2. extract the file to any directory

3. set env

   export ANT_HOME=/app/scouter-master/apache-ant-1.9.5

### Setup Maven
1. download maven package

   https://maven.apache.org
    
2. extract the file to any directory

3. set env

   export MAVEN_HOME=/app/scouter-master/apache-maven-3.3.3 

### Make executable
cd /app/scouter-master/.

chmod +x build_package.sh

chmod +x build_client.sh


### Build
./build_package.sh

./build_client.sh