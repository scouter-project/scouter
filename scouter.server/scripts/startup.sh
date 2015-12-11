#!/usr/bin/env bash

nohup java -Xmx512m -classpath ./boot.jar scouter.boot.Boot ./lib > nohup.out &
