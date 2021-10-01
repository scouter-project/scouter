#!/usr/bin/env bash

##########################
# download maven from :
#   https://maven.apache.org

if [ ! -z "${JAVA_11_HOME}" ]; then
	echo JAVA_11_HOME: ${JAVA_11_HOME}
	JAVA_HOME=${JAVA_11_HOME}
fi

MVN="`which mvn`"
if [ ! -z "${MAVEN_HOME}" ]; then
	echo MAVEN_HOME: ${MAVEN_HOME}
	MVN="${MAVEN_HOME}/bin/mvn"
fi

if [ -z "$MVN" ]; then
    echo maven not found.
	exit 1
else
    $MVN -Dtycho.debug.resolver=true -X -f ./scouter.client.build/pom.xml clean package
fi
