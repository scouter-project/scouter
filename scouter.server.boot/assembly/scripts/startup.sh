#!/usr/bin/env bash

# Work at JDK9 Above Only
export JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=ALL-UNNAMED"

nohup java -Xmx1024m -classpath ./scouter-server-boot.jar scouter.boot.Boot ./lib > nohup.out &
sleep 1
tail -100 nohup.out