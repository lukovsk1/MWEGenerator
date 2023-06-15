#!/bin/bash

ALGORITHMS=()
BUGS=()
MULTIPLE_RUNS=false
TIMEOUT_HOURS=4

echo "Select algorithms:"
select algorithm in HDD GDD HDDrec GDDrec CodeLine; do
  if [ -z "$algorithm" ]; then
    break
  fi
  ALGORITHMS+=($algorithm)
  echo "You have chosen ${ALGORITHMS[@]}. Add another algorithm or write 'ok' to continue."
done

echo "Select bugs:"
select bug in csv_4 gson_6 cli_1 lang_5 jsoup_9 chart_2; do
  if [ -z "$bug" ]; then
    break
  fi
  BUGS+=($bug)
  echo "You have chosen ${BUGS[@]}. Add another bug or write 'ok' to continue."
done

read -n 1 -p "Execute multiple runs (y/n)? " answer
if [ "$answer" == "y" ] || [ "$answer" == "Y" ]; then
  MULTIPLE_RUNS=true
fi
echo

read -p "Set timeout in hours (default 4h): " answer
re='^[0-9]+$'
if [[ "$answer" =~ $re ]] ; then
   TIMEOUT_HOURS=$answer
fi

for bug in "${BUGS[@]}"; do
  for algorithm in "${ALGORITHMS[@]}"; do

    git pull
    echo "building jar file with dependencies"
    mvn clean compile assembly:single

    echo "Running algorithm $algorithm for bug $bug"
    if [[ $bug == csv_4 ]]; then
      java -jar ~/workspace/ddminj/target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar "generator.${algorithm}MWEGenerator" ~/workspace/defects4j/bugs/csv_4_b/ src/main/java src/test/java org.apache.commons.csv.CSVParserTest#testNoHeaderMap "java.lang.NullPointerException" "$MULTIPLE_RUNS" "$TIMEOUT_HOURS"
    elif [[ $bug == gson_6 ]]; then
      java -jar ~/workspace/ddminj/target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar "generator.${algorithm}MWEGenerator" ~/workspace/defects4j/bugs/gson_6_b/ gson/src/main/java/ gson/src/test/java/ com.google.gson.regression.JsonAdapterNullSafeTest#testNullSafeBugSerialize java.lang.NullPointerException "$MULTIPLE_RUNS" "$TIMEOUT_HOURS"
    elif [[ $bug == cli_1 ]]; then
      java -jar ~/workspace/ddminj/target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar "generator.${algorithm}MWEGenerator" ~/workspace/defects4j/bugs/cli_1_b/ src/java/ src/test/ org.apache.commons.cli.bug.BugCLI13Test#testCLI13 junit.framework.AssertionFailedError "$MULTIPLE_RUNS" "$TIMEOUT_HOURS"
    elif [[ $bug == lang_5 ]]; then
      java -jar ~/workspace/ddminj/target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar "generator.${algorithm}MWEGenerator" ~/workspace/defects4j/bugs/lang_5_b/ src/main/java/ src/test/java/ org.apache.commons.lang3.LocaleUtilsTest#testLang865 "java.lang.IllegalArgumentException: Invalid locale format: _GB" "$MULTIPLE_RUNS" "$TIMEOUT_HOURS"
    elif [[ $bug == jsoup_9 ]]; then
      java -jar ~/workspace/ddminj/target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar "generator.${algorithm}MWEGenerator" ~/workspace/defects4j/bugs/jsoup_9_b/ src/main/java/ src/test/java/ org.jsoup.nodes.EntitiesTest#unescape org.junit.ComparisonFailure "$MULTIPLE_RUNS" "$TIMEOUT_HOURS"
    elif [[ $bug == chart_2 ]]; then
      java -jar ~/workspace/ddminj/target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar "generator.${algorithm}MWEGenerator" ~/workspace/defects4j/bugs/chart_2_b/ source/ tests/ org.jfree.data.general.junit.DatasetUtilitiesTests#testBug2849731_2 java.lang.NullPointerException "$MULTIPLE_RUNS" "$TIMEOUT_HOURS"
    fi

    echo "checking in logs and stats files to git"
    git add ./stats
    git add ./logs
    git commit -m "VM run $algorithm for $bug"
    git push
  done
done


