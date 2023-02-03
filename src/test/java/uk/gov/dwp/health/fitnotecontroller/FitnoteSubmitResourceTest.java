package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.junittoolbox.PollingWait;
import com.googlecode.junittoolbox.RunnableAssert;
import gherkin.deps.net.iharder.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.StatusItem;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageHashException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.ImageCompressor;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import uk.gov.dwp.health.fitnotecontroller.utils.MemoryChecker;
import uk.gov.dwp.health.fitnotecontroller.utils.OcrChecker;

import javax.sound.midi.SysexMessage;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1192", "squid:S3008", "squid:S00116"})
// allow string literals and naming convention for variables (left for clarity)
public class FitnoteSubmitResourceTest {

  private static final String SESSION = "session1";

  private static final String OCR_LOGGING_DATA =
      "\"fileType\": \"application/pdf\", \"fileName\": \"fitnote.pdf\", \"browser\": \"Chrome\", \"device\": \"desktop\", \"os\": \"MacOS”,";

  private static byte[] COMPRESSED_PAGE_LARGE;
  private static byte[] COMPRESSED_PAGE_FINAL;

  private static final Optional<String> SESSION_ID = Optional.of(SESSION);
  private static final Optional<String> UNKNOWN_SESSION_ID = Optional.of("Unknown session id");
  private static final Optional<String> MISSING_SESSION_ID = Optional.empty();


  private static String LANDSCAPE_FITNOTE_IMAGE;
  private static String PORTRAIT_FITNOTE_IMAGE;
  private static String PDF_FITNOTE_IMAGE;
  private static String PORTRAIT_JSON;
  private static String VALID_JSON;
  private static String PDF_JSON;
  private int OVER_MAX_MEMORY;

  private FitnoteSubmitResource resourceUnderTest;

  @Mock
  private ImageStorage imageStorage;

  @Mock
  private JsonValidator validator;

  @Mock
  private OcrChecker ocrChecker;

  @Mock
  private FitnoteControllerConfiguration controllerConfiguration;

  @Mock
  private ImageCompressor imageCompressor;

  @BeforeClass
  public static void init() throws IOException {
    COMPRESSED_PAGE_LARGE = FileUtils.readFileToByteArray(new File("src/test/resources/EmptyPageBigger.jpg"));
    COMPRESSED_PAGE_FINAL = FileUtils.readFileToByteArray(new File("src/test/resources/EmptyPage.jpg"));

    LANDSCAPE_FITNOTE_IMAGE = Base64.encodeFromFile("src/test/resources/FullPage_Landscape.jpg");
    PORTRAIT_FITNOTE_IMAGE = Base64.encodeFromFile("src/test/resources/FullPage_Portrait.jpg");
    PDF_FITNOTE_IMAGE = Base64.encodeFromFile("src/test/resources/FullPage_Portrait.pdf");

    PORTRAIT_JSON = "{" + OCR_LOGGING_DATA +  "\"image\":\"" + PORTRAIT_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    VALID_JSON = "{" + OCR_LOGGING_DATA  + "\"image\":\"" + LANDSCAPE_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    PDF_JSON = "{" + OCR_LOGGING_DATA + "\"image\":\"" + PDF_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
  }

  @Before
  public void setup() throws IOException, ImagePayloadException, ImageCompressException, CryptoException {
    when(imageCompressor.compressBufferedImage(any(BufferedImage.class), eq(3), eq(false))).thenReturn(COMPRESSED_PAGE_LARGE);
    when(imageCompressor.compressBufferedImage(any(BufferedImage.class), eq(2), eq(true))).thenReturn(COMPRESSED_PAGE_FINAL);
    when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(3);
    when(controllerConfiguration.getScanTargetImageSizeKb()).thenReturn(3);
    when(controllerConfiguration.getTargetImageSizeKB()).thenReturn(2);
    when(controllerConfiguration.isGreyScale()).thenReturn(true);

    resourceUnderTest = new FitnoteSubmitResource(controllerConfiguration, validator, ocrChecker, imageStorage, imageCompressor);

    ImagePayload returnValue = new ImagePayload();
    returnValue.setSessionId(SESSION);

    when(imageStorage.getPayload(anyString())).thenReturn(returnValue);

    long freeMemory = MemoryChecker.returnCurrentAvailableMemoryInMb(Runtime.getRuntime());
    OVER_MAX_MEMORY = (int) freeMemory + 300;
  }

  @Test
  public void checkFitnoteCallFailsWhenNoSessionIdParameterExists() throws ImagePayloadException, IOException, CryptoException {
    Response response = resourceUnderTest.checkFitnote(MISSING_SESSION_ID);
    assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
  }

