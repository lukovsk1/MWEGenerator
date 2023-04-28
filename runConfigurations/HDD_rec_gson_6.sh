#!/bin/sh
java -jar ~/workspace/ddminj/target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar generator.HDDrecMWEGenerator ~/workspace/defects4j/bugs/gson_6_b/ gson/src/main/java/ gson/src/test/java/ com.google.gson.regression.JsonAdapterNullSafeTest#testNullSafeBugSerialize java.lang.NullPointerException
