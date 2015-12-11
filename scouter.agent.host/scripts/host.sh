#!/usr/bin/env bash

nohup java  -classpath ./scouter.host.jar scouter.boot.Boot ./lib > nohup.out &
