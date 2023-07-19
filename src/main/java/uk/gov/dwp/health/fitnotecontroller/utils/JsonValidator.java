package uk.gov.dwp.health.fitnotecontroller.utils;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.Declaration;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.DeclarationException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.exception.NewAddressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ObjectBuildException;
import org.slf4j.Logger;
import uk.gov.dwp.regex.NinoValidator;
import uk.gov.dwp.regex.PostCodeValidator;

import java.io.IOException;

import static com.fasterxml.jackson.core.StreamReadConstraints.DEFAULT_MAX_STRING_LEN;

public class JsonValidator {
  private static final Logger LOG = LoggerFactory.getLogger(JsonValidator.class.getName());
  private static final String SESSION_ID_MISSING = "Session ID missing";
  private static final String MISSING_ACCEPTED_VALUED = "Missing accepted valued";
  private static final String INVALID_VALUE = "Invalid value";
  static final String SESSION_ID_IS_MANDATORY = "Session ID is mandatory";
  static final String INVALID_NINO = "Invalid Nino";
  static final String INVALID_MOBILE_NUMBER = "Invalid Mobile Number";
  static final String MISSING_NUMBER_FROM_ADDRESS = "Missing number from address";
  static final String HOUSE_NUMBER_LENGTH_EXCEPTION = "House number length exception";
  static final String STREET_NAME_LENGTH_EXCEPTION = "Street name length exception";
  static final String CITY_NAME_LENGTH_EXCEPTION = "City name length exception";
  static final String POSTCODE_VALIDATION_FAILED = "Postcode validation failed";
  static final String NO_JSON = "No Json";

  private int maxStringLength;

  public JsonValidator() {
  }

  public JsonValidator(FitnoteControllerConfiguration config) {
    this.maxStringLength = config.getObjectMaxStringLength();
  }


  public ImagePayload validateAndTranslateSubmission(String json) throws ImagePayloadException {
    ImagePayload payload;
    try {
      payload = buildObjectFromJson(json, ImagePayload.class);
      if (invalidString(payload.getSessionId())) {
        LOG.debug(SESSION_ID_MISSING);
        throw new ImagePayloadException(SESSION_ID_IS_MANDATORY);
      }
      payload.setFitnoteCheckStatus(ImagePayload.Status.UPLOADED);
    } catch (ObjectBuildException | IOException e) {
      throw new ImagePayloadException(e);
    }

    return payload;
  }

  public ImagePayload validateAndTranslateConfirmation(String json) throws ImagePayloadException {
    ImagePayload payload;
    try {
      payload = buildObjectFromJson(json, ImagePayload.class);
      if (invalidString(payload.getSessionId())) {
        throw new ImagePayloadException(SESSION_ID_IS_MANDATORY);
      } else if (!NinoValidator.validateNINO(payload.getNino())) {
        throw new ImagePayloadException(INVALID_NINO);
      }
    } catch (ObjectBuildException | IOException e) {
      throw new ImagePayloadException(e);
    }
    return payload;
  }

  @SuppressWarnings("squid:S4784") // Reviewed that mobile number regex is not vulnerable.
  public ImagePayload validateAndTranslateMobileConfirmation(String json)
      throws ImagePayloadException {
    ImagePayload payload;
    try {
      payload = buildObjectFromJson(json, ImagePayload.class);
      if (invalidString(payload.getSessionId())) {
        throw new ImagePayloadException(SESSION_ID_IS_MANDATORY);
      } // may need better validation?
      if (!invalidString(payload.getMobileNumber())
          && !payload.getMobileNumber().matches("^\\+?[ 0-9]{11,20}")) {
        throw new ImagePayloadException(INVALID_MOBILE_NUMBER);
      }
    } catch (ObjectBuildException | IOException e) {
      throw new ImagePayloadException(e);
    }
    return payload;
  }

  @SuppressWarnings("squid:S5411")
  public Declaration validateAndTranslateDeclaration(String json) throws DeclarationException {
    Declaration declaration;
    try {
      declaration = buildObjectFromJson(json, Declaration.class);
      if (invalidString(declaration.getSessionId())) {
        throw new DeclarationException(SESSION_ID_IS_MANDATORY);
      } else if (declaration.isAccepted() == null) {
        throw new DeclarationException(MISSING_ACCEPTED_VALUED);
      }

      if (!declaration.isAccepted()) {
        throw new DeclarationException(INVALID_VALUE);
      }

    } catch (ObjectBuildException | IOException e) {
      throw new DeclarationException(e);
    }
    return declaration;
  }

  public Address validateAndTranslateAddress(String json) throws NewAddressException {
    Address addressReceived;
    try {
      addressReceived = buildObjectFromJson(json, Address.class);
      if (invalidString(addressReceived.getSessionId())) {
        throw new NewAddressException(SESSION_ID_IS_MANDATORY);
      } else if (invalidString(addressReceived.getHouseNameOrNumber())) {
        throw new NewAddressException(MISSING_NUMBER_FROM_ADDRESS);
      }

      if (invalidOverLengthCheck(addressReceived.getHouseNameOrNumber())) {
        throw new NewAddressException(HOUSE_NUMBER_LENGTH_EXCEPTION);
      } else if (invalidOverLengthCheck(addressReceived.getStreet())) {
        throw new NewAddressException(STREET_NAME_LENGTH_EXCEPTION);
      } else if (invalidOverLengthCheck(addressReceived.getCity())) {
        throw new NewAddressException(CITY_NAME_LENGTH_EXCEPTION);
      }

      if (!PostCodeValidator.validateInput(addressReceived.getPostcode())) {
        throw new NewAddressException(POSTCODE_VALIDATION_FAILED);
      }

    } catch (ObjectBuildException | IOException e) {
      throw new NewAddressException(e);
    }
    return addressReceived;
  }

  private <T> T buildObjectFromJson(String json, Class<T> objectToBuild)
      throws IOException, ObjectBuildException {
    if (json == null) {
      throw new ObjectBuildException(NO_JSON);
    }
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.getFactory()
            .setStreamReadConstraints(StreamReadConstraints
                    .builder()
                    .maxStringLength(maxStringLength == 0 ? DEFAULT_MAX_STRING_LEN
                            : maxStringLength)
                    .build());
    return objectMapper.readValue(json, objectToBuild);
  }

  private boolean invalidString(String str) {
    return str == null || str.trim().length() == 0;
  }

  private boolean invalidOverLengthCheck(String str) {
    boolean isInvalid = false;

    if (!invalidString(str)) {
      isInvalid = str.length() > 35;
    }

    return isInvalid;
  }
}
