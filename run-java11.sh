#!/bin/bash
RUNTIME_VERSION=4.6.0-rc3
MUNIT_JVM=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home/bin/java
mvn clean
mkdir target
mvn verify \
    -Dtest=none \
    -DfailIfNoTests=false \
    -DruntimeProduct=MULE_EE \
    -DruntimeVersion=$RUNTIME_VERSION \
    -Dmunit.jvm=$MUNIT_JVM > ./target/test.log