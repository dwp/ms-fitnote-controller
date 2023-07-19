package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("squid:S1192") // allow string literals
public class FitnoteQueryResourceTest {
    private static final String ERROR_MSG = "Unable to process request";

    @Mock
    private ImageStorage imageStorage;

    private FitnoteQueryResource instance;

    @Before
    public void setup() {
        instance = new FitnoteQueryResource(imageStorage);
    }

    @Test
    public void validateNinoNullSessionIdRejects500() {
        Response response = instance.queryNino(null);

        assertThat("should get 400", response.getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat("error text", response.getEntity().toString(), is(equalTo(ERROR_MSG)));

        verifyNoInteractions(imageStorage);
    }

    @Test
    public void validateAddressNullSessionIdRejects500() {
        Response response = instance.queryAddress(null);

        assertThat("should get 400", response.getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat("error text", response.getEntity().toString(), is(equalTo(ERROR_MSG)));

        verifyNoInteractions(imageStorage);
    }

    @Test
    public void validateMobileNullSessionIdRejects500() {
        Response response = instance.queryMobile(null);

        assertThat("should get 400", response.getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat("error text", response.getEntity().toString(), is(equalTo(ERROR_MSG)));

        verifyNoInteractions(imageStorage);
    }

    @Test
    public void validateNinoEmptyJsonRejects400() throws ImagePayloadException, IOException, CryptoException {
        when(imageStorage.getPayload(eq(null))).thenThrow(new ImagePayloadException("Null sessionId rejected"));
        Response response = instance.queryNino("{}");

        assertThat("should get 400", response.getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat("error text", response.getEntity().toString(), is(equalTo(ERROR_MSG)));

        verify(imageStorage).getPayload(null);
    }

    @Test
    public void validateAddressEmptyJsonRejects400() throws ImagePayloadException, IOException, CryptoException {
        when(imageStorage.getPayload(eq(null))).thenThrow(new ImagePayloadException("Null sessionId rejected"));
        Response response = instance.queryAddress("{}");

        assertThat("should get 400", response.getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat("error text", response.getEntity().toString(), is(equalTo(ERROR_MSG)));

        verify(imageStorage).getPayload(null);
    }

    @Test
    public void validateMobileEmptyJsonRejects400() throws ImagePayloadException, IOException, CryptoException {
        when(imageStorage.getPayload(eq(null))).thenThrow(new ImagePayloadException("Null sessionId rejected"));
        Response response = instance.queryMobile("{}");

        assertThat("should get 400", response.getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat("error text", response.getEntity().toString(), is(equalTo(ERROR_MSG)));

        verify(imageStorage).getPayload(null);
    }

    @Test
    public void validateNinoPopulatedSuccessWithSessionId() throws ImagePayloadException, IOException, CryptoException {
        ImagePayload populatedPayload = new ImagePayload();
        populatedPayload.setSessionId("12345");
        populatedPayload.setNino("AA370773A");

        when(imageStorage.getPayload(anyString())).thenReturn(populatedPayload);
        Response response = instance.queryNino(buildJsonSessionId(populatedPayload.getSessionId()));

        assertThat("should get 200", response.getStatus(), is(equalTo(HttpStatus.SC_OK)));

        verify(imageStorage).getPayload(populatedPayload.getSessionId());

        String serialisedClass = response.getEntity().toString();
        ImagePayload returnPayload = new ObjectMapper().readValue(serialisedClass, ImagePayload.class);

        assertThat(returnPayload.getSessionId(), is(equalTo(populatedPayload.getSessionId())));
        assertThat(returnPayload.getNino(), is(equalTo(populatedPayload.getNino())));

        JsonNode jsonNode = new ObjectMapper().readTree(serialisedClass);
        assertNotNull(jsonNode.get("sessionId"));
        assertNotNull(jsonNode.get("nino"));
        assertNull(jsonNode.get("mobileNumber"));
        assertNull(jsonNode.get("claimantAddress"));
        assertNull(jsonNode.get("expiryTime"));
        assertNull(jsonNode.get("image"));
    }

    @Test
    public void validateAddressPopulatedSuccessWithSessionId() throws ImagePayloadException, IOException, CryptoException {
        ObjectMapper mapper = new ObjectMapper();

        ImagePayload populatedPayload = new ImagePayload();
        populatedPayload.setClaimantAddress(mapper.readValue(buildJsonAddressObject("12345"), Address.class));
        populatedPayload.setSessionId("12345");

        when(imageStorage.getPayload(anyString())).thenReturn(populatedPayload);
        Response response = instance.queryAddress(buildJsonSessionId(populatedPayload.getSessionId()));

        assertThat("should get 200", response.getStatus(), is(equalTo(HttpStatus.SC_OK)));

        verify(imageStorage).getPayload(populatedPayload.getSessionId());

        String serialisedClass = response.getEntity().toString();
        ImagePayload returnPayload = new ObjectMapper().readValue(serialisedClass, ImagePayload.class);

        assertThat(returnPayload.getSessionId(), is(equalTo(populatedPayload.getSessionId())));
        assertThat(returnPayload.getClaimantAddress().getHouseNameOrNumber(), is(equalTo(populatedPayload.getClaimantAddress().getHouseNameOrNumber())));
        assertThat(returnPayload.getClaimantAddress().getSessionId(), is(equalTo(populatedPayload.getClaimantAddress().getSessionId())));
        assertThat(returnPayload.getClaimantAddress().getPostcode(), is(equalTo(populatedPayload.getClaimantAddress().getPostcode())));

        JsonNode jsonNode = new ObjectMapper().readTree(serialisedClass);
        assertNotNull(jsonNode.get("sessionId"));
        assertNotNull(jsonNode.get("claimantAddress"));
        assertNull(jsonNode.get("mobileNumber"));
        assertNull(jsonNode.get("nino"));
        assertNull(jsonNode.get("expiryTime"));
        assertNull(jsonNode.get("image"));
    }

    @Test
    public void validateMobilePopulatedSuccessWithSessionId() throws ImagePayloadException, IOException, CryptoException {
        ImagePayload populatedPayload = new ImagePayload();
        populatedPayload.setMobileNumber("07866786123");
        populatedPayload.setSessionId("12345");

        when(imageStorage.getPayload(anyString())).thenReturn(populatedPayload);
        Response response = instance.queryMobile(buildJsonSessionId(populatedPayload.getSessionId()));

        assertThat("should get 200", response.getStatus(), is(equalTo(HttpStatus.SC_OK)));

        verify(imageStorage).getPayload(populatedPayload.getSessionId());

        String serialisedClass = response.getEntity().toString();
        ImagePayload returnPayload = new ObjectMapper().readValue(serialisedClass, ImagePayload.class);

        assertThat(returnPayload.getSessionId(), is(equalTo(populatedPayload.getSessionId())));
        assertThat(returnPayload.getMobileNumber(), is(equalTo(populatedPayload.getMobileNumber())));

        JsonNode jsonNode = new ObjectMapper().readTree(serialisedClass);
        assertNotNull(jsonNode.get("sessionId"));
        assertNotNull(jsonNode.get("mobileNumber"));
        assertNull(jsonNode.get("claimantAddress"));
        assertNull(jsonNode.get("nino"));
        assertNull(jsonNode.get("expiryTime"));
        assertNull(jsonNode.get("image"));
    }

    @Test
    public void validateNinoEmptySuccessWithSessionId() throws ImagePayloadException, IOException, CryptoException {
        ImagePayload populatedPayload = new ImagePayload();
        populatedPayload.setSessionId("12345");

        when(imageStorage.getPayload(anyString())).thenReturn(populatedPayload);
        Response response = instance.queryNino(buildJsonSessionId(populatedPayload.getSessionId()));

        assertThat("should get 200", response.getStatus(), is(equalTo(HttpStatus.SC_OK)));

        verify(imageStorage).getPayload(populatedPayload.getSessionId());

        String serialisedClass = response.getEntity().toString();
        ImagePayload returnPayload = new ObjectMapper().readValue(serialisedClass, ImagePayload.class);

        assertThat(returnPayload.getSessionId(), is(equalTo(populatedPayload.getSessionId())));
        assertThat(returnPayload.getNino(), is(equalTo(populatedPayload.getNino())));

        JsonNode jsonNode = new ObjectMapper().readTree(serialisedClass);
        assertNotNull(jsonNode.get("sessionId"));
        assertNotNull(jsonNode.get("nino"));
        assertNull(jsonNode.get("mobileNumber"));
        assertNull(jsonNode.get("claimantAddress"));
        assertNull(jsonNode.get("expiryTime"));
        assertNull(jsonNode.get("image"));
    }

    @Test
    public void validateAddressEmptySuccessWithSessionId() throws ImagePayloadException, IOException, CryptoException {
        ImagePayload populatedPayload = new ImagePayload();
        populatedPayload.setSessionId("12345");

        when(imageStorage.getPayload(anyString())).thenReturn(populatedPayload);
        Response response = instance.queryAddress(buildJsonSessionId(populatedPayload.getSessionId()));

        assertThat("should get 200", response.getStatus(), is(equalTo(HttpStatus.SC_OK)));

        verify(imageStorage).getPayload(populatedPayload.getSessionId());

        String serialisedClass = response.getEntity().toString();
        ImagePayload returnPayload = new ObjectMapper().readValue(serialisedClass, ImagePayload.class);

        assertThat(returnPayload.getSessionId(), is(equalTo(populatedPayload.getSessionId())));
        assertThat(returnPayload.getClaimantAddress().getHouseNameOrNumber(), is(equalTo(populatedPayload.getClaimantAddress().getHouseNameOrNumber())));
        assertThat(returnPayload.getClaimantAddress().getPostcode(), is(equalTo(populatedPayload.getClaimantAddress().getPostcode())));

        JsonNode jsonNode = new ObjectMapper().readTree(serialisedClass);
        assertNotNull(jsonNode.get("sessionId"));
        assertNotNull(jsonNode.get("claimantAddress"));
        assertNull(jsonNode.get("mobileNumber"));
        assertNull(jsonNode.get("nino"));
        assertNull(jsonNode.get("expiryTime"));
        assertNull(jsonNode.get("image"));
    }

    @Test
    public void validateMobileEmptySuccessWithSessionId() throws ImagePayloadException, IOException, CryptoException {
        ImagePayload populatedPayload = new ImagePayload();
        populatedPayload.setSessionId("12345");

        when(imageStorage.getPayload(anyString())).thenReturn(populatedPayload);
        Response response = instance.queryMobile(buildJsonSessionId(populatedPayload.getSessionId()));

        assertThat("should get 200", response.getStatus(), is(equalTo(HttpStatus.SC_OK)));

        verify(imageStorage).getPayload(populatedPayload.getSessionId());

        String serialisedClass = response.getEntity().toString();
        ImagePayload returnPayload = new ObjectMapper().readValue(serialisedClass, ImagePayload.class);

        assertThat(returnPayload.getSessionId(), is(equalTo(populatedPayload.getSessionId())));
        assertThat(returnPayload.getMobileNumber(), is(equalTo(populatedPayload.getMobileNumber())));

        JsonNode jsonNode = new ObjectMapper().readTree(serialisedClass);
        assertNotNull(jsonNode.get("sessionId"));
        assertNotNull(jsonNode.get("mobileNumber"));
        assertNull(jsonNode.get("claimantAddress"));
        assertNull(jsonNode.get("nino"));
        assertNull(jsonNode.get("expiryTime"));
        assertNull(jsonNode.get("image"));
    }

    @Test
    public void validateNinoThrownErrorIsHandled() throws ImagePayloadException, IOException, CryptoException {
        when(imageStorage.getPayload(anyString())).thenThrow(new CryptoException("bad bad bad"));

        Response response = instance.queryNino(buildJsonSessionId("123"));

        verify(imageStorage).getPayload("123");

        assertThat("should get 500", response.getStatus(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
        assertThat("error text", response.getEntity().toString(), is(equalTo(ERROR_MSG)));
    }

    @Test
    public void validateAddressThrownErrorIsHandled() throws ImagePayloadException, IOException, CryptoException {
        when(imageStorage.getPayload(anyString())).thenThrow(new CryptoException("bad bad bad"));

        Response response = instance.queryAddress(buildJsonSessionId("123"));

        verify(imageStorage).getPayload("123");

        assertThat("should get 500", response.getStatus(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
        assertThat("error text", response.getEntity().toString(), is(equalTo(ERROR_MSG)));
    }

    @Test
    public void validateMobileThrownErrorIsHandled() throws ImagePayloadException, IOException, CryptoException {
        when(imageStorage.getPayload(anyString())).thenThrow(new CryptoException("bad bad bad"));

        Response response = instance.queryMobile(buildJsonSessionId("123"));

        verify(imageStorage).getPayload("123");

        assertThat("should get 500", response.getStatus(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
        assertThat("error text", response.getEntity().toString(), is(equalTo(ERROR_MSG)));
    }

    private String buildJsonSessionId(String sessionId) {
        return String.format("{\"sessionId\":\"%s\"}", sessionId);
    }

    private String buildJsonAddressObject(String sessionId) {
        return String.format("{ \"sessionId\" :\"%s\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}", sessionId);
    }
}
