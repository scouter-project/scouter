#!/usr/bin/env bash

nohup java  -classpath ./scouter.host.jar scouter.boot.Boot ./lib > nohup.out &
sleep 1
tail -100 nohup.out
