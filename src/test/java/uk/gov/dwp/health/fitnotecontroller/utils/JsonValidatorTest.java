package uk.gov.dwp.health.fitnotecontroller.utils;

import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.DeclarationException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.exception.NewAddressException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"squid:S1192", "squid:S00116", "squid:S5976"}) // allow string literals and non-standard variable names for clarity
// not using parameterized test as not using latest junit
public class JsonValidatorTest extends JsonValidator {
    private static final String JPG_IMAGE_LANDSCAPE = "src/test/resources/FullPage_Landscape.jpg";
    private String ENCODED_LANDSCAPE_STRING = null;

    private static final String INVALID_HOUSE_LENGTH = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"123456789012345678901234567890123456\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String INVALID_STREET_LENGTH = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"123456789012345678901234567890123456\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String INVALID_CITY_LENGTH = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"123456789012345678901234567890123456\", \"postcode\" : \"NE12 9LG\"}";
    private static final String INVALID_POSTCODE_LENGTH = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"123456789012345678901234567890123456\"}";
    private static final String VALID_ADDRESS = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_SESSION_ID = "{ \"nameOrumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_NUMBER = "{ \"sessionId\" :\"123456\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_STREET = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\" , \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_CITY = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\",  \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_POSTCODE = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\"}";

    private JsonValidator validatorUnderTest = new JsonValidator();

    @Before
    public void setup() throws IOException {
        ENCODED_LANDSCAPE_STRING = Base64.encodeBase64String(FileUtils.readFileToByteArray(new File(JPG_IMAGE_LANDSCAPE)));
    }

    @Test
    public void imagePayloadObjectIsReturnedForValidJsonOnSubmission() throws ImagePayloadException {
        String sessionId = "123456";
        String json = "{\"image\":\"" + ENCODED_LANDSCAPE_STRING + "\",\"sessionId\":\"" + sessionId + "\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateSubmission(json);
        assertThat(imagePayload, is(notNullValue()));
        assertThat(imagePayload.getImage(), is(ENCODED_LANDSCAPE_STRING));
        assertThat(imagePayload.getSessionId(), is(sessionId));
    }

    @Test
    public void imagePayloadObjectIsReturnedForValidJson() throws ImagePayloadException {
        String sessionId = "123456";
        String json = "{\"sessionId\":\"" + sessionId + "\"," +
                "\"nino\":\"AA370773A\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateConfirmation(json);
        assertThat(imagePayload, is(notNullValue()));
        assertThat(imagePayload.getSessionId(), is(sessionId));
    }

    @Test
    public void nullSessionIdOnSubmissionThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateSubmission(("{\"image\":\"" + "678678678678678fuyff" + "\"}"));
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
        }
    }

    @Test
    public void nullJsonOnSubmissionThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateSubmission(null);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(NO_JSON));
        }
    }

    @Test
    public void emptySessionIdOnSubmissionThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateSubmission(("{\"image\":\"" + "678678678678678fuyff" + "\",\"sessionId\":\"\"}"));
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
        }
    }

    @Test
    public void invalidJsonOnSubmissionThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateSubmission("invalid json");
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains("JsonParseException"));
        }
    }

    @Test
    public void nullSessionIdOnConfirmationThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateSubmission(("{\"nino\":\"AA370773A\"," +
                    "\"mobileNumber\":\"0113999999\"}"));
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
        }
    }

    @Test
    public void emptySessionIdOnConfirmationThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateConfirmation("{\"sessionId\":\"\"," +
                    "\"nino\":\"AA370773A\"," +
                    "\"mobileNumber\":\"0113999999\"}");
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
        }
    }

    @Test
    public void invalidJsonOnConfirmationThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateConfirmation("invalid json");
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains("JsonParseException"));
        }
    }

    @Test
    public void emptyNINOOnConfirmationThrowsException() {
        try {
            String json = "{\"sessionId\":\"123456\"," +
                    "\"nino\":\"\"}";
            validatorUnderTest.validateAndTranslateConfirmation(json);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(INVALID_NINO));
        }
    }

    @Test
    public void missingNINOOnConfirmationThrowsException() {
        try {
            String json = "{\"sessionId\":\"123456\"}";
            validatorUnderTest.validateAndTranslateConfirmation(json);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(INVALID_NINO));
        }
    }

    @Test
    public void nullJsonOnConfirmationThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateConfirmation(null);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(NO_JSON));
        }
    }

    @Test
    public void jsonOnMobileConfirmationIsValid() throws ImagePayloadException {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\",\"mobileNumber\":\"07869123456\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), sessionId);
    }

    @Test
    public void jsonOnMobileConfirmationIsValidWithLeadingPlus() throws ImagePayloadException {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\",\"mobileNumber\":\"+44113999999\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), sessionId);
    }

    @Test
    public void jsonOnMobileConfirmationIsInvalid8Digits() {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\",\"mobileNumber\":\"12345678\"}";
        try {
            validatorUnderTest.validateAndTranslateMobileConfirmation(json);
            fail("should have thrown invalid mobile number error");
        } catch (ImagePayloadException e) {
            assertTrue("expecting missing mobile number", e.getMessage().contains(INVALID_MOBILE_NUMBER));
        }
    }

    @Test
    public void jsonOnMobileConfirmationIsValidWith11Digits() throws ImagePayloadException {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\",\"mobileNumber\":\"12345678901\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), sessionId);
    }

    @Test
    public void jsonOnMobileConfirmationIsValidWith15Digits() throws ImagePayloadException {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\",\"mobileNumber\":\"111111111111111\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), sessionId);
    }

    @Test
    public void jsonOnMobileConfirmationIsInvalidWith21Digits() {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\",\"mobileNumber\":\"111111111111111111111\"}";
        try {
            validatorUnderTest.validateAndTranslateMobileConfirmation(json);
            fail("should have thrown invalid mobile number error");
        } catch (ImagePayloadException e) {
            assertTrue("expecting missing mobile number", e.getMessage().contains(INVALID_MOBILE_NUMBER));
        }
    }


    @Test
    public void jsonOnMobileConfirmationIsInvalidWithPlusMidString() {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\",\"mobileNumber\":\"01139+99999\"}";
        try {
            validatorUnderTest.validateAndTranslateMobileConfirmation(json);
            fail("should have thrown invalid mobile number error");
        } catch (ImagePayloadException e) {
            assertTrue("expecting missing mobile number", e.getMessage().contains(INVALID_MOBILE_NUMBER));
        }
    }

    @Test
    public void jsonOnMobileConfirmationWithNoStringIsValid() throws ImagePayloadException {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\",\"mobileNumber\":\"\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), sessionId);
    }

    @Test
    public void jsonOnMobileConfirmationWithNoSecondData() throws ImagePayloadException {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), sessionId);
    }

    @Test
    public void jsonOnMobileConfirmationWithMobileNumberAsText() {
        String sessionId = "10";
        String json = "{\"sessionId\":\"" + sessionId + "\",\"mobileNumber\":\"A test message that should fail\"}";

        try {
            validatorUnderTest.validateAndTranslateMobileConfirmation(json);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(INVALID_MOBILE_NUMBER));
        }
    }


    @Test
    public void emptySessionIdOnDeclarationThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateDeclaration("{\"sessionId\":\"\",\"accepted\" : false}");
            fail("Should have thrown a DeclarationException");
        } catch (DeclarationException e) {
            assertTrue("Expecting a DeclarationException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
        }
    }

    @Test
    public void invalidJsonOnDeclarationThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateDeclaration("Invalid json");
            fail("Should have thrown a DeclarationException");
        } catch (DeclarationException e) {
            assertTrue("Expecting a DeclarationException", e.getMessage().contains("JsonParseException"));
        }
    }

    @Test
    public void nullJsonOnDeclarationThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateDeclaration(null);
            fail("Should have thrown a DeclarationException");
        } catch (DeclarationException e) {
            assertTrue("Expecting a DeclarationException", e.getMessage().contains(NO_JSON));
        }
    }

    @Test(expected = NewAddressException.class)
    public void nullAddressThrowsException() throws NewAddressException {
        validatorUnderTest.validateAndTranslateAddress(null);
    }

    @Test
    public void emptySessionIdOnAddressThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_SESSION_ID);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains("Unrecognized field"));
        }
    }

    @Test
    public void emptyNumberOnAddressThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_NUMBER);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(MISSING_NUMBER_FROM_ADDRESS));
        }
    }

    @Test
    public void houseNameOrNumberLongerThan35Chars() {
        try {
            validatorUnderTest.validateAndTranslateAddress(INVALID_HOUSE_LENGTH);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(HOUSE_NUMBER_LENGTH_EXCEPTION));
        }
    }

    @Test
    public void streetNameLongerThan35Chars() {
        try {
            validatorUnderTest.validateAndTranslateAddress(INVALID_STREET_LENGTH);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(STREET_NAME_LENGTH_EXCEPTION));
        }
    }

    @Test
    public void cityLongerThan35Chars() {
        try {
            validatorUnderTest.validateAndTranslateAddress(INVALID_CITY_LENGTH);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(CITY_NAME_LENGTH_EXCEPTION));
        }
    }

    @Test
    public void postcodeLongerThan35Chars() {
        try {
            validatorUnderTest.validateAndTranslateAddress(INVALID_POSTCODE_LENGTH);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(POSTCODE_VALIDATION_FAILED));
        }
    }

    @Test
    public void emptyStreetOnAddressDoesNotThrowException() throws NewAddressException {
        validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_STREET);
    }

    @Test
    public void emptyCityOnAddressDoesNotThrowException() throws NewAddressException {
        validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_CITY);
    }

    @Test
    public void emptyPostcodeOnAddressThrowsException() {
        try {
            validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_POSTCODE);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(POSTCODE_VALIDATION_FAILED));
        }
    }

    @Test
    public void validJsonReturnsCorrectAddress() throws NewAddressException {
        Address addressReturned = validatorUnderTest.validateAndTranslateAddress(VALID_ADDRESS);
        assertThat(addressReturned.getSessionId(), is("123456"));
        assertThat(addressReturned.getHouseNameOrNumber(), is("254"));
        assertThat(addressReturned.getStreet(), is("Bakers Street"));
        assertThat(addressReturned.getCity(), is("London"));
        assertThat(addressReturned.getPostcode(), is("NE12 9LG"));
    }

}
