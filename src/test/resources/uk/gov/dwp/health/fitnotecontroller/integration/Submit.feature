Feature: Fitnote submit

  @FitnoteSubmitTest
  Scenario: Submit readable fitnote
      Given the http client is up
      When I hit the service url "http://localhost:9101/photo" with the following json body
        | image     | /OcrTest.jpg |
        | sessionId | "3"             |
      Then I receive a HTTP response of 202
      And I hit the service url "http://localhost:9101/imagestatus" with session id "3" getting return status 200 and finally containing the following json body
        | fitnoteStatus | SUCCEEDED |
#
  @FitnoteSubmitTest
  Scenario: Submit partial fitnote
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest_LHS.jpg |
      | sessionId | "4"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "4" getting return status 200 and finally containing the following json body
      | fitnoteStatus | FAILED_IMG_OCR_PARTIAL |

  @FitnoteSubmitTest
  Scenario: Submit unreadable fitnote
      Given the http client is up
      When I hit the service url "http://localhost:9101/photo" with the following json body
        | image     | /OcrTest_RHS.jpg |
        | sessionId | "5"                   |
      Then I receive a HTTP response of 202
      And I hit the service url "http://localhost:9101/imagestatus" with session id "5" getting return status 200 and finally containing the following json body
        | fitnoteStatus | FAILED_IMG_OCR |

  @FitnoteSubmitTest
  Scenario: Submit readable Fitnote with an empty session Id
      Given the http client is up
      When I hit the service url "http://localhost:9101/photo" with the following json body
        | image     | /OcrTest.jpg |
        | sessionId | ""            |
      Then I receive a HTTP response of 400

  @FitnoteSubmitTest
  Scenario: Submit a pdf scanned fitnote is a FAILED_IMG_OCR_PARTIAL at 300dpi colour
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /FullPage_Portrait.pdf |
      | sessionId | "46"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "46" getting return status 200 and finally containing the following json body
      | fitnoteStatus | FAILED_IMG_OCR_PARTIAL |

  @FitnoteSubmitTest
  Scenario: Submit readable fitnote - small
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /Ocr_small.jpg |
      | sessionId | "8"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "8" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |

  @FitnoteSubmitTest
  Scenario: Submit invalid json as Fitnote Image
      Given the http client is up
      When I hit the service url "http://localhost:9101/photo" with the following json body
        |||
      Then I receive a HTTP response of 400

  @FitnoteSubmitTest
  Scenario: Verify that the timeout functionality kicks in after 60 seconds
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /FullPage_Portrait.jpg |
      | sessionId | "11"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "11" getting return status 200 and finally timing out trying to match the following body
      | fitnoteStatus | NEVER_GONNA_HAPPEN |

  @FitnoteSubmitTest
  Scenario: Replay readable fitnote until failure
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "567"             |
    Then I receive a HTTP response of 202
    Then I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "568"             |
    And I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "567"             |
    Then I receive a HTTP response of 202
    Then I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "568"             |
    And I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "567"             |
    Then I receive a HTTP response of 202
    Then I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "568"             |
    And I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "567"             |
    Then I receive a HTTP response of 202
    Then I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "568"             |
    And I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "567"             |
    Then I receive a HTTP response of 202
    Then I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "568"             |
    And I receive a HTTP response of 202
    Then I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "569"             |
    And I receive a HTTP response of 400
