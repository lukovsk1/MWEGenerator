#!/bin/sh
java -jar ~/workspace/ddminj/target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar generator.GDDrecMWEGenerator ~/workspace/defects4j/bugs/csv_4_b/ src/main/java src/test/java org.apache.commons.csv.CSVParserTest#testNoHeaderMap "java.lang.NullPointerException"
