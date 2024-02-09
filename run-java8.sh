#!/bin/bash
RUNTIME_VERSION=4.4.0
MUNIT_JVM=/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home/bin/java
mvn clean
mkdir target
mvn verify \
    -Dtest=none \
    -DfailIfNoTests=false \
    -DruntimeProduct=MULE_EE \
    -DruntimeVersion=$RUNTIME_VERSION \
    -Dmunit.jvm=$MUNIT_JVM > ./test8.log