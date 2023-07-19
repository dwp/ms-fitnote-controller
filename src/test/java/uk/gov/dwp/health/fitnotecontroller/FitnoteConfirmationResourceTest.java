package uk.gov.dwp.health.fitnotecontroller;

import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class FitnoteConfirmationResourceTest {

    private static final String VALID_JSON = "{\"sessionId\":\"%s\"}";
    @Mock
    private ImageStorage imageStore;
    @Mock
    private JsonValidator jsonValidator;
    private FitnoteConfirmationResource resourceUnderTest;

    @Before
    public void setup() {
        resourceUnderTest = new FitnoteConfirmationResource(imageStore, jsonValidator);
    }

    @Test
    public void sendNinoToServiceRespondsWith200ForKnownSessionId() throws IOException, ImagePayloadException, CryptoException {
        String sessionId = "123456";
        String nino = "AA370773A";
        String json = buildNinoJsonPayload(sessionId, nino);
        ImagePayload newPayload = buildNinoPayload(sessionId, nino);

        when(jsonValidator.validateAndTranslateConfirmation(json)).thenReturn(newPayload);

        Response response = resourceUnderTest.confirmFitnote(json);

        assertThat(response.getStatus(), is(equalTo(HttpStatus.SC_OK)));
        String entity = (String) response.getEntity();
        assertThat(entity, Is.is(equalTo(String.format(VALID_JSON, sessionId))));

        verify(imageStore).updateNinoDetails(newPayload);

    }

    @Test
    public void confirmFitnoteForceCryptoException() throws IOException, CryptoException, ImagePayloadException {
        String sessionId = "123456";
        String nino = "AA370773A";
        String json = buildNinoJsonPayload(sessionId, nino);
        ImagePayload newPayload = buildNinoPayload(sessionId, nino);

        when(jsonValidator.validateAndTranslateConfirmation(json))
                .thenReturn(newPayload);

        doThrow(new CryptoException("CRYPTO"))
                .when(imageStore)
                .updateNinoDetails(newPayload);

        Response response = resourceUnderTest.confirmFitnote(json);

        assertEquals(500, response.getStatus());
        assertEquals("Internal Server Error", response.getStatusInfo().getReasonPhrase());
    }

    @Test
    public void confirmFitnoteForceIoException() throws ImagePayloadException, IOException, CryptoException {
        String sessionId = "123456";
        String nino = "AA370773A";
        String json = buildNinoJsonPayload(sessionId, nino);
        ImagePayload newPayload = buildNinoPayload(sessionId, nino);

        when(jsonValidator.validateAndTranslateConfirmation(json))
                .thenReturn(newPayload);

        doThrow(new IOException("IO Exception"))
                .when(imageStore)
                .updateNinoDetails(newPayload);

        Response response = resourceUnderTest.confirmFitnote(json);

        assertEquals(500, response.getStatus());
        assertEquals("Internal Server Error", response.getStatusInfo().getReasonPhrase());
    }

    @Test
    public void sendMobileToServiceRespondsWith200ForKnownSessionId() throws IOException,
            ImagePayloadException, CryptoException {

        String sessionId = "123456";
        String mobileNumber = "0113999999";
        String json = buildMobileJsonPayload(sessionId, mobileNumber);
        ImagePayload newPayload = buildMobilePayload(sessionId, mobileNumber);

        when(jsonValidator.validateAndTranslateMobileConfirmation(json)).thenReturn(newPayload);

        Response response = resourceUnderTest.confirmMobile(json);

        assertThat(response.getStatus(), is(equalTo(HttpStatus.SC_OK)));
        String entity = (String) response.getEntity();
        assertThat(entity, Is.is(equalTo(String.format(VALID_JSON, sessionId))));

        verify(imageStore).updateMobileDetails(newPayload);

    }

    @Test
    public void confirmMobileForceImagePayloadException() throws ImagePayloadException {
        String sessionId = "123456";
        String mobileNumber = "0113999999";
        String json = buildMobileJsonPayload(sessionId, mobileNumber);

        when(jsonValidator.validateAndTranslateMobileConfirmation(json))
                .thenThrow(new ImagePayloadException("EXCEPTION"));

        Response response = resourceUnderTest.confirmMobile(json);

        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getStatusInfo().getReasonPhrase());
    }

    @Test
    public void confirmMobileForceCryptoException() throws ImagePayloadException, IOException, CryptoException {
        String sessionId = "123456";
        String mobileNumber = "0113999999";
        String json = buildMobileJsonPayload(sessionId, mobileNumber);
        ImagePayload newPayload = buildMobilePayload(sessionId, mobileNumber);

        when(jsonValidator.validateAndTranslateMobileConfirmation(json))
                .thenReturn(newPayload);

        doThrow(new CryptoException("CRYPTO"))
                .when(imageStore)
                .updateMobileDetails(newPayload);

        Response response = resourceUnderTest.confirmMobile(json);

        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getStatusInfo().getReasonPhrase());
    }

    @Test
    public void confirmMobileForceIoException() throws ImagePayloadException, IOException, CryptoException {
        String sessionId = "123456";
        String mobileNumber = "0113999999";
        String json = buildMobileJsonPayload(sessionId, mobileNumber);
        ImagePayload newPayload = buildMobilePayload(sessionId, mobileNumber);

        when(jsonValidator.validateAndTranslateMobileConfirmation(json))
                .thenReturn(newPayload);

        doThrow(new IOException("IO Exception"))
                .when(imageStore)
                .updateMobileDetails(newPayload);

        Response response = resourceUnderTest.confirmMobile(json);

        assertEquals(500, response.getStatus());
        assertEquals("Internal Server Error", response.getStatusInfo().getReasonPhrase());
    }

    private ImagePayload buildMobilePayload(String sessionId, String mobileNumber) {
        ImagePayload payload = new ImagePayload();
        payload.setSessionId(sessionId);
        payload.setMobileNumber(mobileNumber);
        return payload;
    }

    private ImagePayload buildNinoPayload(String sessionId, String nino) {
        ImagePayload payload = new ImagePayload();
        payload.setSessionId(sessionId);
        payload.setNino(nino);
        return payload;
    }

    @Test
    public void invalidJsonOnConfirmationReturns400() throws ImagePayloadException {
        String json = "{\"nino\":\"AA370773A\"," +
                "\"mobileNumber\":\"0113999999\"}";
        when(jsonValidator.validateAndTranslateConfirmation(json)).thenThrow(new ImagePayloadException("thrown for test purposes"));
        Response response = resourceUnderTest.confirmFitnote(json);
        assertThat(response.getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        verifyNoMoreInteractions(imageStore);
    }

    private String buildNinoJsonPayload(String sessionId, String nino) {
        return "{\"sessionId\":\"" + sessionId + "\"," +
                "\"nino\":\"" + nino + "\"}";
    }

    private String buildMobileJsonPayload(String sessionId, String mobileNumber) {
        return "{\"sessionId\":\"" + sessionId + "\"," +
                "\"mobileNumber\":\"" + mobileNumber + "\"}";
    }

}
