# Fitnote Controller
[![Build Status](https://travis-ci.org/dwp/fitnote-controller.svg?branch=master)](https://travis-ci.org/dwp/fitnote-controller) [![Known Vulnerabilities](https://snyk.io/test/github/dwp/fitnote-controller/badge.svg)]

Main service functionality for submitting, checking and adding additional claimant details to a submitted fit note.      

Managed by DWP by Health PDU.

## Table of contents

* Development
* Testing
* Service Endpoints
* Configuration
* Health Checks
* Schedules
* Production Release

## Development

Dev using Java 11, Dropwizard

Clone repository and run `mvn clean package`

Starting the service -
 
    cd target; java -jar fitnote-controller-<version>.jar server path/to/config.yml

## Testing

Tests using Java using Junit 4, Cucumber for Java

Clone repository and run `mvn clean test`

## Configuration

      - AWS_ACCESS_KEY_ID= AWS KEY
      - AWS_SECRET_ACCESS_KEY= SECRET AWS KEY
      - SERVER_APP_PORT= PORT
      - REDIS_STORE_URI= URI FOR REDIS CLUSTER
      - KMS_ENDPOINT_OVERRIDE= KMS ENDPOINT
      - REDIS_DATA_KEY_REQUESTID= REQ ID FOR REDIS
      - SNS_DATA_KEY_REQUEST_ID= SNS DATA KEY ID
      - SNS_TOPIC_NAME= SNS TOPIC
      - SNS_ROUTING_KEY= SNS ROUTING KEY
      - SNS_SUBJECT= SNS SUBJECT
      - S3_ENDPOINT_OVERRIDE= S3 ENDPOINT
      - ENDPOINT_OVERRIDE= AWS ENDPOINT OVERIDE
      - S3_BUCKET_NAME= S3 BUCKET NAME
      - REGION= AWS REGION
      - ESTIMATED_REQUEST_MEMORY_MB= ESTIMATED MEM FOR EACH REQ

## Service Endpoints

each endpoint can be called in any order with the exception of `declaration` which will check all of the mandatory data items for the session have been set.  If any entries are missing or invalid at `declaration` the software will return an error.

All api endpoints check for existence of a sessionId in the redis store.  If the session does not exist, it is created and then updated based on the endpoint functionality. Each further call with the session id will update the current ImagePayload and set the expiry time in redis based on the `sessionExpiryTimeInSeconds` configuration parameter.

### `/photo`

**POST** request expecting JSON object of base 64 encoded image of the fitnote and a session id.

    {
        "image":"base64-encoded-string",
        "sessionId":"session1",
        "fileType": "application/pdf",
        "fileName": "fitnote.pdf",
        "browser": "Chrome",
        "device": "desktop",
        "os": "MacOS"
    }

Returns:-

* **202** :: Success.  Returns json session id `{"sessionId":"session1"}`
* **400** :: Bad or Malformed content.  Returns "Unable to process request" (error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)
* **503** :: Service Unavailable.  Returns "Unable to process request" when not enough memory is available on controller to OCR the image (errors are logged)

## `/imagestatus`

**GET** request, passing `sessionId` as a url parameter to return the status of the image

Returns:-

    {
        "fitnoteStatus":"<status>"
    }
    
* **200** :: Success.  Returns json status
* **400** :: Missing session-id in json (error is logged)

currently the list of statuses are

* `CREATED`
* `UPLOADED`
* `CHECKING`
* `FAILED_IMG_SIZE`
* `PASS_IMG_SIZE`
* `FAILED_IMG_OCR`
* `FAILED_IMG_OCR_PARTIAL`
* `PASS_IMG_OCR`
* `SUCCEEDED`
* `FAILED_ERROR`

## `/extendSession`

**GET**  request, passing the `sessionId` as a url parameter to grant another `sessionExpiryTimeInSeconds` to the current session.

Returns:-

* **200** :: Success (no return body)
* **400** :: Missing session-id in json (error is logged)

## `/address`

**POST** request with the claimant's address

    {
        "sessionId":"session1",
        "houseNameOrNumber": "221b",
        "street": "Baker Street",
        "city": "London",
        "postcode": "NW1 6XE"
    }

Returns:-

* **200** :: Success (no return body)
* **400** :: Bad or Malformed content; `houseNameOrNumber` & `postcode` are mandatory.  Returns "Unable to process request" (error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## `/nino`

**POST** request to specify the mandatory NINO

    {
        "sessionId":"123456",
        "nino":"AA370773"
    }

Returns:-

* **200** :: Success.  Returns json session id `{"sessionId":"session1"}`
* **400** :: Bad or Malformed content.  Returns "Unable to process request" (error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## `/mobile`

**POST** request to specify the optional mobile phone number

    {
        "sessionId":"123456",
        "mobileNumber":"07877654321"
    }

Returns:-

* **200** :: Success.  Returns json session id `{"sessionId":"session1"}`
* **400** :: Bad or Malformed content.  Returns "Unable to process request" (error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## `/declaration`

**POST** request to complete the user journey with an accept.

    {
        "sessionId":"session1",
        "accepted":true
    }

* _true_ : declare ok, package, send to SNS and clear session
* _false_ : declare not ok, log, clear the session and return

Returns:-

* **200** :: Success.
* **400** :: Bad or Malformed content.  Returns what is missing (detailed error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## Query Endpoints

## `/queryNino`

**POST** request to query the nino details for a given sessionId.

`{"sessionId":"session1"}`

Return data :-

    {
        "sessionId": "session1",
        "nino": "AA370773A"
    }

or (if no nino has been set for the session)

    {
        "sessionId": "session1",
        "nino": ""
    }

Return code :-

* **200** :: Success, with the formatted JSON
* **400** :: Bad or Malformed content.  Returns what is missing (detailed error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## `/queryAddress`

**POST** request to query the address details for a given sessionId.

`{"sessionId":"session1"}`

Return data :-

    {
        "sessionId": "session1",
        "claimantAddress": {
            "sessionId": "session1",
            "houseNameOrNumber": "221b",
            "street": "Baker Street",
            "city": "London",
            "postcode": "NW1 6XE"
        }
    }

or (if no address has been set for the session)

    {
        "sessionId": "session1",
        "claimantAddress": {
            "sessionId": null,
            "houseNameOrNumber": null,
            "street": null,
            "city": null,
            "postcode": null
        }
    }

Return code :-

* **200** :: Success, with the formatted JSON
* **400** :: Bad or Malformed content.  Returns what is missing (detailed error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## `/queryMobile`

**POST** request to query the mobile phone number details for a given sessionId.

`{"sessionId":"session1"}`

Return data :-

    {
        "sessionId": "session1",
        "mobileNumber": "07877123456"
    }

or (if no phone has been set for the session)

    {
        "sessionId": "session1",
        "mobileNumber": ""
    }

Return code :-

* **200** :: Success, with the formatted JSON
* **400** :: Bad or Malformed content.  Returns what is missing (detailed error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## Configuration Env Vars

* `SESSION_EXPIRY_TIME_IN_SECONDS` : the length of time before (in seconds) before an inactive session is cleared from the cache
* `IMAGE_REPLAY_EXPIRY_SECONDS` : the length of time before the replay cache is cleared for a submitted image
* `MAX_ALLOWED_IMAGE_REPLAY` : the threshold for the same image to be replayed to the service before an error is thrown and the submission rejected
* `IMAGE_HASH_SALT` : the 'salt' to use when creating the image hash for the replay cache
* `REDIS_STORE_URI`: the URI of the Redis instance (eg. redis://localhost:6379)
* `REDIS_ENCRYPT_MESSAGES`: whether to encrypt the ImagePayload before persisting to Redis
* `REDIS_ENCRYPTION_TRANSIT`: whether to SSL encrypt the connection to redis
* `KMS_ENDPOINT_OVERRIDE/REDIS_DATA_KEY_REQUESTID`: The `CryptoConfig` object for KMS interaction for redis activities (when `redisEncryptMessages is true)
* `SNS_ROUTING_KEY` : the routing key (added to the headers) for SNS published messages
* `SNS_TOPIC_NAME` : the SNS topic exchange name
* `SNS_SUBJECT` : The subject of the SNS notification message
* `SNS_ENCRYPT_MESSAGES`: whether to encrypt the SNS messages
* `KMS_ENDPOINT_OVERRIDE/SNS_DATA_KEY_REQUEST_ID`: The `CryptoConfig` object for KMS interaction for SNS activities
* `OCR_CHECKS_ENABLED` : is OCR enabled, defaults to **true**
* `FORCE_LANDSCAPE_IMAGE_SUBMISSION` : force the submissions of landscape images, defaults to **true**
* `REJECTING_OVERSIZE_IMAGES` : works in conjunction with `targetImageSizeKB` and will force a reject if it is not possible to compress the image to the target size.  Defaults to **true**
* `PDF_SCAN_DPI` : the DPI to use when the incoming document has been detected as PDF.  300 is the optimum (and default) value for this to generate a BufferedImage that is detailed enough to be read but small enough to be compressed to the target size.  >300 DPI will fail image compression.
* `TARGET_IMAGE_SIZE_KB` : the target compressed image size in KB defaults to **500**
* `GREY_SCALE` : grey scale the image during compression, defaults to **true**
* `MAX_LOG_CHARS` : the maximum number of OCR characters to output to the log (in debug mode), defaults to **50**
* `TARGET_BRIGHTNESS` : the brightness to use to optimise the image for OCR, defaults to **179**
* `BORDER_LOSS_PERCENTAGE` : the amount of border to clip, defaults to **10**
* `SCAN_TARGET_IMAGE_SIZE` : the compression image size to optimise for OCR scan, defaults to **1000**
* `HIGH_TARGET` : The maximum percentage threshold for accepted image OCR
* `DIAGONAL_TARGET` : the minimum success percentage on a diagonal corner of a 100% match, defaults to **20**
* `CONTRAST_CUT_OFF` : the contrast to apply against the image for optimal OCR, defaults to **105**
* `ESTIMATED_REQUEST_MEMORY_MB` : the expected request size (in megabytes) of a photo submissions, defaults to **3**.  if there is not enough free memory available to service the request it will be rejected


## Healthcheck

Health check can be found at **`/healthcheck` *[GET]***

## Version-info (Enabled via the APPLICATION_INFO_ENABLED env var)

Version info can be found at **`/version-info` *[GET]***

### Schedules

The CI pipeline has a stage which sets up a schedule to run the `develop` branch every night - the schedule can be found in the `CI/CD/Schedules` section of Gitlab.

## Production Release

To create production artefacts the following process must be followed https://confluence.service.dwpcloud.uk/display/DHWA/SRE

**Run Docker Tests**
docker-compose up --exit-code-from  cucumber-tests
