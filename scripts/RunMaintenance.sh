#!/bin/sh

./updateCatalogInfo.sh
./fragments.sh
mvn -f ../pom.xml versions:update-properties -DgenerateBackupPoms=false
./mvnCheck.sh
