package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.im4java.core.IM4JavaException;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.DataMatrixResult;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.Views;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageHashException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageTransformException;
import uk.gov.dwp.health.fitnotecontroller.handlers.MsDataMatrixCreatorHandler;
import uk.gov.dwp.health.fitnotecontroller.utils.ImageUtils;
import uk.gov.dwp.health.fitnotecontroller.utils.OcrChecker;
import uk.gov.dwp.health.fitnotecontroller.utils.ImageCompressor;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import uk.gov.dwp.health.fitnotecontroller.utils.MemoryChecker;
import uk.gov.dwp.health.fitnotecontroller.utils.PdfImageExtractor;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat.Status.FAILED;
import static uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat.Status.SUCCESS;
import static uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload.Status.PASS_IMG_OCR;
import static uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload.Status.valueOf;

@Path("/")
public class FitnoteSubmitResource extends AbstractResource {
  private static final Logger LOG = LoggerFactory.getLogger(FitnoteSubmitResource.class.getName());
  private static final String LOG_STANDARD_REGEX = "[\\u0000-\\u001f]";
  private FitnoteControllerConfiguration controllerConfiguration;
  private ImageCompressor imageCompressor;
  private OcrChecker ocrChecker;
  private MsDataMatrixCreatorHandler msDataMatrixCreatorHandler;
  private List acceptedTypes;

  public FitnoteSubmitResource(
          FitnoteControllerConfiguration controllerConfiguration, ImageStorage imageStorage,
          MsDataMatrixCreatorHandler msDataMatrixCreatorHandler) {
    this(
            controllerConfiguration,
            new JsonValidator(controllerConfiguration),
            new OcrChecker(controllerConfiguration),
            imageStorage,
            new ImageCompressor(controllerConfiguration),
            msDataMatrixCreatorHandler);
  }

  public FitnoteSubmitResource(
          FitnoteControllerConfiguration controllerConfiguration,
          JsonValidator validator,
          OcrChecker ocrChecker,
          ImageStorage imageStorage,
          ImageCompressor imageCompressor,
          MsDataMatrixCreatorHandler msDataMatrixCreatorHandler) {
    super(imageStorage, validator);
    this.controllerConfiguration = controllerConfiguration;
    this.ocrChecker = ocrChecker;
    this.imageCompressor = imageCompressor;
    this.acceptedTypes = getAcceptedTypes();
    this.msDataMatrixCreatorHandler = msDataMatrixCreatorHandler;
  }

  @GET
  @Path("/imagestatus")
  @Produces(MediaType.APPLICATION_JSON)
  public Response checkFitnote(@QueryParam("sessionId") Optional<String> sessionId)
          throws ImagePayloadException, IOException, CryptoException {
    Response response;
    if (sessionId.isPresent()) {
      String formatted = sessionId.get().replaceAll(LOG_STANDARD_REGEX, "");
      ImagePayload payload = imageStore.getPayload(formatted);

      response =
              Response.status(Response.Status.OK)
                      .entity(
                              createStatusOnlyResponseFrom(
                                      payload.getFitnoteCheckStatus()))
                      .build();
    } else {
      response = Response.status(Response.Status.BAD_REQUEST).build();
    }
    return response;
  }

