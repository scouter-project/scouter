#!/usr/bin/env bash

nohup java -Xmx512m -classpath ./scouter-server-boot.jar scouter.boot.Boot ./lib > nohup.out &
sleep 1
tail -100 nohup.out

