#!/usr/bin/env bash

##########################
# download ant from :
#   https://ant.apache.org

ANT="`which ant`"

if [ ! -z "${ANT_HOME}" ]; then
    ANT="${ANT_HOME}/bin/ant"
fi

if [ -z "$ANT" ]; then
	echo ant not found.
	exit 1
else
    $ANT -buildfile ./scouter.deploy/build.xml
fi
