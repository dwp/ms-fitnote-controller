Feature: Fitnote confirmation

  @FitnoteConfirmationTest
  Scenario: Submit NINO and mobile number
    Given the http client is up
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId | "10"        |
      | nino      | "AA370773A" |
      | mobileNumber | "07111223398"|
    Then I receive a HTTP response of 200

  @FitnoteConfirmMobileTest
  Scenario: Submit mobile number
    Given the http client is up
    When I hit the service url "http://localhost:9101/mobile" with the following json body
      | sessionId | "10"        |
      | mobileNumber | "07987654321"|
    Then I receive a HTTP response of 200

  @FitnoteConfirmMobileTest
  Scenario: Submit invalid json as mobile number
    Given the http client is up
    When I hit the service url "http://localhost:9101/mobile" with the following json body
      | [blank] | [blank] |
    Then I receive a HTTP response of 400

  @FitnoteConfirmationTest
  Scenario: Submit NINO without mobile number
    Given the http client is up
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId | "10"        |
      | nino      | "AB123456D" |
    Then I receive a HTTP response of 200


  @FitnoteConfirmationTest
  Scenario: Submit invalid json as Nino and mobile number
    Given the http client is up
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | [blank] | [blank] |
    Then I receive a HTTP response of 400

