#!/bin/sh

email=$(git config user.email)
username=$(echo "${email}" | cut -d '@' -f 1)
username=$(echo "${username}" | tr '[:upper:]' '[:lower:]')
date=$(date '+%Y-%m-%d')
sed -i '' "s/verified.by:.*/verified.by: $username/g" ../catalog-info.yaml
sed -i '' "s/verified.on:.*/verified.on: $date/g" ../catalog-info.yaml
