#!/usr/bin/env bash

##########################
# download ant from :
#   https://ant.apache.org

MVN="`which mvn`"
if [ ! -z "${MAVEN_HOME}" ]; then
	echo MAVEN_HOME: ${MAVEN_HOME}
	MVN="${MAVEN_HOME}/bin/mvn"
fi

if [ -z "$MVN" ]; then
    echo maven not found.
	exit 1
else
    $MVN -Dmaven.test.skip=true clean install
#    $MVN -Dmaven.test.skip=true -f ./scouter.agent.java/pom.xml -Pjava-legacy clean package
    $MVN -Dmaven.test.skip=true -f ./scouter.deploy/pom.xml clean package
fi
