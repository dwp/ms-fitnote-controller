package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.crypto.exceptions.EventsMessageException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.Declaration;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.DeclarationException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.health.messageq.amazon.sns.MessagePublisher;
import uk.gov.dwp.health.messageq.items.event.EventMessage;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1192", "squid:S1075"}) // allow string literals and hard-coded URI
public class FitnoteDeclarationResourceTest {

    private FitnoteDeclarationResource resource;
    private static final String ACCEPTED_DECLARATION = "{ \"sessionId\" : \"123456\" , \"accepted\" : true }";
    private static final String DECLINED_DECLARATION = "{ \"sessionId\" : \"123456\" , \"accepted\" : false}";
    private static final String INVALID_DECLARATION = "{ \"sessionId\" : \"123456\"  }";
    private static final String EMPTY_DECLARATION = "{ \"sessionId\" :  , \"accepted\" : }";
    private static final String VALID_NEW_ADDRESS = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String SUBJECT_NAME = "fitnote-subject";
    private static final String TOPIC_NAME = "test-topic";
    private static final String ROUTING_KEY = "route.one";

    @Mock
    private ImageStorage imageStore;

    @Mock
    private JsonValidator jsonValidator;

    @Mock
    private MessagePublisher snsPublisher;

    @Mock
    private FitnoteControllerConfiguration config;

    @Before
    public void setup() {
        resource = new FitnoteDeclarationResource(imageStore, jsonValidator, snsPublisher, config);

        when(config.getSnsRoutingKey()).thenReturn(ROUTING_KEY);
        when(config.getSnsTopicName()).thenReturn(TOPIC_NAME);
        when(config.getSnsSubject()).thenReturn(SUBJECT_NAME);
    }

