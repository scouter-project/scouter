#!/usr/bin/env bash

READLINK="`dirname $0`/readlink.sh"
AGENT_HOME="`dirname $($READLINK $0)`"

rm -f $AGENT_HOME/*.scouter
