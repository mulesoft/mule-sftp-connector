#!/bin/bash
#export JAVA_HOME=/Users/cleottavedia/java/jdk-11.0.2.jdk/Contents/Home
#RUNTIME_VERSION=4.4.0-20230522
RUNTIME_VERSION=4.6.0-rc2
#RUNTIME_VERSION=4.5.0
#RUNTIME_VERSION=4.4.0-20220221
#MUNIT_JVM=/Users/cleottavedia/java/jdk-11.0.2.jdk/Contents/Home/bin/java
#RUNTIME_VERSION=4.3.0
MUNIT_JVM=/Users/cleottavedia/java/jdk-17.0.2.jdk/Contents/Home/bin/java

echo "JAVA_HOME=$JAVA_HOME"

mvn clean
mkdir target

mvn clean install \
    -DruntimeProduct=MULE_EE \
    -DruntimeVersion=$RUNTIME_VERSION \
    -Dmunit.jvm=$MUNIT_JVM -Dtest=none -DfailIfNoTests=false -DminVersion=4.6.0 -DruntimeVersion=4.6.0-rc2 -DruntimeProduct=MULE_EE
    #-Dmule.module.tweaking.validation.skip=true
    #-Dmule.jvm.version.extension.enforcement=LOOSE
    #> target/test.log

#cat target/test.log | grep "WARNING: Illegal reflective access by" > target/illegal-access.log

#cat target/illegal-access.log | \
#    sort | uniq | \
#    grep -Ev "org.mule.module.artifact|org.mule.metadata|org.mule.runtime|org.mule.service|net.sf.cglib"


#mvn clean install -DruntimeProduct=MULE_EE -DruntimeVersion=4.4.0-20230522 -Dmunit.test=file-listener-test-cases/new-file-being-constantly-updated-is-listened.xml
#mvn clean install -DruntimeProduct=MULE_EE -DruntimeVersion=4.5.0-rc11 -Dmunit.test=file-listener-test-cases/new-file-being-constantly-updated-is-listened.xml

#mvn clean install -DskipTests=true -DruntimeProduct=MULE_EE -DruntimeVersion=4.5.0-rc11

#mvn clean install -DruntimeVersion=4.4.0-20220221 -DadditionalRuntimeVersions=4.3.0-20220221 -DruntimeProduct=MULE_EE
#mvn clean install -DruntimeVersion=4.4.0-20220221 -DruntimeProduct=MULE_EE
#mvn clean install -DruntimeVersion=4.3.0-20220221 -DruntimeProduct=MULE_EE


#mvn clean install -DruntimeVersion=4.3.0  -Dtest=none -DfailIfNoTests=false  -Dmule.verbose.exceptions=false -Dmunit.failIfNoTests=false -Dmunit.test=attachment/attachments-test-case.xml#downloadAttachment -Dmunit.debug