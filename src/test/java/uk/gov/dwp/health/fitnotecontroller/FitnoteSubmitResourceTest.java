package uk.gov.dwp.health.fitnotecontroller;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.health.fitnotecontroller.utils.ImageUtilsTest.getMatchingStringsMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.junittoolbox.PollingWait;
import com.googlecode.junittoolbox.RunnableAssert;
import jakarta.ws.rs.core.Response;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.hc.core5.http.ParseException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.DataMatrixResult;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.StatusItem;
import uk.gov.dwp.health.fitnotecontroller.domain.StringToMatch;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageHashException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.handlers.MsDataMatrixCreatorHandler;
import uk.gov.dwp.health.fitnotecontroller.utils.ImageCompressor;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import uk.gov.dwp.health.fitnotecontroller.utils.MemoryChecker;
import uk.gov.dwp.health.fitnotecontroller.utils.OcrChecker;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1192", "squid:S3008", "squid:S00116"})
// allow string literals and naming convention for variables (left for clarity)
public class FitnoteSubmitResourceTest {

  private static final String SESSION = "session1";

  private static final String OCR_LOGGING_DATA =
          "\"fileType\": \"application/pdf\", \"fileName\": \"fitnote.pdf\", \"browser\": \"Chrome\", \"device\": \"desktop\", \"os\": \"MacOS”,";

  private static byte[] COMPRESSED_PAGE_LARGE;
  private static byte[] COMPRESSED_PAGE_FINAL;
  private static byte[] EXPANDED_CROP_FINAL;
  private static byte[] NHS_FINAL;
  private static byte[] EXPANDED_EDGE_FINAL;

  private static final Optional<String> SESSION_ID = Optional.of(SESSION);
  private static final Optional<String> UNKNOWN_SESSION_ID = Optional.of("Unknown session id");
  private static final Optional<String> MISSING_SESSION_ID = Optional.empty();


  private static String LANDSCAPE_FITNOTE_IMAGE;
  private static String PORTRAIT_FITNOTE_IMAGE;
  private static String PDF_FITNOTE_IMAGE;
  private static String PDF_PASSWORD_FITNOTE_IMAGE;
  private static String PDF_NHS_FITNOTE_IMAGE;
  private static String LARGE_JPG_IMAGE;
  private static String EXPANDED_CROP_IMAGE;
  private static String EXPANDED_EDGE_IMAGE;
  private static String HEIC_IMAGE;
  private static String LARGE_HEIC;
  private static String EXR_IMAGE;
  private static String DATA_MATRIX_IMAGE;

  private static String PORTRAIT_JSON;
  private static String VALID_JSON;
  private static String LARGE_VALID_JSON;
  private static String PDF_JSON;
  private static String PDF_PASSWORD_JSON;
  private static String PDF_NHS_JSON;
  private static String EXPANDED_CROP_JSON;
  private static String EXPANDED_EDGE_JSON;
  private static String HEIC_JSON;
  private static String LARGE_HEIC_JSON;
  private static String EXR_JSON;

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

  @Mock
  private MsDataMatrixCreatorHandler msDataMatrixCreatorHandler;

