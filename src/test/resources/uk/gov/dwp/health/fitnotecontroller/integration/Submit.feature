Feature: Fitnote submit

  @FitnoteSubmitTest
  Scenario: Submit readable fitnote
      Given the http client is up
      When I hit the service url "http://localhost:9101/photo" with the following json body
        | image     | /OcrTest.jpg |
        | sessionId | "3"          |
      Then I receive a HTTP response of 202
      And I hit the service url "http://localhost:9101/imagestatus" with session id "3" getting return status 200 and finally containing the following json body
        | fitnoteStatus | SUCCEEDED |

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
        | fitnoteStatus | FAILED_IMG_OCR_PARTIAL |

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
      | image     | /FullPage_bottomHalf.pdf |
      | sessionId | "46"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "46" getting return status 200 and finally containing the following json body
      | fitnoteStatus | FAILED_IMG_OCR_PARTIAL |

  @FitnoteSubmitTest
  Scenario: Submit a pdf scanned fitnote is an expanded search using cropping at 300dpi colour
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /FullPage_Portrait.pdf |
      | sessionId | "48"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "48" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |

  @FitnoteSubmitTest
  Scenario: Submit a pdf a scanned fitnote is an expanded search using edge detection at 300dpi colour
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /expanded_fitnote.pdf |
      | sessionId | "49"                  |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "49" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |

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
  Scenario: Submit readable fitnote heic format
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest.heic |
      | sessionId | "47"          |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "47" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |

  @FitnoteSubmitTest
  Scenario: Submit readable fitnote NHS format
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /NHS_fitnote.pdf |
      | sessionId | "25"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "25" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |

  @FitnoteSubmitTest
  Scenario: Submit a pdf password fitnote is a FAILED_IMG_PASSWORD
    Given the http client is up
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /password.pdf |
      | sessionId | "24"          |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9101/imagestatus" with session id "24" getting return status 200 and finally containing the following json body
      | fitnoteStatus | FAILED_IMG_PASSWORD |

  @FitnoteSubmitTest
  Scenario: Submit invalid json as Fitnote Image
      Given the http client is up
      When I hit the service url "http://localhost:9101/photo" with the following json body
        | [blank] | [blank] |
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
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | image     | /OcrTest-Replay.jpg |
      | sessionId | "569"             |
    Then I receive a HTTP response of 202 with the following json body
      | sessionId | "569"             |
    And I hit the service url "http://localhost:9101/imagestatus" with session id "569" getting return status 200 and finally timing out trying to match the following body
      | fitnoteStatus | FAILED_IMG_MAX_REPLAY |
