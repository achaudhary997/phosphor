#!/bin/bash

JVMSPATH=/home/vagrant/jvms
PHOSPHORPATH=/home/vagrant/btpWork/phosphor/Phosphor/target/Phosphor-0.0.4-SNAPSHOT.jar

BENCHMARKS=(avrora batik fop h2 jython luindex pmd sunflow xalan)

JAVA_HOME1=/usr/lib/jvm/jdk1.7.0_80
JAVA_HOME2=/usr/lib/jvm/jdk1.8.0_221
JAVA_HOME3=/usr/lib/jvm/java-8-openjdk-amd64


for bm in "${BENCHMARKS[@]}"
do

echo "$JVMSPATH/jre1.7.0_55-inst/bin/java -Xbootclasspath/p:$PHOSPHORPATH -javaagent:$PHOSPHORPATH -cp target/dacapo-inst-obj/ -Declipse.java.home=$JAVA_ Harness $bm"

$JVMSPATH/jre1.7.0_55-inst/bin/java -Xmx1g -Xbootclasspath/p:$PHOSPHORPATH -javaagent:$PHOSPHORPATH -cp $PHOSPHORPATH/../dacapo-inst-obj/ -Declipse.java.home=$JAVA_HOME1 Harness $bm
# $JVMSPATH/jre1.8.0_05-inst/bin/java -Xmx1g -Xbootclasspath/p:$PHOSPHORPATH -javaagent:$PHOSPHORPATH -cp $PHOSPHORPATH/../dacapo-inst-obj/ -Declipse.java.home=$JAVA_HOME2 Harness $bm
# $JVMSPATH/icedtea8-inst/bin/java -Xmx1g -Xbootclasspath/p:$PHOSPHORPATH -javaagent:$PHOSPHORPATH -cp $PHOSPHORPATH/../dacapo-inst-obj/ -Declipse.java.home=$JAVA_HOME3 Harness $bm

if [ $? -ne 0 ]; then
HAD_ERROR=`expr $HAD_ERROR + 1`
echo "ERROR!!!"
fi

done
echo "Total errors: $HAD_ERROR"
if [ $HAD_ERROR -ne 0 ]; then
exit 255
fi

