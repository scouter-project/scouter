#!/usr/bin/env bash

. $(dirname $0)/env.sh

java -Xmx512m -classpath "$TUNAHOME/boot.jar" scouter.boot.Boot "$TUNAHOME/lib" -console

