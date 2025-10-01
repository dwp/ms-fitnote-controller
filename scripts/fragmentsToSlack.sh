#!/bin/bash

# for running/testing locally
#FRAGMENT_INCLUDE_PATH=../gitlab-ci/config/fragments.yml


major=0
minor=0
patch=0

FRAGMENT_NAMES=$(yq '.include.[] | select(.project) | .project' "$FRAGMENT_INCLUDE_PATH")
featureRegexCheck="^([fF][-].+)$"
FRAGMENT_LINE="*Fragments:*"
FRAGMENT_URL="https://gitlab.com/"
for i in $FRAGMENT_NAMES
do
  #reset feature branch flag
  isFeatureBranchToggle=0
  export current_project="$i"
  CURRENT_REF_ORG=$(yq '.include.[] | select(.project == env(current_project)) | .ref' "$FRAGMENT_INCLUDE_PATH")
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
    FRAGMENT_LINE+="\n • <$FRAGMENT_URL$i|${i##*/}> \n Current Ref: $CURRENT_REF_ORG \n Latest Ref: $LATEST_REF_ORG"
  fi
done

# for local testing
#CI_PIPELINE_URL=https://gitlab.com/dwp/health/fitnote/components/ms-datamatrix-creator/-/pipelines/1927999487
REPO_PATH=${CI_PIPELINE_URL%%/-*}


FIRST_HEADING="*${REPO_PATH##*/} - Fragments*"
SECOND_HEADING="<${REPO_PATH}|${REPO_PATH##*/}> \n :building_construction: \n Total new version(s) available \n • Major: $major\n• Minor: $minor\n• Patch: $patch"
LINK_HEADING="<${REPO_PATH}|${REPO_PATH##*/}>  :package:"
LINK_HEADING_2="<${CI_PIPELINE_URL}|Pipeline>  :electric_plug:"

json_string=$(printf '{
    "blocks": [
    		{
    			"type": "section",
    			"text": {
    				"type": "mrkdwn",
    				"text": "%s"
    			}
    		},
    		{
    			"type": "section",
    			"block_id": "section567",
    			"text": {
    				"type": "mrkdwn",
    				"text": "%s"
    			},
    			"accessory": {
    				"type": "image",
    				"image_url": "https://upload.wikimedia.org/wikipedia/commons/3/3c/Datamatrix.png",
    				"alt_text": "Data matrix"
    			}
    		},
    		{
    			"type": "section",
    			"block_id": "section789",
    			"fields": [
    				{
    					"type": "mrkdwn",
    					"text": "%s"
    				}
    			]
    		}
    	],
    	"attachments": [
    		{
    			"blocks": [
    				{
    					"type": "section",
    					"text": {
    						"type": "mrkdwn",
    						"text": "*Links*"
    					}
    				},
    				{
    					"type": "section",
    					"text": {
    						"type": "mrkdwn",
    						"text": "%s"
    					},
    					"accessory": {
    						"type": "button",
    						"text": {
    							"type": "plain_text",
    							"text": "View",
    							"emoji": true
    						},
    						"value": "view_alternate_1"
    					}
    				},
    				{
    					"type": "section",
    					"text": {
    						"type": "mrkdwn",
    						"text": "%s"
    					},
    					"accessory": {
    						"type": "button",
    						"text": {
    							"type": "plain_text",
    							"text": "View",
    							"emoji": true
    						},
    						"value": "view_alternate_2"
    					}
    				}
    			]
    		}
    	]
}' "$FIRST_HEADING" "$SECOND_HEADING" "$FRAGMENT_LINE" "$LINK_HEADING" "$LINK_HEADING_2")

curl -X POST -H 'Content-type: application/json' --data "$json_string" "$SLACK_WEBHOOK_URL"

