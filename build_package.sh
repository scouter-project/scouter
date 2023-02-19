#!/usr/bin/bash

##########################
# download ant from :
#   https://ant.apache.org
export JAVA_HOME=/home/kranian/program/jdk1.8.0_301
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
    $MVN -Dmaven.test.skip=true -f ./scouter.agent.java/pom.xml -Pjava-legacy clean package
    $MVN -Dmaven.test.skip=true -f ./scouter.deploy/pom.xml clean package
fi