  @Test
  public void checkFitnoteCallWhenImageStoreThrowsException() throws ImagePayloadException, IOException, CryptoException {
   ImagePayload imagePayload = new ImagePayload();
   imagePayload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_MAX_REPLAY);
    when(imageStorage.getPayload(anyString())).thenReturn(imagePayload);
    Response response = resourceUnderTest.checkFitnote(UNKNOWN_SESSION_ID);
    assertThat(response.getStatus(), is(equalTo(SC_OK)));
    String responseBody = (String) response.getEntity();
    assertThat(responseBody, is(equalTo("{\"fitnoteStatus\":\"FAILED_IMG_MAX_REPLAY\"}")));
  }

  @Test
  public void checkFitnoteCallIsOkWithUnknownSessionIdParameterIsPassedIn() throws ImagePayloadException, IOException, CryptoException {
    Response response = resourceUnderTest.checkFitnote(UNKNOWN_SESSION_ID);
    assertThat(response.getStatus(), is(equalTo(SC_OK)));
  }

  @Test
  public void checkFitnoteCallFailsWhenNoSessionIdParameterIsPassedIn() throws ImagePayloadException, IOException, CryptoException {
    Response response = resourceUnderTest.checkFitnote(MISSING_SESSION_ID);
    assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
  }

  @Test
  public void extendSessionCallIsOkWithUnknownSessionIdParameterIsPassedIn() throws ImagePayloadException, IOException, CryptoException {
    Response response = resourceUnderTest.extendSession(UNKNOWN_SESSION_ID);
    assertThat(response.getStatus(), is(equalTo(SC_OK)));
  }

  @Test
  public void extendSessionCallFailsWhenNoSessionIdParameterIsPassedIn() throws ImagePayloadException, IOException, CryptoException {
    Response response = resourceUnderTest.extendSession(MISSING_SESSION_ID);
    assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
  }

  @Test
  public void portraitImageFailsChecksWhenOn() throws IOException, CryptoException, ImagePayloadException, InterruptedException {
    when(controllerConfiguration.isLandscapeImageEnforced()).thenReturn(true);
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(PORTRAIT_FITNOTE_IMAGE);
    imagePayload.setSessionId(SESSION);
    createAndValidateImage(PORTRAIT_JSON, true, imagePayload);
    Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
    verify(validator).validateAndTranslateSubmission(PORTRAIT_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("FAILED_IMG_SIZE");
  }

  @Test
  public void failsWhenThereIsNotEnoughMemoryForPhoto() {
    when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(OVER_MAX_MEMORY);

    Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_SERVICE_UNAVAILABLE)));
  }

  @Test
  public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedJpg() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
    createAndValidateImage(VALID_JSON, true, imagePayload);
    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class)))
        .thenReturn(new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null));

    Response response = resourceUnderTest.submitFitnote(VALID_JSON);
    verify(validator).validateAndTranslateSubmission(VALID_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(2)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedPdf() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
    when(controllerConfiguration.getPdfScanDPI()).thenReturn(300);

    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(PDF_FITNOTE_IMAGE);
    createAndValidateImage(PDF_JSON, true, imagePayload);

    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class)))
        .thenReturn(new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null));
    Response response = resourceUnderTest.submitFitnote(PDF_JSON);
    verify(validator).validateAndTranslateSubmission(PDF_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(2)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void jsonIsPassedIntoServiceWithOcrDisabledAnd202IsReturned() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(false);
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
    createAndValidateImage(VALID_JSON, true, imagePayload);
    Response response = resourceUnderTest.submitFitnote(VALID_JSON);
    verify(validator).validateAndTranslateSubmission(VALID_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(1)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void invalidJsonReturns400ImagePayloadException() throws CryptoException, ImagePayloadException, IOException {
    String json = "invalidJson";
    ImagePayload imagePayload = new ImagePayload();
    imagePayload.setSessionId("12");
    imagePayload.setFitnoteCheckStatus(ImagePayload.Status.CREATED);
    imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
    when(validator.validateAndTranslateSubmission(anyString())).thenReturn(imagePayload);
    when(imageStorage.getPayload("12")).thenThrow(new ImagePayloadException(""));
    Response response = resourceUnderTest.submitFitnote(json);
    assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
    verifyNoMoreInteractions(ocrChecker);
  }

  @Test
  public void invalidJsonReturns400CryptoException() throws CryptoException, ImagePayloadException, IOException {
    String json = "invalidJson";
    ImagePayload imagePayload = new ImagePayload();
    imagePayload.setSessionId("12");
    imagePayload.setFitnoteCheckStatus(ImagePayload.Status.CREATED);
    imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
    when(validator.validateAndTranslateSubmission(anyString())).thenReturn(imagePayload);
    when(imageStorage.getPayload("12")).thenThrow(new CryptoException(""));
    Response response = resourceUnderTest.submitFitnote(json);
    assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
    verifyNoMoreInteractions(ocrChecker);
  }

  @Test
  public void invalidJsonReturns500IOException() throws CryptoException, ImagePayloadException, IOException {
    String json = "invalidJson";
    ImagePayload imagePayload = new ImagePayload();
    imagePayload.setSessionId("12");
    imagePayload.setFitnoteCheckStatus(ImagePayload.Status.CREATED);
    imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
    when(validator.validateAndTranslateSubmission(anyString())).thenReturn(imagePayload);
    when(imageStorage.getPayload("12")).thenThrow(new IOException(""));
    Response response = resourceUnderTest.submitFitnote(json);
    assertThat(response.getStatus(), is(equalTo(SC_INTERNAL_SERVER_ERROR)));
    verifyNoMoreInteractions(ocrChecker);
  }

  @Test
  public void exceptionWhileTryingOCRIsTranslatedInto500() throws ImagePayloadException, IOException, CryptoException, InterruptedException {
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
    String json = "json";
    when(validator.validateAndTranslateSubmission(json)).thenReturn(imagePayload);
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenThrow(new IOException("thrown for test purposes"));
    Response response = resourceUnderTest.submitFitnote(json);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("FAILED_ERROR");
  }

  @Test
  public void confirmWhenImagePassesOCRCheck200AndSessionIdIsReturned() throws ImagePayloadException, IOException {
    ImagePayload imagePayload = new ImagePayload();
    String base64Image = "Base64Image";
    imagePayload.setImage(base64Image);
    String sessionId = SESSION;
    imagePayload.setSessionId(sessionId);
    createAndValidateImage(VALID_JSON, true, imagePayload);
    Response response = resourceUnderTest.submitFitnote(VALID_JSON);
    assertThat(response.getStatus(), is(SC_ACCEPTED));
    String entity = (String) response.getEntity();
    assertThat(entity, is(equalTo("{\"sessionId\":\"" + sessionId + "\"}")));
  }

  @Test
  public void confirmWhenImagePassesOCRCheckButFailsCompression200IsReturned() throws ImagePayloadException, IOException, ImageCompressException, InterruptedException {
    when(imageCompressor.compressBufferedImage(any(BufferedImage.class), any(int.class), eq(true))).thenReturn(null);
    ImagePayload imagePayload = new ImagePayload();
    imagePayload.setImage(Base64.encodeBytes(COMPRESSED_PAGE_LARGE));
    imagePayload.setSessionId(SESSION);
    createAndValidateImage(VALID_JSON, true, imagePayload);
    Response response = resourceUnderTest.submitFitnote(VALID_JSON);
    assertThat(response.getStatus(), is(SC_ACCEPTED));

    examineImageStatusResponseForValueOrTimeout("FAILED_ERROR");
  }

  @Test
  public void confirmWhenImageFailsOCRCheck400IsReturned() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
    createAndValidateImage(VALID_JSON, false, imagePayload);

    Response response = resourceUnderTest.submitFitnote(VALID_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("FAILED_IMG_OCR");

    verify(imageCompressor, times(1)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void failedImagePersistCausesInternalServiceException() throws IOException, CryptoException, ImagePayloadException, InterruptedException {
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(Base64.encodeBytes(COMPRESSED_PAGE_LARGE));

    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenThrow(new IOException("hello"));
    when(validator.validateAndTranslateSubmission(VALID_JSON)).thenReturn(imagePayload);
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);

    Response response = resourceUnderTest.submitFitnote(VALID_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("FAILED_ERROR");
  }

  @Test
  public void imageHashExceptionReturnsOk() throws ImageHashException, IOException, ImagePayloadException, CryptoException {
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(Base64.encodeBytes(COMPRESSED_PAGE_LARGE));
    when(validator.validateAndTranslateSubmission(any(String.class))).thenReturn(imagePayload);
    doThrow(new ImageHashException("")).when(imageStorage).updateImageHashStore(imagePayload);
    Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
    String entity = (String) response.getEntity();
    assertThat(entity, is(equalTo("{\"sessionId\":\"session1\"}")));
  }

    private void examineImageStatusResponseForValueOrTimeout(String expectedStatus) throws InterruptedException {
    TimeUnit.SECONDS.sleep(1); // pause before first execution to allow for async processes to begin/end
    PollingWait wait = new PollingWait().timeoutAfter(59, SECONDS).pollEvery(1, SECONDS);

    wait.until(new RunnableAssert("checking /imagestatus for current session") {
      @Override
      public void run() throws Exception {
        Response response = resourceUnderTest.checkFitnote(SESSION_ID);
        assertEquals(decodeResponse(response.getEntity().toString()).getFitnoteStatus(), expectedStatus);
      }
    });
  }

  private void createAndValidateImage(String json, boolean isValid, ImagePayload imagePayload) throws ImagePayloadException, IOException {
    when(validator.validateAndTranslateSubmission(json)).thenReturn(imagePayload);
    when(ocrChecker.imageContainsReadableText(imagePayload))
        .thenReturn(new ExpectedFitnoteFormat(isValid ? ExpectedFitnoteFormat.Status.SUCCESS
            : ExpectedFitnoteFormat.Status.FAILED, ""));
  }

  private StatusItem decodeResponse(String response) throws IOException {
    return new ObjectMapper().readValue(response, StatusItem.class);
  }
}
