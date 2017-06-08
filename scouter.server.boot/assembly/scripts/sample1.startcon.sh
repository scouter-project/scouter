#!/usr/bin/env bash

. $(dirname $0)/env.sh

java -Xmx512m -classpath "$TUNAHOME/scouter-server-boot.jar" scouter.boot.Boot "$TUNAHOME/lib" -console

