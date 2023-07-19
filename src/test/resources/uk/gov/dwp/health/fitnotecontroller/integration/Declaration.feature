Feature: Fitnote declaration

  @FitnoteDeclarationTest
  Scenario: Submit an accepted declaration with all mandatory session data present
    Given the http client is up
    And I create a sns topic named "fitnote-topic-exchange"
    And I create a catch all subscription for queue name "test-1" binding to topic "fitnote-topic-exchange" with msg visibility timeout of 2 seconds
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId | "11"         |
      | image     | /OcrTest.jpg |
    Then I receive a HTTP response of 202
    And I wait 2 seconds to guarantee message delivery
    And I hit the service url "http://localhost:9101/imagestatus" with session id "11" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId | "11"       |
      | nino      | "AA370773A" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    When I hit the service url "http://localhost:9101/address" with the following json body
      | sessionId         | "11"       |
      | houseNameOrNumber | "254"      |
      | postcode          | "NE12 9PG" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    And I hit the service url "http://localhost:9101/queryNino" with a POST and session id "11" getting return status 200 the following json body
      | sessionId | 11       |
      | nino      | AA370773A |
    And I hit the service url "http://localhost:9101/queryAddress" with a POST and session id "11" getting return status 200 the following json body
      | sessionId       | 11                                                                                           |
      | claimantAddress | {"sessionId":"11","houseNameOrNumber":"254","street":null,"city":null,"postcode":"NE12 9PG"} |
    And I hit the service url "http://localhost:9101/queryMobile" with a POST and session id "11" getting return status 200 the following json body
      | sessionId    | 11 |
      | mobileNumber | [blank] |
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId | "11" |
      | accepted  | true |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    And a message is successfully removed from the queue, there were a total of 1 messages on queue "test-1"
    And the message has a correlation id, delivery mode PERSISTENT and a valid ImagePayload with the following NINO, House number, Postcode and matching SessionId
      | nino              | "AA370773A" |
      | houseNameOrNumber | "254"      |
      | postcode          | "NE12 9PG" |
      | sessionId         | "11"       |
    And I clear all content for queue name "test-1"


  @FitnoteDeclarationTest
  Scenario: Submit a declined declaration with all mandatory session data present
    Given the http client is up
    And I create a sns topic named "fitnote-topic-exchange"
    And I create a catch all subscription for queue name "test-2" binding to topic "fitnote-topic-exchange" with msg visibility timeout of 2 seconds
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId | "12"         |
      | image     | /OcrTest.jpg |
    Then I receive a HTTP response of 202
    And I wait 2 seconds to guarantee message delivery
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId | "12"       |
      | nino      | "AA370773A" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    And I hit the service url "http://localhost:9101/queryNino" with a POST and session id "12" getting return status 200 the following json body
      | sessionId | 12       |
      | nino      | AA370773A |
    And I hit the service url "http://localhost:9101/queryAddress" with a POST and session id "12" getting return status 200 the following json body
      | sessionId       | 12                                                                                    |
      | claimantAddress | {"sessionId":null,"houseNameOrNumber":null,"street":null,"city":null,"postcode":null} |
    And I hit the service url "http://localhost:9101/queryMobile" with a POST and session id "12" getting return status 200 the following json body
      | sessionId    | 12 |
      | mobileNumber | [blank] |
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId | "12"  |
      | accepted  | false |
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test-2"
    And I clear all content for queue name "test-2"

  @FitnoteDeclarationTest
  Scenario: Submit an accepted declaration with session missing fitnote image
    Given the http client is up
    And I create a sns topic named "fitnote-topic-exchange"
    And I create a catch all subscription for queue name "test-3" binding to topic "fitnote-topic-exchange" with msg visibility timeout of 2 seconds
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId | "13"       |
      | nino      | "AA370773A" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    And I hit the service url "http://localhost:9101/queryNino" with a POST and session id "13" getting return status 200 the following json body
      | sessionId | 13       |
      | nino      | AA370773A |
    And I hit the service url "http://localhost:9101/queryAddress" with a POST and session id "13" getting return status 200 the following json body
      | sessionId       | 13                                                                                    |
      | claimantAddress | {"sessionId":null,"houseNameOrNumber":null,"street":null,"city":null,"postcode":null} |
    And I hit the service url "http://localhost:9101/queryMobile" with a POST and session id "13" getting return status 200 the following json body
      | sessionId    | 13 |
      | mobileNumber | [blank] |
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId | "13" |
      | accepted  | true |
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test-3"
    And I clear all content for queue name "test-3"

  @FitnoteDeclarationTest
  Scenario: Submit an accepted declaration with session missing an address
    Given the http client is up
    And I create a sns topic named "fitnote-topic-exchange"
    And I create a catch all subscription for queue name "test-4" binding to topic "fitnote-topic-exchange" with msg visibility timeout of 2 seconds
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId | "14"         |
      | image     | /OcrTest.jpg |
    Then I receive a HTTP response of 202
    And I wait 2 seconds to guarantee message delivery
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId | "14"       |
      | nino      | "AA370773A" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    And I hit the service url "http://localhost:9101/queryNino" with a POST and session id "14" getting return status 200 the following json body
      | sessionId | 14       |
      | nino      | AA370773A |
    And I hit the service url "http://localhost:9101/queryAddress" with a POST and session id "14" getting return status 200 the following json body
      | sessionId       | 14                                                                                    |
      | claimantAddress | {"sessionId":null,"houseNameOrNumber":null,"street":null,"city":null,"postcode":null} |
    And I hit the service url "http://localhost:9101/queryMobile" with a POST and session id "14" getting return status 200 the following json body
      | sessionId    | 14 |
      | mobileNumber | [blank] |
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId | "14" |
      | accepted  | true |
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test-4"
    And I clear all content for queue name "test-4"

  @FitnoteDeclarationTest
  Scenario: Submit an accepted declaration with session missing NINO
    Given the http client is up
    And I create a sns topic named "fitnote-topic-exchange"
    And I create a catch all subscription for queue name "test-5" binding to topic "fitnote-topic-exchange" with msg visibility timeout of 2 seconds
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId | "16"         |
      | image     | /OcrTest.jpg |
    Then I receive a HTTP response of 202
    And I wait 2 seconds to guarantee message delivery
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId | "16" |
      | accepted  | true |
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test-5"
    And I clear all content for queue name "test-5"


  @FitnoteDeclarationTest
  Scenario: Submit invalid json as declaration
    Given the http client is up
    And I create a sns topic named "fitnote-topic-exchange"
    And I create a catch all subscription for queue name "test-6" binding to topic "fitnote-topic-exchange" with msg visibility timeout of 2 seconds
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId | "17"         |
      | image     | /OcrTest.jpg |
    Then I receive a HTTP response of 202
    And I wait 2 seconds to guarantee message delivery
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId | "17"       |
      | nino      | "AA370773A" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    And I hit the service url "http://localhost:9101/queryNino" with a POST and session id "17" getting return status 200 the following json body
      | sessionId | 17       |
      | nino      | AA370773A |
    And I hit the service url "http://localhost:9101/queryAddress" with a POST and session id "17" getting return status 200 the following json body
      | sessionId       | 17                                                                                    |
      | claimantAddress | {"sessionId":null,"houseNameOrNumber":null,"street":null,"city":null,"postcode":null} |
    And I hit the service url "http://localhost:9101/queryMobile" with a POST and session id "17" getting return status 200 the following json body
      | sessionId    | 17 |
      | mobileNumber | [blank] |
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | [blank] | [blank] |
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test-6"
    And I hit the service url "http://localhost:9101/imagestatus" with session id "17" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |
    And I clear all content for queue name "test-6"

  @FitnoteDeclarationTest
  Scenario: Submit multiple accepted declarations with all mandatory session data present
    Given the http client is up
    And I create a sns topic named "fitnote-topic-exchange"
    And I create a catch all subscription for queue name "test-7" binding to topic "fitnote-topic-exchange" with msg visibility timeout of 2 seconds
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId | "22"         |
      | image     | /OcrTest.jpg |
    Then I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId | "23"         |
      | image     | /OcrTest.jpg |
    Then I receive a HTTP response of 202
    And I wait 2 seconds to guarantee message delivery
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId | "22"       |
      | nino      | "AA370773A" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId | "23"       |
      | nino      | "AA370773A" |
    Then I receive a HTTP response of 200
    When I hit the service url "http://localhost:9101/address" with the following json body
      | sessionId         | "22"       |
      | houseNameOrNumber | "254"      |
      | postcode          | "NE12 9PG" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    When I hit the service url "http://localhost:9101/address" with the following json body
      | sessionId         | "23"      |
      | houseNameOrNumber | "88"      |
      | postcode          | "LS6 9PG" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    When I hit the service url "http://localhost:9101/mobile" with the following json body
      | sessionId    | "23"          |
      | mobileNumber | "07866754321" |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    And I hit the service url "http://localhost:9101/imagestatus" with session id "23" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |
    And I hit the service url "http://localhost:9101/queryNino" with a POST and session id "23" getting return status 200 the following json body
      | sessionId | 23       |
      | nino      | AA370773A |
    And I hit the service url "http://localhost:9101/queryAddress" with a POST and session id "23" getting return status 200 the following json body
      | sessionId       | 23                                                                                         |
      | claimantAddress | {"sessionId":"23","houseNameOrNumber":"88","street":null,"city":null,"postcode":"LS6 9PG"} |
    And I hit the service url "http://localhost:9101/queryMobile" with a POST and session id "23" getting return status 200 the following json body
      | sessionId    | 23          |
      | mobileNumber | 07866754321 |
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId | "23" |
      | accepted  | true |
    Then I receive a HTTP response of 200
    And I hit the service url "http://localhost:9101/imagestatus" with session id "22" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |
    And I hit the service url "http://localhost:9101/queryNino" with a POST and session id "22" getting return status 200 the following json body
      | sessionId | 22       |
      | nino      | AA370773A |
    And I hit the service url "http://localhost:9101/queryAddress" with a POST and session id "22" getting return status 200 the following json body
      | sessionId       | 22                                                                                           |
      | claimantAddress | {"sessionId":"22","houseNameOrNumber":"254","street":null,"city":null,"postcode":"NE12 9PG"} |
    And I hit the service url "http://localhost:9101/queryMobile" with a POST and session id "22" getting return status 200 the following json body
      | sessionId    | 22 |
      | mobileNumber | [blank] |
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId | "22" |
      | accepted  | true |
    Then I receive a HTTP response of 200
    And I wait 2 seconds to guarantee message delivery
    And a message is successfully removed from the queue, there were a total of 2 messages on queue "test-7"
    And the message has a correlation id, delivery mode PERSISTENT and a valid ImagePayload with the following NINO, House number, Postcode and matching SessionId
      | nino              | "AA370773A" |
      | houseNameOrNumber | "88"       |
      | postcode          | "LS6 9PG"  |
      | sessionId         | "23"       |
    Then I wait 4 seconds for the visibility timeout to expire
    And a message is successfully removed from the queue, there were a total of 1 messages on queue "test-7"
    And the message has a correlation id, delivery mode PERSISTENT and a valid ImagePayload with the following NINO, House number, Postcode and matching SessionId
      | nino              | "AA370773A" |
      | houseNameOrNumber | "254"      |
      | postcode          | "NE12 9PG" |
      | sessionId         | "22"       |
    And there are no pending messages on queue "test-7"
    And I clear all content for queue name "test-7"
