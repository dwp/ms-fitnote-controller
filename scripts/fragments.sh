#!/bin/bash

FRAGMENT_INCLUDE_PATH=../gitlab-ci/config/fragments.yml
major=0
minor=0
patch=0
FRAGMENT_NAMES=$(yq '.include.[] | select(.project) | .project' $FRAGMENT_INCLUDE_PATH)
featureRegexCheck="^([fF][-].+)$"
for i in $FRAGMENT_NAMES
do
  #reset feature branch flag
  isFeatureBranchToggle=0
  export current_project="$i"
  CURRENT_REF_ORG=$(yq '.include.[] | select(.project == env(current_project)) | .ref' $FRAGMENT_INCLUDE_PATH)
  LATEST_REF_ORG=$(git ls-remote --tags --refs --sort="v:refname" ssh://git@gitlab.com/"$i".git | tail -n1 | cut -f2 -d$'\t' | cut -f3 -d/)


  if [[ "$CURRENT_REF_ORG" =~ $featureRegexCheck ]] && [[ "$CI_COMMIT_REF_NAME" =~ $featureRegexCheck ]]; then
    isFeatureBranchToggle=1
  elif [[ "$CURRENT_REF_ORG" =~ ^([0-9]+[.][0-9]+[.][0-9]+)$ ]]; then
    #Remove dots from current ref
    CURRENT_REF=${CURRENT_REF_ORG//./ }
  elif [[ "$CURRENT_REF_ORG" =~ ^([0-9]+[-][0-9]+[-][0-9]+)$ ]]; then
    #Remove hyphens from current ref
    CURRENT_REF=${CURRENT_REF_ORG//-/ }
  else
    exit 10
  fi

  if [[ "$LATEST_REF_ORG" =~ ^([0-9]+[.][0-9]+[.][0-9]+)$ ]]; then
    #Remove dots from latest ref
    LATEST_REF=${LATEST_REF_ORG//./ }
  elif [[ "$LATEST_REF_ORG" =~ ^([0-9]+[-][0-9]+[-][0-9]+)$ ]]; then
    #Remove hyphens from latest ref
    LATEST_REF=${LATEST_REF_ORG//-/ }
  elif [[ -z "$LATEST_REF_ORG" ]]; then
     continue
  else
    exit 10
  fi

  # remove any alpha suffixes for fragments under development
  CURRENT_REF=${CURRENT_REF%-alpha}
  LATEST_REF=${LATEST_REF%-alpha}

  PATCH_CURRENT_REF=$(echo "$CURRENT_REF" | awk '{print $3}')
  MINOR_CURRENT_REF=$(echo "$CURRENT_REF" | awk '{print $2}')
  MAJOR_CURRENT_REF=$(echo "$CURRENT_REF" | awk '{print $1}')
  PATCH_LATEST_REF=$(echo "$LATEST_REF" | awk '{print $3}')
  MINOR_LATEST_REF=$(echo "$LATEST_REF" | awk '{print $2}')
  MAJOR_LATEST_REF=$(echo "$LATEST_REF" | awk '{print $1}')

  if [[ isFeatureBranchToggle -eq 1 ]]; then
    continue
  elif [[ $MAJOR_CURRENT_REF -lt $MAJOR_LATEST_REF ]] || [[ $MINOR_CURRENT_REF -lt $MINOR_LATEST_REF ]]  || [[ $PATCH_CURRENT_REF -lt $PATCH_LATEST_REF ]]; then
    if [[ $MAJOR_CURRENT_REF -lt $MAJOR_LATEST_REF ]]; then
      ((++major))
    elif [[ $MINOR_CURRENT_REF -lt $MINOR_LATEST_REF ]]; then
      ((++minor))
    elif [[ $PATCH_CURRENT_REF -lt $PATCH_LATEST_REF ]]; then
      ((++patch))
    fi
  fi
  if [[ $MAJOR_CURRENT_REF -lt $MAJOR_LATEST_REF ]] || [[ $MINOR_CURRENT_REF -lt $MINOR_LATEST_REF ]]  || [[ $PATCH_CURRENT_REF -lt $PATCH_LATEST_REF ]]; then
    echo "fragment updated: ${i##*/}"
    frag_name=$(echo "${i}" | sed -r 's/\//\\\//g')
    perl -i~ -0pe "s/$frag_name'\n    ref: .*/$frag_name'\n    ref: $LATEST_REF_ORG/" ../gitlab-ci/config/fragments.yml
  fi

done



echo "------------------------------"
echo "Total new version(s) updated"
echo "Major:" $major
echo "Minor:" $minor
echo "Patch:" $patch
echo "------------------------------"