  @GET
  @Path("/extendSession")
  public Response extendSession(@QueryParam("sessionId") Optional<String> sessionId)
          throws ImagePayloadException, IOException, CryptoException {
    Response response;
    if (sessionId.isPresent()) {
      String formatted = sessionId.get().replaceAll(LOG_STANDARD_REGEX, "");
      imageStore.extendSessionTimeout(formatted);

      response = Response.status(Response.Status.OK).build();
    } else {
      response = Response.status(Response.Status.BAD_REQUEST).build();
    }
    return response;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/photo")
  public Response submitFitnote(String json) {
    ImagePayload incomingPayload = null;
    Response response;
    try {

      if (MemoryChecker.hasEnoughMemoryForRequest(
              Runtime.getRuntime(), controllerConfiguration.getEstimatedRequestMemoryMb())) {
        incomingPayload =
                jsonValidator.validateAndTranslateSubmission(json.replaceAll(LOG_STANDARD_REGEX,
                        ""));
        LOG.info("Processing image for {} ", incomingPayload.getLogMessage());
        imageStore.updateImageHashStore(incomingPayload);
        LOG.info("Updated image hashstore for {}", incomingPayload.getLogMessage());

        ImagePayload storedPayload = imageStore.getPayload(incomingPayload.getSessionId());
        LOG.info("Retrieved image payload for {}", incomingPayload.getLogMessage());
        storedPayload.setFitnoteCheckStatus(incomingPayload.getFitnoteCheckStatus());
        storedPayload.setImage(incomingPayload.getImage());
        imageStore.updateImageDetails(storedPayload);
        LOG.info("Updated image details for {}", incomingPayload.getLogMessage());

        response =
                createResponseOf(
                        HttpStatus.SC_ACCEPTED, createSessionOnlyResponseFrom(incomingPayload));
        LOG.info("Sent Successful response for {}", incomingPayload.getLogMessage());
        LOG.debug("Json Validated correctly");
        checkAsynchronously(storedPayload);

      } else {
        response = createResponseOf(HttpStatus.SC_SERVICE_UNAVAILABLE, ERROR_RESPONSE);
        LOG.error("Java out of memory, current free memory {}",
                MemoryChecker.returnCurrentAvailableMemoryInMb(Runtime.getRuntime()));
      }

    }  catch (ImageHashException e) {
      response = createResponseOf(HttpStatus.SC_ACCEPTED, incomingPayload);
      LOG.error("Throwing ImageHashException while processing {}",
              getLogMessage(incomingPayload));
      LOG.error("ImageHashException :: {}", e.getMessage());
    } catch (IOException e) {
      response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_RESPONSE);
      LOG.error("Throwing IOException while processing {}",
              getLogMessage(incomingPayload));
      LOG.error("IOException :: {}", e.getMessage());
      LOG.debug(ERROR_RESPONSE, e);
    } catch (CryptoException e) {
      response = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_RESPONSE);
      LOG.error("Throwing CryptoException while processing {}",
              getLogMessage(incomingPayload));
      formatAndLogError(e.getClass().getName(), e.getMessage());
      LOG.debug(ERROR_RESPONSE, e);
    } catch (ImagePayloadException  e) {
      response = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_RESPONSE);
      LOG.error("Throwing ImagePayloadException while processing {}",
              getLogMessage(incomingPayload));
      formatAndLogError(e.getClass().getName(), e.getMessage());
      LOG.debug(ERROR_RESPONSE, e);
    }


    LOG.debug("Completed /photo, send back status {} while "
                    + "processing {}", response.getStatusInfo().getStatusCode(),
            getLogMessage(incomingPayload));
    return response;
  }

  private Response createResponseOf(int status, ImagePayload incomingPayload) {
    Response response = null;
    try {
      response = createResponseOf(
              status, createSessionOnlyResponseFrom(incomingPayload));
    } catch (JsonProcessingException ex) {
      LOG.error("JsonProcessingException :: {}", ex.getMessage());
    }
    return response;
  }

  private void formatAndLogError(String className, String message) {
    LOG.error("{} {}", className, message);
  }

  private void checkAsynchronously(ImagePayload payload) {
    payload.setFitnoteCheckStatus(ImagePayload.Status.CHECKING);
    final long startTime = System.currentTimeMillis();
    new Thread(
            () -> {
              try {
                String fileMimeType = getFileTypeAndLogFileInfo(payload);
                if (fileMimeType == null || !acceptedTypes.contains(fileMimeType)) {
                  setErrorPayload(ImagePayload.Status.FAILED_IMG_FILE_TYPE, payload, null,
                          startTime);
                  return;
                }
                byte[] origImage = Base64.decodeBase64(payload.getImage());
                final boolean isPdf = fileMimeType.equals("pdf");
                byte[] convertedImg = convertAndCompressImage(payload, fileMimeType);
                fileMimeType = "jpg";
                if (convertedImg.length != origImage.length) {
                  displayLogs(fileMimeType, payload);
                  origImage = convertedImg;
                }

                ExpectedFitnoteFormat expectedFitnoteFormat =
                        validateAndOcrImageFromInputTypes(payload, fileMimeType);
                if (!expectedFitnoteFormat.getStatus().equals(SUCCESS)) {
                  imageStore.updateImageDetails(payload);
                  logTimeTaken(startTime, payload.getFitnoteCheckStatus());
                  return;
                }

                byte[] compressedImage;
                try (ByteArrayInputStream imageStream =
                             new ByteArrayInputStream(Base64.decodeBase64(payload.getImage()))) {
                  compressedImage =
                          imageCompressor.compressBufferedImage(fileMimeType,
                                  ImageIO.read(imageStream),
                                  controllerConfiguration.getTargetImageSizeKB(),
                                  controllerConfiguration.isGreyScale());
                }
                errorIfNull(compressedImage);
                byte[] finalImage = improveDMRegion(compressedImage, origImage,
                        expectedFitnoteFormat.getMatchAngle(), payload.getSessionId(), isPdf);
                setPayloadImageFinal(finalImage, payload);
                imageStore.updateImageDetails(payload);
                logTimeTaken(startTime, payload.getFitnoteCheckStatus());
              } catch (ImageCompressException e) {
                setErrorPayload(ImagePayload.Status.FAILED_IMG_COMPRESS, payload, e, startTime);
              } catch (ImageTransformException e) {
                setErrorPayload(ImagePayload.Status.FAILED_IMG_FILE_TYPE, payload, e, startTime);
              } catch (Exception e) {
                setErrorPayload(ImagePayload.Status.FAILED_ERROR, payload, e, startTime);
              }
            })
            .start();
  }

  private byte[] convertAndCompressImage(ImagePayload payload, String fileMimeType) throws
          ImageTransformException, IOException, ImagePayloadException, InterruptedException,
          IM4JavaException, ImageCompressException {
    byte[] origImage = Base64.decodeBase64(payload.getImage());
    int megapixel = ImageUtils.calculateMegapixel(origImage);
    LOG.info("Megapixel count {}", megapixel);
    if (megapixel > 250) {
      LOG.error("Megapixel error {}", megapixel);
      throw new ImageCompressException(
              "The megapixel count is too high" + megapixel);
    }
    if (fileMimeType.equals("pdf")) {
      convertPdf(payload);
      origImage = Base64.decodeBase64(payload.getImage());
    } else if (!fileMimeType.equals("jpg")) {
      // convert all non jpg to jpg
      byte[] convertedImg = ImageUtils.convertImage(origImage, 100d);
      if (convertedImg == null) {
        LOG.error("Large scale error");
        LOG.error("Megapixel error {}", megapixel);
        throw new ImageCompressException(
                "The encoded string could not be converted from " + fileMimeType + " to a jpg");
      }
      setPayloadImage(convertedImg, payload);
      origImage = convertedImg;
    }

    return origImage;
  }

  private void logTimeTaken(long startTime, ImagePayload.Status imagePayloadStatus) {
    LOG.info("Time taken to process image {} = (seconds) {}", imagePayloadStatus,
            (System.currentTimeMillis() - startTime) / 1000);
  }

  private String getFileTypeAndLogFileInfo(ImagePayload payload) {
    String fileMimeType = ImageUtils.getImageMimeType(payload);
    int imageSize = ImageUtils.getImageLength(payload);
    LOG.info("file type is " + fileMimeType
            + ", sessionId is " + payload.getSessionId());
    LOG.info("initial file size is = {}", getFileSize(imageSize)
            + ", sessionId is " + payload.getSessionId());
    return fileMimeType;
  }

  private String getFileSize(int imageSize) {
    long kb = 1024;
    long mb = kb * 1024;
    BigDecimal mbSize = BigDecimal.valueOf(imageSize).divide(BigDecimal.valueOf(mb));
    if (mbSize.intValue() > 0) {
      return mbSize.setScale(1, RoundingMode.HALF_EVEN).doubleValue() + " MB";
    } else {
      return FileUtils.byteCountToDisplaySize(imageSize);
    }
  }

  private byte[] improveDMRegion(byte[] compressedImage, byte[] origImage,
                                 int angle, String sessionId, boolean isPdf) {
    byte[] image = compressedImage;
    final long startTime = System.currentTimeMillis();
    LOG.info("Before layer dm region file size (bytes) = {}", compressedImage.length);
    image = overlayDataMatrix(origImage, sessionId, compressedImage, angle, isPdf);
    if (image == null) {
      LOG.info("Failed overlayDataMatrix, will attempt layerDMRegionOnImage");
      image = ImageUtils.layerDMRegionOnImage(compressedImage, origImage, angle);
    }
    LOG.info("After improve dm region file size (bytes) = {}", image.length);
    LOG.info("Time taken to improve DM region = seconds {}",
            (System.currentTimeMillis() - startTime) / 1000);
    return image;
  }

  private byte[] overlayDataMatrix(byte[] origImage, String sessionId, byte[] compressedImage,
                                   int angle, boolean isPdf) {
    DataMatrixResult dataMatrixResult = null;
    try {
      byte[] rotatedImage = origImage;
      if (angle > 0) {
        rotatedImage = ImageUtils.createRotatedCopy(origImage, angle);
      }
      String base64Img = Base64.encodeBase64String(rotatedImage);
      dataMatrixResult = msDataMatrixCreatorHandler.generateBase64DataMatrixFromImage(sessionId,
              base64Img, isPdf);
    } catch (IOException e) {
      LOG.info("Failed to retrieve data matrix");
      LOG.error("IOException :: {}", e.getMessage());
      return null;
    }
    if (dataMatrixResult == null) {
      LOG.info("Failed to retrieve data matrix");
      return null;
    }
    LOG.info("Retrieved data matrix");
    return ImageUtils.placeDMOnImage(compressedImage, dataMatrixResult, isPdf);
  }

  private void displayLogs(String fileMimeType, ImagePayload payload) {
    int imageSize = ImageUtils.getImageLength(payload);
    LOG.info("new file size (bytes) = {}", imageSize);
    LOG.info("new file mime type is " + fileMimeType);
  }

  private String createSessionOnlyResponseFrom(ImagePayload payload)
          throws JsonProcessingException {
    ObjectMapper mapper = JsonMapper
            .builder()
            .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
            .build();
    return mapper.writerWithView(Views.SessionOnly.class).writeValueAsString(payload);
  }

  private String createStatusOnlyResponseFrom(
          ImagePayload.Status fitnoteStatus) {
    return String.format(
            "{\"fitnoteStatus\":\"%s\"}", fitnoteStatus);
  }

  private ExpectedFitnoteFormat validateAndOcrImageFromInputTypes(ImagePayload payload,
                                                                  String fileMimeType)
          throws IOException, ImagePayloadException, ImageCompressException,
          ImageTransformException {
    ExpectedFitnoteFormat fitnoteFormat = null;
    try {
      if (validatePayloadImageJpg(payload)) {
        fitnoteFormat = ocrImage(payload, fileMimeType);
      } else {
        fitnoteFormat = new ExpectedFitnoteFormat(FAILED, "Failed: Image is not landscape");
      }

    } catch (ImageTransformException e) {
      LOG.debug(e.getClass().getName(), e);

      throw new ImageTransformException(
              "The encoded string could not be transformed to a BufferedImage");
    }

    return fitnoteFormat;
  }

  private boolean validatePayloadImageJpg(ImagePayload payload)
          throws IOException, ImagePayloadException, ImageTransformException {
    if (payload.getImage() == null) {
      throw new ImagePayloadException(
              "The encoded string is null.  Cannot be transformed to an image");
    }

    BufferedImage imageBuf;
    try (ByteArrayInputStream imageStream =
                 new ByteArrayInputStream(Base64.decodeBase64(payload.getImage()))) {
      imageBuf = ImageIO.read(imageStream);
    }

    if (imageBuf == null) {
      throw new ImageTransformException(
              "The encoded string could not be transformed to a BufferedImage");
    }

    payload.setFitnoteCheckStatus(ImagePayload.Status.PASS_IMG_SIZE);
    LOG.info("NO LANDSCAPE ENFORCEMENT FOR IMAGE DIMENSIONS");
    return true;
  }

  private ExpectedFitnoteFormat ocrImage(ImagePayload payload, String fileMimeType)
          throws IOException, ImageCompressException {

    if (!controllerConfiguration.isOcrChecksEnabled()) {
      LOG.info("NO OCR CHECKS OR ROTATION CONFIGURED IMAGES");
      ExpectedFitnoteFormat expectedFitnoteFormat =  new ExpectedFitnoteFormat(SUCCESS, null);
      return expectedFitnoteFormat;

    } else {
      byte[] compressedImage;
      try (ByteArrayInputStream imageStream =
                   new ByteArrayInputStream(Base64.decodeBase64(payload.getImage()))) {
        compressedImage =
                imageCompressor.compressBufferedImage(fileMimeType,
                        ImageIO.read(imageStream),
                        controllerConfiguration.getScanTargetImageSizeKb(),
                        false);
      }

      errorIfNull(compressedImage);
      setPayloadImage(compressedImage, payload);

      ExpectedFitnoteFormat expectedFitnoteFormat = ocrChecker.imageContainsReadableText(payload);
      ExpectedFitnoteFormat.Status imageStatus = expectedFitnoteFormat.getStatus();
      if (imageStatus.equals(SUCCESS)) {
        payload.setFitnoteCheckStatus(PASS_IMG_OCR);

      } else if (imageStatus.equals(ExpectedFitnoteFormat.Status.PARTIAL)) {
        payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_OCR_PARTIAL);
        payload.setImage(null);

      } else if (imageStatus.equals(FAILED)
              || imageStatus.equals(ExpectedFitnoteFormat.Status.INITIALISED)) {
        payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_OCR);
        payload.setImage(null);

        LOG.warn("Unable to OCR the fitnote");
      }
      return expectedFitnoteFormat;
    }

  }


  private void setPayloadImage(byte[] compressedImage, ImagePayload payload) {
    LOG.info(
            "Written {} bytes back to the ImagePayload for {} - OCR Checks",
            compressedImage.length,
            payload.getSessionId());
    payload.setImage(Base64.encodeBase64String(compressedImage));
  }

  private void setPayloadImageFinal(byte[] compressedImage, ImagePayload payload) {
    LOG.info(
            "Written {} bytes back to the ImagePayload for {} - FINAL Submission",
            compressedImage.length,
            payload.getSessionId());
    payload.setFitnoteCheckStatus(ImagePayload.Status.SUCCEEDED);
    payload.setImage(Base64.encodeBase64String(compressedImage));
  }

  private void errorIfNull(byte[] compressedImage) throws ImageCompressException {
    if (compressedImage == null) {
      throw new ImageCompressException("The compressed image return a null byte array");
    }
  }

  private String getLogMessage(ImagePayload incomingPayload) {
    return (incomingPayload != null ? incomingPayload.getLogMessage() : "ImagePayload is null");
  }

  private List<String> getAcceptedTypes() {
    ArrayList<String> acceptedTypes = new ArrayList<>();
    acceptedTypes.addAll(Arrays.asList(ImageIO.getReaderFileSuffixes()));
    acceptedTypes.add("pdf");
    acceptedTypes.add("heic");
    acceptedTypes.add("heif");
    return acceptedTypes;
  }

  private void setErrorPayload(ImagePayload.Status imagePayloadStatus, ImagePayload payload,
                               Exception e, long startTime) {
    logTimeTaken(startTime, imagePayloadStatus);
    payload.setFitnoteCheckStatus(imagePayloadStatus);
    payload.setImage(null);

    if (e != null) {
      formatAndLogError(e.getClass().getName(), e.getMessage());
      LOG.debug(e.getClass().getName(), e);
    }

    try {
      imageStore.updateImageDetails(payload);

    } catch (ImagePayloadException | IOException | CryptoException e1) {
      formatAndLogError(e1.getClass().getName(), e1.getMessage());
      LOG.debug(e1.getClass().getName(), e1);
    }
  }

  private void convertPdf(ImagePayload payload)
          throws IOException, ImageTransformException, ImagePayloadException {
    byte[] pdfImage =
            PdfImageExtractor.extractImage(
                    Base64.decodeBase64(payload.getImage()),
                    controllerConfiguration.getPdfScanDPI());
    if (pdfImage != null) {
      try (ByteArrayInputStream pdfStream = new ByteArrayInputStream(pdfImage)) {
        payload.setImage(Base64.encodeBase64String(pdfImage));
      }

    } else {
      throw new ImageTransformException(
              "The encoded string could not be transformed to a BufferedImage");
    }
  }

}
