#!/bin/bash
rm toTestInst/*
java -Xmx1g -jar target/Phosphor-0.0.4-SNAPSHOT.jar RankCollege toTestInst
cd toTestInst
# $JAVA_HOME/bin/java -Xbootclasspath/a:../target/Phosphor-0.0.4-SNAPSHOT.jar -javaagent:../target/Phosphor-0.0.4-SNAPSHOT.jar -Xmx2g -XX:MaxPermSize=1g -cp . RankCollege
../target/jre-inst-obj/bin/java -Xbootclasspath/a:../target/Phosphor-0.0.4-SNAPSHOT.jar -javaagent:../target/Phosphor-0.0.4-SNAPSHOT.jar -Xmx2g -XX:MaxPermSize=1g -cp . RankCollege
cd ..
