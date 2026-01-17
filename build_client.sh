#!/usr/bin/env bash

##########################
# download maven from :
#   https://maven.apache.org

if [ ! -z "${JAVA_21_HOME}" ]; then
	echo JAVA_21_HOME: ${JAVA_21_HOME}
	JAVA_HOME=${JAVA_21_HOME}
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
    # Increase XML entity size limit for Eclipse 2024-03 metadata
    export MAVEN_OPTS="-Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0"
    $MVN -Dtycho.debug.resolver=true -X -f ./scouter.client.build/pom.xml clean package
fi
