#!/bin/bash

JVMSPATH=/home/vagrant/jvms
PHOSPHORPATH=/home/vagrant/btpWork/phosphor/Phosphor

unset JAVA_HOME
rm -rf jvms/*inst*
java -jar $PHOSPHORPATH/target/Phosphor-0.0.4-SNAPSHOT.jar $JVMSPATH/jre1.7.0_55/ $JVMSPATH/jre1.7.0_55-inst
java -jar $PHOSPHORPATH/target/Phosphor-0.0.4-SNAPSHOT.jar $JVMSPATH/jre1.8.0_05/ $JVMSPATH/jre1.8.0_05-inst
java -jar $PHOSPHORPATH/target/Phosphor-0.0.4-SNAPSHOT.jar $JVMSPATH/icedtea7/ $JVMSPATH/icedtea7-inst
java -jar $PHOSPHORPATH/target/Phosphor-0.0.4-SNAPSHOT.jar $JVMSPATH/icedtea8/ $JVMSPATH/icedtea8-inst


chmod +x $JVMSPATH/jre1.7.0_55-inst/bin/*
chmod +x $JVMSPATH/jre1.8.0_05-inst/bin/*
chmod +x $JVMSPATH/icedtea7-inst/bin/*
chmod +x $JVMSPATH/icedtea8-inst/bin/*


cp $JVMSPATH/icedtea7-inst/lib/jce.jar $JVMSPATH/jre1.7.0_55-inst/lib/jce.jar
cp $JVMSPATH/icedtea8-inst/lib/jce.jar $JVMSPATH/jre1.8.0_05-inst/lib/jce.jar
