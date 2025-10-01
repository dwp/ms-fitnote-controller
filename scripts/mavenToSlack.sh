#!/bin/bash

# for running/testing locally
#POM_INCLUDE_PATH=pom.xml

#CI_PIPELINE_URL=https://gitlab.com/dwp/health/fitnote/components/ms-datamatrix-creator/-/pipelines/1927999487

dependencyVersions=$(mvn -f "$POM_INCLUDE_PATH" -U --no-transfer-progress versions:display-dependency-updates -Dversions.outputLineWidth=120 -DprocessDependencyManagement=false -DprocessPluginDependenciesInPluginManagement=false|grep " -> "|awk '{print $2": "$4" -> "$6}')
pluginVersions=$(mvn -f "$POM_INCLUDE_PATH" -U --no-transfer-progress versions:display-plugin-updates -Dversions.outputLineWidth=120 -DprocessDependencyManagement=false -DprocessPluginDependenciesInPluginManagement=false|grep " -> "|awk '{print $2": "$4" -> "$6}')


REPO_PATH=${CI_PIPELINE_URL%%/-*}

FIRST_HEADING="*${REPO_PATH##*/} - Maven dependencies*"
SECOND_HEADING="<${REPO_PATH}|${REPO_PATH##*/}> \n :building_construction: \n"
LINK_HEADING="<${REPO_PATH}|${REPO_PATH##*/}>  :package:"
LINK_HEADING_2="<${CI_PIPELINE_URL}|Pipeline>  :electric_plug:"


MAVEN_DEPENDENCY_LINE="*Dependencies:* \n$dependencyVersions"
MAVEN_PLUGIN_LINE="*Plugins:* \n $pluginVersions"

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
    			"text": {
            "type": "mrkdwn",
            "text": "%s"
          }
    		},
    		{
          "type": "section",
          "block_id": "section581",
          "text": {
            "type": "mrkdwn",
            "text": "%s"
          }
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
}' "$FIRST_HEADING" "$SECOND_HEADING" "$MAVEN_DEPENDENCY_LINE" "$MAVEN_PLUGIN_LINE" "$LINK_HEADING" "$LINK_HEADING_2")

curl -X POST -H 'Content-type: application/json' --data "$json_string" "$SLACK_WEBHOOK_URL"