  @BeforeClass
  public static void init() throws IOException {
    // needed for testing locally, please comment out when committing code
    // you need ImageMagick installed (https://imagemagick.org)
    // String myPath="imagemagickinstalledbinlocation";
    //ProcessStarter.setGlobalSearchPath(myPath);
    // -------------

    COMPRESSED_PAGE_LARGE = FileUtils.readFileToByteArray(new File("src/test/resources/EmptyPageBigger.jpg"));
    COMPRESSED_PAGE_FINAL = FileUtils.readFileToByteArray(new File("src/test/resources/EmptyPage.jpg"));
    EXPANDED_CROP_FINAL = FileUtils.readFileToByteArray(new File("src/test/resources/fitnoteExpandedSearch.jpg"));
    NHS_FINAL = FileUtils.readFileToByteArray(new File("src/test/resources/NHS_fitnote.jpg"));
    EXPANDED_EDGE_FINAL = FileUtils.readFileToByteArray(new File("src/test/resources/fitnotes/newSample/IMG_0009.JPG"));

    LANDSCAPE_FITNOTE_IMAGE = getEncodedImage("src/test/resources/FullPage_Landscape.jpg");
    PORTRAIT_FITNOTE_IMAGE = getEncodedImage("src/test/resources/FullPage_Portrait.jpg");
    PDF_FITNOTE_IMAGE = getEncodedImage("src/test/resources/FullPage_Portrait.pdf");
    PDF_PASSWORD_FITNOTE_IMAGE = getEncodedImage("src/test/resources/password.pdf");
    PDF_NHS_FITNOTE_IMAGE = getEncodedImage("src/test/resources/NHS_fitnote.pdf");
    EXPANDED_CROP_IMAGE = getEncodedImage("src/test/resources/fitnoteExpandedSearch.jpg");
    EXPANDED_EDGE_IMAGE = getEncodedImage("src/test/resources/fitnotes/newSample/IMG_0009.JPG");
    HEIC_IMAGE = getEncodedImage("src/test/resources/DarkPage.heic");
    LARGE_HEIC = getEncodedImage("src/test/resources/5MB.heic");
    EXR_IMAGE = getEncodedImage("src/test/resources/test-fail-type.txt");
    LARGE_JPG_IMAGE = getEncodedImage("src/test/resources/DarkPageLargeSize.jpg");
    DATA_MATRIX_IMAGE = getEncodedImage("src/test/resources/datamatrix.png");

    PORTRAIT_JSON = "{" + OCR_LOGGING_DATA +  "\"image\":\"" + PORTRAIT_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    VALID_JSON = "{" + OCR_LOGGING_DATA  + "\"image\":\"" + LANDSCAPE_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    LARGE_VALID_JSON = "{" + OCR_LOGGING_DATA  + "\"image\":\"" + LARGE_JPG_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    PDF_JSON = "{" + OCR_LOGGING_DATA + "\"image\":\"" + PDF_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    PDF_PASSWORD_JSON = "{" + OCR_LOGGING_DATA + "\"image\":\"" + PDF_PASSWORD_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    PDF_NHS_JSON = "{" + OCR_LOGGING_DATA + "\"image\":\"" + PDF_NHS_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    EXPANDED_CROP_JSON = "{" + OCR_LOGGING_DATA + "\"image\":\"" + EXPANDED_CROP_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    EXPANDED_EDGE_JSON = "{" + OCR_LOGGING_DATA + "\"image\":\"" + EXPANDED_EDGE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    HEIC_JSON = "{" + OCR_LOGGING_DATA + "\"image\":\"" + HEIC_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    EXR_JSON = "{" + OCR_LOGGING_DATA + "\"image\":\"" + EXR_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    LARGE_HEIC_JSON = "{" + OCR_LOGGING_DATA + "\"image\":\"" + LARGE_HEIC + "\",\"sessionId\":\"" + SESSION + "\"}";
  }

