#!/bin/bash

ulimit -n 2048

PATH_SEPARATOR=":"
ROOT="../../.."

CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}."
CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${ROOT}/core"
CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${ROOT}/lib/messagebundles"

COMPONENTS="${ROOT}/components"
for COMPONENT in $(ls ${COMPONENTS}); do
	if [ -d "${COMPONENTS}/${COMPONENT}/build/classes" ]
	then
		CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${COMPONENTS}/${COMPONENT}/build/classes"
		CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${COMPONENTS}/${COMPONENT}/src/main/resources"
	fi
done

for JAR in $(find ${ROOT}/lib/jars -name *.jar); do 
   CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${JAR}"
done

for JAR in $(find ${COMPONENTS}/mojito-ui/misc/lib -name *.jar); do 
   CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${JAR}"
done

export CLASSPATH

javac org/limewire/mojito/Main.java
#java -ea -Djava.nio.preferSelect=true org.limewire.mojito.Main $*
java -ea org.limewire.mojito.Main $*

exit 0
