#!/usr/bin/env bash

nohup java  -classpath ./scouter.webapp.jar scouterx.webapp.main.WebAppMain > nohup.out &
sleep 1
tail -100 nohup.out