  @Before
  public void setup() throws IOException, ImagePayloadException, ImageCompressException, CryptoException {
    when(imageCompressor.compressBufferedImage(anyString(), any(BufferedImage.class), eq(3), eq(true))).thenReturn(COMPRESSED_PAGE_LARGE);
    when(imageCompressor.compressBufferedImage(anyString(), any(BufferedImage.class), eq(2), eq(true))).thenReturn(COMPRESSED_PAGE_FINAL);
    when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(3);
    when(controllerConfiguration.getScanTargetImageSizeKb()).thenReturn(3);
    when(controllerConfiguration.getTargetImageSizeKB()).thenReturn(2);
    when(controllerConfiguration.isGreyScale()).thenReturn(true);
    when(controllerConfiguration.getOcrVerticalSlice()).thenReturn(6);

    resourceUnderTest = new FitnoteSubmitResource(controllerConfiguration, validator, ocrChecker, imageStorage, imageCompressor, msDataMatrixCreatorHandler);

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
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(PORTRAIT_FITNOTE_IMAGE);
    imagePayload.setSessionId(SESSION);
    createAndValidateImage(PORTRAIT_JSON, true, imagePayload);
    Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
    verify(validator).validateAndTranslateSubmission(PORTRAIT_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("PASS_IMG_SIZE");
  }

  @Test
  public void failsWhenThereIsNotEnoughMemoryForPhoto() {
    when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(OVER_MAX_MEMORY);

    Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_SERVICE_UNAVAILABLE)));
  }

  @Test
  public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedLargeJpg() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(LARGE_JPG_IMAGE);
    createAndValidateImage(LARGE_VALID_JSON, true, imagePayload);
    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class)))
            .thenReturn(new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null));

    Response response = resourceUnderTest.submitFitnote(LARGE_VALID_JSON);
    verify(validator).validateAndTranslateSubmission(LARGE_VALID_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(2)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void jsonIsPassedIntoServiceWithOcrEnabledUsingOverlay200() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException, ParseException {
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(LARGE_JPG_IMAGE);
    createAndValidateImage(LARGE_VALID_JSON, true, imagePayload);

    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class)))
            .thenReturn(new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null));
    DataMatrixResult dataMatrixResult = new DataMatrixResult();
    dataMatrixResult.setFinalImage(DATA_MATRIX_IMAGE);
    dataMatrixResult.setPosition(new Point(100,100));
    when(msDataMatrixCreatorHandler.generateBase64DataMatrixFromImage(anyString(), anyString(),
          anyBoolean())).thenReturn(dataMatrixResult);

    Response response = resourceUnderTest.submitFitnote(LARGE_VALID_JSON);
    verify(validator).validateAndTranslateSubmission(LARGE_VALID_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(2)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedHeic() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(HEIC_IMAGE);
    createAndValidateImage(HEIC_JSON, true, imagePayload);
    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class)))
            .thenReturn(new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null));

    Response response = resourceUnderTest.submitFitnote(HEIC_JSON);
    verify(validator).validateAndTranslateSubmission(HEIC_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(2)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
  }


  @Test
  public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedJpg() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
    createAndValidateImage(VALID_JSON, true, imagePayload);
    ExpectedFitnoteFormat fitnoteFormat = new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null);
    fitnoteFormat.setMatchAngle(90);
    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class)))
            .thenReturn(fitnoteFormat);

    Response response = resourceUnderTest.submitFitnote(VALID_JSON);
    verify(validator).validateAndTranslateSubmission(VALID_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(2)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
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

    verify(imageCompressor, times(2)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedNhsPdfStyle() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
    when(controllerConfiguration.getPdfScanDPI()).thenReturn(300);

    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(PDF_NHS_FITNOTE_IMAGE);
    createAndValidateImage(PDF_NHS_JSON, true, imagePayload);
    ExpectedFitnoteFormat fitnoteFormat = new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null);
    BufferedImage image = ImageIO.read( new ByteArrayInputStream(NHS_FINAL));
    // enable test to match NHS logic due to using jpg v PDF conversion
    BufferedImage nhsImage =
        image.getSubimage(0, 0, image.getWidth(), image.getHeight() / 4);
    fitnoteFormat.setFinalImage(nhsImage);


    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class)))
            .thenReturn(fitnoteFormat);
    Response response = resourceUnderTest.submitFitnote(PDF_NHS_JSON);
    verify(validator).validateAndTranslateSubmission(PDF_NHS_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(2)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedExpandedCropStyle() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    when(imageCompressor.compressBufferedImage(anyString(), any(BufferedImage.class), eq(2), eq(true))).thenReturn(EXPANDED_CROP_FINAL);
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);

    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(EXPANDED_CROP_IMAGE);
    createAndValidateImage(EXPANDED_CROP_JSON, true, imagePayload);
    ExpectedFitnoteFormat fitnoteFormat = new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null);
    fitnoteFormat.setTopHeight(200);
    fitnoteFormat.setBottomHeight(950);
    BufferedImage image = ImageIO.read( new ByteArrayInputStream(EXPANDED_CROP_FINAL));
    fitnoteFormat.setFinalImage(image);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
            (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, stringToMatch);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, stringToMatch);
    stringToMatch.setupPercentage(61);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, stringToMatch);


    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class)))
            .thenReturn(fitnoteFormat);
    Response response = resourceUnderTest.submitFitnote(EXPANDED_CROP_JSON);
    verify(validator).validateAndTranslateSubmission(EXPANDED_CROP_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(2)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedExpandedCropStyleAlt() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    when(imageCompressor.compressBufferedImage(anyString(), any(BufferedImage.class), eq(2), eq(true))).thenReturn(EXPANDED_CROP_FINAL);
    when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);

    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(EXPANDED_CROP_IMAGE);
    createAndValidateImage(EXPANDED_CROP_JSON, true, imagePayload);
    ExpectedFitnoteFormat fitnoteFormat = new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null);
    fitnoteFormat.setTopHeight(200);
    fitnoteFormat.setBottomHeight(950);
    BufferedImage image = ImageIO.read( new ByteArrayInputStream(EXPANDED_CROP_FINAL));
    fitnoteFormat.setFinalImage(image);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, new StringToMatch(null));
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, stringToMatch);
    stringToMatch.setupPercentage(61);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, stringToMatch);


    when(ocrChecker.imageContainsReadableText(any(ImagePayload.class)))
        .thenReturn(fitnoteFormat);
    Response response = resourceUnderTest.submitFitnote(EXPANDED_CROP_JSON);
    verify(validator).validateAndTranslateSubmission(EXPANDED_CROP_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("SUCCEEDED");

    verify(imageCompressor, times(2)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
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

    verify(imageCompressor, times(1)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
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
    when(imageCompressor.compressBufferedImage(anyString(), any(BufferedImage.class), any(int.class), eq(true))).thenReturn(null);
    ImagePayload imagePayload = new ImagePayload();
    imagePayload.setImage(getEncodedImage(COMPRESSED_PAGE_LARGE));
    imagePayload.setSessionId(SESSION);
    createAndValidateImage(VALID_JSON, true, imagePayload);
    Response response = resourceUnderTest.submitFitnote(VALID_JSON);
    assertThat(response.getStatus(), is(SC_ACCEPTED));

    examineImageStatusResponseForValueOrTimeout("FAILED_IMG_COMPRESS");
  }

  @Test
  public void failImageCompressionAsScaleTooLarge() throws ImagePayloadException, IOException, InterruptedException {
    ImagePayload imagePayload = new ImagePayload();
    imagePayload.setImage(LARGE_HEIC);
    imagePayload.setSessionId(SESSION);
    createAndValidateImage(LARGE_HEIC_JSON, true, imagePayload);
    Response response = resourceUnderTest.submitFitnote(LARGE_HEIC_JSON);
    assertThat(response.getStatus(), is(SC_ACCEPTED));

    examineImageStatusResponseForValueOrTimeout("FAILED_IMG_COMPRESS");
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

    verify(imageCompressor, times(1)).compressBufferedImage(anyString(), any(BufferedImage.class), anyInt(), anyBoolean());
  }

  @Test
  public void failedImagePersistCausesInternalServiceException() throws IOException, CryptoException, ImagePayloadException, InterruptedException {
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(getEncodedImage(COMPRESSED_PAGE_LARGE));

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
    imagePayload.setImage(getEncodedImage(COMPRESSED_PAGE_LARGE));
    when(validator.validateAndTranslateSubmission(any(String.class))).thenReturn(imagePayload);
    doThrow(new ImageHashException("")).when(imageStorage).updateImageHashStore(imagePayload);
    Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
    String entity = (String) response.getEntity();
    assertThat(entity, is(equalTo("{\"sessionId\":\"session1\"}")));
  }

  @Test
  public void failedImageType() throws IOException, CryptoException, ImagePayloadException, InterruptedException {
    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(EXR_IMAGE);

    when(validator.validateAndTranslateSubmission(EXR_JSON)).thenReturn(imagePayload);

    Response response = resourceUnderTest.submitFitnote(EXR_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

    examineImageStatusResponseForValueOrTimeout("FAILED_IMG_FILE_TYPE");
  }

  @Test
  public void testPasswordPdfError() throws CryptoException, ImagePayloadException, IOException, InterruptedException {
    when(controllerConfiguration.getPdfScanDPI()).thenReturn(300);

    ImagePayload imagePayload = imageStorage.getPayload(SESSION);
    imagePayload.setImage(PDF_PASSWORD_FITNOTE_IMAGE);
    createAndValidateImage(PDF_PASSWORD_JSON, true, imagePayload);

    Response response = resourceUnderTest.submitFitnote(PDF_PASSWORD_JSON);
    verify(validator).validateAndTranslateSubmission(PDF_PASSWORD_JSON);
    assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
    verifyNoMoreInteractions(ocrChecker);

    examineImageStatusResponseForValueOrTimeout("FAILED_IMG_PASSWORD");
  }

  private void examineImageStatusResponseForValueOrTimeout(String expectedStatus) throws InterruptedException {
    TimeUnit.SECONDS.sleep(1); // pause before first execution to allow for async processes to begin/end
    PollingWait wait = new PollingWait().timeoutAfter(59, SECONDS).pollEvery(1, SECONDS);

    wait.until(new RunnableAssert("checking /imagestatus for current session") {
      @Override
      public void run() throws Exception {
        Response response = resourceUnderTest.checkFitnote(SESSION_ID);
        assertEquals(expectedStatus, decodeResponse(response.getEntity().toString()).getFitnoteStatus());
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

  private static String getEncodedImage(String imageFileName) throws IOException {
    File file = new File(imageFileName);
    return Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
  }

  private String getEncodedImage(byte[] imageAsBytes)  {
    return Base64.encodeBase64String(imageAsBytes);
  }
}
