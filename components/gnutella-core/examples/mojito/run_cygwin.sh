#!/bin/sh

ulimit -n 1024

PATH_SEPARATOR=";"
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
exit_code=$?;

if [[ ${exit_code} == 127 ]]; then
	echo "Please make sure the {JDK_HOME}/bin directory is in your PATH";
	echo;
else
	java -ea org.limewire.mojito.Main $*;
fi;


exit 0