    @Test
    public void returns200WithValidAcceptedDeclarationPlainContent() throws DeclarationException, IOException, ImagePayloadException, CryptoException, NoSuchMethodException, IllegalAccessException, InstantiationException, EventsMessageException, InvocationTargetException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am image", "123456", "AA370773A", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(200));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verify(snsPublisher, times(1)).publishMessageToSnsTopic(eq(false), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));
    }

    @Test
    public void returns200WithValidAcceptedDeclarationEncryptedContent() throws DeclarationException, IOException, ImagePayloadException, CryptoException, NoSuchMethodException, IllegalAccessException, InstantiationException, EventsMessageException, InvocationTargetException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am image", "123456", "AA370773A", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);
        when(config.isSnsEncryptMessages()).thenReturn(true);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(200));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verify(snsPublisher, times(1)).publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));
    }

    @Test
    public void return400WithMissingImagePayload() throws IOException, DeclarationException, ImagePayloadException, CryptoException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload(null, "123456", "AA370773A", "123456", false);
        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity().toString(), containsString("without a Fitnote Image"));
        assertThat(response.getStatus(), is(400));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verifyZeroInteractions(snsPublisher);
    }

    @Test
    public void return400WithMissingNinoPayload() throws IOException, DeclarationException, ImagePayloadException, CryptoException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am an image", "123456", null, "123456", true);
        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity().toString(), containsString("without a valid NINO"));
        assertThat(response.getStatus(), is(400));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verifyZeroInteractions(snsPublisher);
    }

    @Test
    public void return400WithMissingAddress() throws IOException, DeclarationException, ImagePayloadException, CryptoException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am an image", "123456", "AA370773A", "123456", true);

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity().toString(), containsString("Address must be specified"));
        assertThat(response.getStatus(), is(400));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verifyZeroInteractions(snsPublisher);
    }

    @Test
    public void returns400WithInvalidDeclaration() throws DeclarationException {
        when(jsonValidator.validateAndTranslateDeclaration(INVALID_DECLARATION)).thenThrow(new DeclarationException("test exception"));

        Response response = resource.submitDeclaration(INVALID_DECLARATION);
        assertThat(response.getStatus(), is(400));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(INVALID_DECLARATION));
        verifyZeroInteractions(snsPublisher);
    }

    @Test
    public void returns400WithEmptyDeclaration() throws DeclarationException {
        when(jsonValidator.validateAndTranslateDeclaration(EMPTY_DECLARATION)).thenThrow(new DeclarationException("test exception"));

        Response response = resource.submitDeclaration(EMPTY_DECLARATION);
        assertThat(response.getStatus(), is(400));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(EMPTY_DECLARATION));
        verifyZeroInteractions(snsPublisher);
    }

    @Test
    public void returns400WithADeclinedDeclaration() throws DeclarationException {
        when(jsonValidator.validateAndTranslateDeclaration(DECLINED_DECLARATION)).thenThrow(new DeclarationException("Invalid value"));

        Response response = resource.submitDeclaration(DECLINED_DECLARATION);
        assertThat(response.getStatus(), is(400));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(DECLINED_DECLARATION));
        verifyZeroInteractions(snsPublisher);
    }

    @Test
    public void returns500WhenAnUnexpectedExceptionIsThrown() throws DeclarationException {
        when(jsonValidator.validateAndTranslateDeclaration("{\"test\":\"string\"}")).thenThrow(DeclarationException.class);
        Response response = resource.submitDeclaration("{\"test\":\"string\"}");

        assertThat(response.getEntity(), is("Unable to process request"));
        assertThat(response.getStatus(), is(400));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq("{\"test\":\"string\"}"));
        verifyZeroInteractions(snsPublisher);
    }

    @Test
    public void returns500WhenASnsExceptionIsThrown() throws IOException, DeclarationException, ImagePayloadException, NoSuchMethodException, InvocationTargetException, EventsMessageException, InstantiationException, IllegalAccessException, CryptoException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload(" ", "123456", "NINO", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        doThrow(new EventsMessageException("i am an exception!")).when(snsPublisher).publishMessageToSnsTopic(eq(false), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(500));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verify(snsPublisher, times(1)).publishMessageToSnsTopic(eq(false), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));
    }

    @Test
    public void returns400WhenAcceptedDeclarationWithNoNino() throws DeclarationException, IOException, ImagePayloadException, CryptoException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);

        ImagePayload payload = buildImagePayload(" ", "123456", "", "", true);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity(), is("Cannot process declaration without a valid NINO"));
        assertThat(response.getStatus(), is(400));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verifyZeroInteractions(snsPublisher);
    }

    @Test
    public void returns400WhenAcceptedDeclarationWithBadImageCheck() throws DeclarationException, IOException, ImagePayloadException, CryptoException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);

        ImagePayload payload = buildImagePayload("i am image", "123456", "AA370773A", "234567", false);

        Address mockNewAddress = new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class);
        payload.setClaimantAddress(mockNewAddress);

        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity(), is("Cannot process declaration without a successfully scanned Fitnote Image"));
        assertThat(response.getStatus(), is(400));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verifyZeroInteractions(snsPublisher);
    }

    @Test
    public void returns200WithValidAddress() throws DeclarationException, IOException, NoSuchMethodException, InvocationTargetException, EventsMessageException, InstantiationException, IllegalAccessException, CryptoException, ImagePayloadException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload(" ", "123456", "NINO", "123456", true);

        Address mockNewAddress = new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class);
        payload.setClaimantAddress(mockNewAddress);

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(200));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verify(snsPublisher, times(1)).publishMessageToSnsTopic(eq(false), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));
    }

    @Test
    public void returns500WhenCryptoExceptionIsThrown() throws DeclarationException, IOException, ImagePayloadException, CryptoException, NoSuchMethodException, IllegalAccessException, InstantiationException, EventsMessageException, InvocationTargetException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am image", "123456", "AA370773A", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);
        when(config.isSnsEncryptMessages()).thenReturn(true);
        doThrow(new CryptoException("i am an exception!"))
                .when(snsPublisher).publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(500));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verify(snsPublisher, times(1)).publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));
    }

    @Test
    public void returns500WhenNoSuchMethodExceptionIsThrown() throws DeclarationException, IOException, ImagePayloadException, CryptoException, NoSuchMethodException, IllegalAccessException, InstantiationException, EventsMessageException, InvocationTargetException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am image", "123456", "AA370773A", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);
        when(config.isSnsEncryptMessages()).thenReturn(true);
        doThrow(new NoSuchMethodException("No such method exception!"))
                .when(snsPublisher).publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(500));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verify(snsPublisher, times(1)).publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));
    }

    @Test
    public void returns500WhenInvocationTargetExceptionIsThrown() throws DeclarationException, IOException, ImagePayloadException, CryptoException, NoSuchMethodException, IllegalAccessException, InstantiationException, EventsMessageException, InvocationTargetException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am image", "123456", "AA370773A", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);
        when(config.isSnsEncryptMessages()).thenReturn(true);
        doThrow(new InvocationTargetException(new Throwable(), "No such method exception!"))
                .when(snsPublisher).publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(500));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verify(snsPublisher, times(1)).publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));
    }

    @Test
    public void returns500WhenIllegalAccessExceptionIsThrown() throws DeclarationException, IOException, ImagePayloadException, CryptoException, NoSuchMethodException, IllegalAccessException, InstantiationException, EventsMessageException, InvocationTargetException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am image", "123456", "AA370773A", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);
        when(config.isSnsEncryptMessages()).thenReturn(true);
        doThrow(new IllegalAccessException("No such method exception!")).when(snsPublisher)
                .publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(500));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verify(snsPublisher, times(1)).publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));
    }

    @Test
    public void returns500WhenInstantiationExceptionIsThrown() throws DeclarationException, IOException, ImagePayloadException, CryptoException, NoSuchMethodException, IllegalAccessException, InstantiationException, EventsMessageException, InvocationTargetException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am image", "123456", "AA370773A", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);
        when(config.isSnsEncryptMessages()).thenReturn(true);
        doThrow(new InstantiationException("No such method exception!")).when(snsPublisher)
                .publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(500));

        verify(jsonValidator, times(1)).validateAndTranslateDeclaration(eq(ACCEPTED_DECLARATION));
        verify(snsPublisher, times(1)).publishMessageToSnsTopic(eq(true), eq(TOPIC_NAME), eq(SUBJECT_NAME), any(EventMessage.class), eq(null));
    }

    /*
     * private methods
     */

    private ImagePayload buildImagePayload(String imageString, String sessionId, String nino, String mobileNum, boolean imagePassed) {
        ImagePayload payload = new ImagePayload();
        payload.setImage(imageString);
        payload.setSessionId(sessionId);
        payload.setNino(nino);
        payload.setMobileNumber(mobileNum);

        if (imagePassed) {
            payload.setFitnoteCheckStatus(ImagePayload.Status.SUCCEEDED);
        } else {
            payload.setFitnoteCheckStatus(ImagePayload.Status.CREATED);
        }

        return payload;
    }
}
