#!/usr/bin/env bash

READLINK="`dirname $0`/readlink.sh"
AGNTHOME="`dirname $($READLINK $0/..)`"

rm -f $AGNTHOME/*.scouter
