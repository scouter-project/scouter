#!/bin/bash

#JAVA_HOME=/usr/java/default
#JAVA=$JAVA_HOME/bin/java
JAVA=java

JAVA_OPTS=""
JAVA_OPTS="$JAVA_OPTS -server"
JAVA_OPTS="$JAVA_OPTS -Xms1g -Xmx1g"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC"
JAVA_OPTS="$JAVA_OPTS -XX:+UseTLAB -XX:+ResizeTLAB"

# GC LOG
JAVA_OPTS="$JAVA_OPTS \
    -verbose:gc \
    -Xloggc:logs/gc.$(date '+%Y%m%d_%H%M%S').log \
    -XX:+PrintGCDetails \
    -XX:+PrintGCTimeStamps \
    -XX:+PrintGCDateStamps \
    -XX:+PrintHeapAtGC \
    -XX:+PrintPromotionFailure \
    -XX:+PrintClassHistogram \
    -XX:+PrintTenuringDistribution \
    -XX:+PrintGCApplicationStoppedTime \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=10 \
    -XX:GCLogFileSize=10M"

JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs"
JAVA_OPTS="$JAVA_OPTS -XX:ErrorFile=logs/err.log"

JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"
JAVA_OPTS="$JAVA_OPTS -Dsun.net.inetaddr.ttl=0"
JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"

nohup java $JAVA_OPTS -classpath ./scouter-server-boot.jar scouter.boot.Boot ./lib > nohup.out &
sleep 1
tail -100 nohup.out

