#!/usr/bin/env bash
#export JDK_JAVA_OPTIONS="--add-modules java.xml.bind"
nohup java -cp ./scouter.webapp.jar:./lib/*:.  scouterx.webapp.main.WebAppMain > nohup.out &
sleep 1
tail -100 nohup.out
