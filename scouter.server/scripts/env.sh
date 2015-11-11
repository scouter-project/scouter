#!/usr/bin/env bash

READLINK="`dirname $0`/readlink.sh"
TUNAHOME="`dirname $($READLINK $0/..)`"

JAVAOPTS="-Xmx512m"

