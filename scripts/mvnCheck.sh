#!/bin/sh

cd ..
mvn spotbugs:check  -T100C -DskipTests
rc=$?
if [ $rc -ne 0 ] ; then
  echo Could not perform mvn spotbugs:check; exit $rc
fi
mvn checkstyle:check -Dcheckstyle.config.location=google_checks.xml -Dcheckstyle.violationSeverity=warning -Dcheckstyle.suppressions.location=checkstyle/checkstyle-suppressions.xml
rc=$?
if [ $rc -ne 0 ] ; then
  echo Could not perform mvn checkstyle:check; exit $rc
fi
mvn verify -T100C -DskipTests
rc=$?
if [ $rc -ne 0 ] ; then
  echo Could not perform mvn verify; exit $rc
fi
mvn clean package -Dmaven.test.skip=true -T100C -DskipTests
rc=$?
if [ $rc -ne 0 ] ; then
  echo Could not perform mvn clean package; exit $rc
fi
echo You can push!
