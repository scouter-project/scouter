#!/usr/bin/env bash

nohup java -cp ./scouter-agent-batch-1.7.0.jar -Dscouter.config=./conf/scouter.batch.conf scouter.agent.batch.Main > nohup.out &
sleep 1
tail -100 nohup.out
