#!/usr/bin/env bash

. $(dirname $0)/env.sh

mkdir -p "$TUNAHOME/logs" > /dev/null 2>&1
cp nohup.out "$TUNAHOME/logs/nohup.$(date '+%Y%m%d%H%M%S').out" > /dev/null 2>&1
nohup java $JAVAOPTS -classpath "$TUNAHOME/scouter-server-boot.jar" scouter.boot.Boot "$TUNAHOME/lib" > nohup.out &

echo "Scouter server launching..."
echo "See the nohup.out."

