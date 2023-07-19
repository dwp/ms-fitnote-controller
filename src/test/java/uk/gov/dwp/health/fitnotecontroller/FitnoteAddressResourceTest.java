package uk.gov.dwp.health.fitnotecontroller;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.exception.NewAddressException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FitnoteAddressResourceTest {
    private static final String VALID_ADDRESS =
            "{ \"sessionId\" :\"123456\"," +
                    " \"houseNameOrNumber\" : \"254\", " +
                    "\"street\" : \"Street\", " +
                    "\"city\": \"City\", " +
                    "\"postcode\" : \"SW1A 1AA\"}";
    private static final String INVALID_ADDRESS = "{}";
    private static final String SESSION_ID = "123456";

    @Mock
    private JsonValidator jsonValidator;

    @Mock
    private FitnoteControllerConfiguration config;

    @Mock
    private ImageStorage imageStorage;

    private FitnoteAddressResource resourceUnderTest;

    @Before
    public void setup() throws ImagePayloadException, IOException, CryptoException {
        ImagePayload item = new ImagePayload();
        item.setSessionId(SESSION_ID);

        when(imageStorage.getPayload(eq(SESSION_ID))).thenReturn(item);

        resourceUnderTest = new FitnoteAddressResource(imageStorage, jsonValidator);
    }

    @Test
    public void newAddressReturns500WhenUnexpectedExceptionOccurs() {
        Response response = resourceUnderTest.updateAddress(INVALID_ADDRESS);
        assertThat(response.getStatus(), is(500));

    }

    @Test
    public void newAddressReturns400WhenInvalidJsonBodyIsSupplied() throws NewAddressException {
        when(jsonValidator.validateAndTranslateAddress(INVALID_ADDRESS)).thenThrow(new NewAddressException("hello"));
        Response response = resourceUnderTest.updateAddress(INVALID_ADDRESS);
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void newAddressReturns200WhenAValidJsonBodyIsSupplied() throws IOException, NewAddressException {
        Address mockNewAddress = new ObjectMapper().readValue(VALID_ADDRESS, Address.class);
        when(jsonValidator.validateAndTranslateAddress(VALID_ADDRESS)).thenReturn(mockNewAddress);
        Response response = resourceUnderTest.updateAddress(VALID_ADDRESS);
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void imagePayloadIsUpdatedWithNewAddressWhenSessionIDExists() throws NewAddressException, IOException, ImagePayloadException, CryptoException {
        Address mockNewAddress = new ObjectMapper().readValue(VALID_ADDRESS, Address.class);
        when(jsonValidator.validateAndTranslateAddress(VALID_ADDRESS)).thenReturn(mockNewAddress);
        resourceUnderTest.updateAddress(VALID_ADDRESS);

        Address newAddress = imageStorage.getPayload(SESSION_ID).getClaimantAddress();
        assertThat(newAddress.getHouseNameOrNumber(), is("254"));
        assertThat(newAddress.getStreet(), is("Street"));
        assertThat(newAddress.getCity(), is("City"));
        assertThat(newAddress.getPostcode(), is("SW1A 1AA"));
    }
}
