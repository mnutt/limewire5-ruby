@echo off

echo This script doesn't work.  Please fix it.
exit 

set CLASSPATH=.;../core;../lib/messagebundles/;../lib/jars/collections.jar;../lib/jars/xerces.jar;../lib/jars/jl011.jar;../lib/jars/themes.jar;../lib/jars/logicrypto.jar;../lib/jars/mp3sp14.jar;../lib/jars/commons-httpclient.jar;../lib/jars/commons-logging.jar;../lib/jars/i18n.jar;../lib/jars/icu4j.jar;../lib/jars/ProgressTabs.jar;../lib/jars/id3v2.jar;../lib/jars/log4j.jar;../lib/jars/jcraft.jar;../lib/jars/looks.jar;../lib/jars/daap.jar;../lib/jars/jmdns.jar;../lib/jars/tritonus.jar;../lib/jars/vorbis.jar

java -ss32k -oss32k -ms4m -Xminf0.10 -Xmaxf0.25 com.limegroup.gnutella.Main

