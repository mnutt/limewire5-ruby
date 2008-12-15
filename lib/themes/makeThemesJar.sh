#!/bin/sh

ls -A1 | while read X
do
    if [ "$X" = "CVS" ]; then
        echo "ignoring cvs dir."
    elif [ -d "$X" ]; then
        lwtp=${X}_theme.lwtp
        if [ "$X" = "brushed_metal" ] || [ "$X" = "pinstripes" ] ; then
            export lwtp=${X}_theme_osx.lwtp
        fi
        
        rm -f $lwtp
        zip -0 -j $lwtp $X/*
    fi
done

rm -f themes.jar
jar -0Mcf themes.jar *.lwtp
rm -f *.lwtp
mv themes.jar ../jars/other/themes.jar

