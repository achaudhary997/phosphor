#!/bin/bash

ASM_PATH=./asm-all-5.0.3.jar
PHOSPHOR_JAR_PATH=/home/vagrant/btpWork/phosphor/project/Phosphor/target/Phosphor-0.0.4-SNAPSHOT.jar
INST_JAVA_PATH=/home/vagrant/btpWork/phosphor/project/Phosphor/target/jre-inst-obj

if [ "$#" -ne 1 ]
then
	echo "Usage: ./run.sh <FOLDER_NAME>"
	exit
fi

FOLDER=$1
MAIN_FILE=""

if [ "$FOLDER" = "basic" ]; then
	MAIN_FILE="Basic"
elif [ "$FOLDER" = "rankCollege" ]; then
	MAIN_FILE="RankCollege"
elif [ "$FOLDER" = "soot" ]; then
	MAIN_FILE="soot.Main -h"
else
	echo "Invalid Folder!"
	exit
fi

cd $FOLDER
ls
rm inst/*

if java -Xmx1g -jar $PHOSPHOR_JAR_PATH uninst inst
then
	cd inst
	$INST_JAVA_PATH/bin/java -Xbootclasspath/a:$PHOSPHOR_JAR_PATH -javaagent:$PHOSPHOR_JAR_PATH -Xmx2g -XX:MaxPermSize=1g -cp . $MAIN_FILE
fi