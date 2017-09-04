#!/usr/bin/env bash

nohup java  -cp ./scouter.webapp.jar:./lib/*:.  scouterx.webapp.main.WebAppMain > nohup.out &
sleep 1
tail -100 nohup.out
