#!/usr/bin/env bash

READLINK="`dirname $0`/readlink.sh"
AGENT_HOME="`dirname $($READLINK $0)`"
LOGS_DIR="$AGENT_HOME/logs"
LOG_FILE="$LOGS_DIR/host.agent.$(date '+%Y%m%d%H%M%S').log"
JAVA="java"
CLASSPATH="$AGENT_HOME/scouter.host.jar"
MAINCLASS="scouter.boot.Boot"

mkdir -p $LOGS_DIR > /dev/null 2>&1
nohup $JAVA -classpath $CLASSPATH $MAINCLASS $AGENT_HOME/lib > $LOG_FILE 2>&1 &

echo "Scouter host agent launching..."
echo "See logs : $LOG_FILE"
