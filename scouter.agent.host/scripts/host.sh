#!/usr/bin/env bash

READLINK="`dirname $0`/readlink.sh"
AGNTHOME="`dirname $($READLINK $0/..)`"

mkdir $AGNTHOME/logs > /dev/null 2>&1
cp $AGNTHOME/nohup.out $AGNTHOME/logs/nohup.$(date '+%Y%m%d%H%M%S').out > /dev/null 2>&1

nohup java -classpath $AGNTHOME/scouter.host.jar scouter.boot.Boot $AGNTHOME/lib > $AGNTHOME/nohup.out &

echo "Scouter host agent launching..."
echo "See the nohup.out."
