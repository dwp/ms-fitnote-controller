#!/bin/sh

./updateMetadata.sh
./fragments.sh
mvn -f ../pom.xml versions:update-properties -DgenerateBackupPoms=false
./mvnCheck.sh
