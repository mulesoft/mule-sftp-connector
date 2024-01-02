#!/bin/bash
RUNTIME_VERSION=4.6.0-SNAPSHOT
MUNIT_JVM=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java
mvn clean
mkdir target
mvn verify \
    -DruntimeProduct=MULE_EE \
    -DruntimeVersion=$RUNTIME_VERSION \
    -Dmunit.jvm=$MUNIT_JVM \
    -Dtest=none -DfailIfNoTests=false \
    -Dmunit.test=file-listener-test-cases/new-file-is-listened-and-later-filtered.xml#newFileIsListenedAndLaterFiltered
