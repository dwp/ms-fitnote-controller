Feature: Fitnote address

  @FitnoteAddressTest
  Scenario: Submit a new address containing mandatory and optional fields
    Given the http client is up
    When I hit the service url "http://localhost:9101/address" with the following json body
    | sessionId         | "99"               |
    | houseNameOrNumber | "254"             |
    | street            | "Bakers Street"   |
    | city              | "London"          |
    | postcode          | "NE12 9PG"        |
    Then I receive a HTTP response of 200

  @FitnoteAddressTest
  Scenario: Submit a new address containing mandatory fields only
    Given the http client is up
    When I hit the service url "http://localhost:9101/address" with the following json body
      | sessionId         | "99"               |
      | houseNameOrNumber | "254"             |
      | postcode          | "NE12 9PG"        |
    Then I receive a HTTP response of 200

  @FitnoteAddressTest
  Scenario: Submit a new address containing Optional fields only
    Given the http client is up
    When I hit the service url "http://localhost:9101/address" with the following json body
      | sessionId         | "99"               |
      | street            | "Bakers Street"   |
      | city              | "London"          |
    Then I receive a HTTP response of 400

  @FitnoteAddressTest
  Scenario: Submit a new address with empty mandatory fields
    Given the http client is up
    When I hit the service url "http://localhost:9101/address" with the following json body
      | sessionId         | "99"               |
      | houseNameOrNumber | ""                |
      | street            | "Bakers Street"   |
      | city              | "London"          |
      | postcode          | ""                |
    Then I receive a HTTP response of 400

   @FitnoteAddressTest
   Scenario: Submit invalid json as a new address
     Given the http client is up
     When I hit the service url "http://localhost:9101/address" with the following json body
       | [blank] | [blank] |
     Then I receive a HTTP response of 400
