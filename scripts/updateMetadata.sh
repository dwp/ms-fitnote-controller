#!/bin/sh

email=$(git config user.email)
username=$(echo "${email}" | cut -d '@' -f 1)
username=$(echo "${username}" | tr '[:upper:]' '[:lower:]')
date=$(date '+%Y-%m-%d')
sed -i '' "s/    name:.*/    name: \"\@$username\"/g" ../.dwp/project-metadata.yaml
sed -i '' "s/    date:.*/    date: \"$date\"/g" ../.dwp/project-metadata.yaml
