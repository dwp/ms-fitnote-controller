package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.Views;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageHashException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageTransformException;
import uk.gov.dwp.health.fitnotecontroller.utils.OcrChecker;
import uk.gov.dwp.health.fitnotecontroller.utils.ImageCompressor;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import uk.gov.dwp.health.fitnotecontroller.utils.MemoryChecker;
import uk.gov.dwp.health.fitnotecontroller.utils.PdfImageExtractor;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

@Path("/")
public class FitnoteSubmitResource extends AbstractResource {
  private static final Logger LOG = LoggerFactory.getLogger(FitnoteSubmitResource.class.getName());
  private static final String LOG_STANDARD_REGEX = "[\\u0000-\\u001f]";
  private FitnoteControllerConfiguration controllerConfiguration;
  private ImageCompressor imageCompressor;
  private OcrChecker ocrChecker;

  public FitnoteSubmitResource(
      FitnoteControllerConfiguration controllerConfiguration, ImageStorage imageStorage) {
    this(
        controllerConfiguration,
        new JsonValidator(),
        new OcrChecker(controllerConfiguration),
        imageStorage,
        new ImageCompressor(controllerConfiguration));
  }

  public FitnoteSubmitResource(
      FitnoteControllerConfiguration controllerConfiguration,
      JsonValidator validator,
      OcrChecker ocrChecker,
      ImageStorage imageStorage,
      ImageCompressor imageCompressor) {
    super(imageStorage, validator);
    this.controllerConfiguration = controllerConfiguration;
    this.ocrChecker = ocrChecker;
    this.imageCompressor = imageCompressor;
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
            jsonValidator.validateAndTranslateSubmission(json.replaceAll(LOG_STANDARD_REGEX, ""));
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
    new Thread(
        () -> {
          try {
            if (!validateAndOcrImageFromInputTypes(payload)) {
              imageStore.updateImageDetails(payload);
              return;
            }

            byte[] compressedImage;
            try (ByteArrayInputStream imageStream =
                     new ByteArrayInputStream(Base64.decodeBase64(payload.getImage()))) {
              compressedImage =
                  imageCompressor.compressBufferedImage(
                      ImageIO.read(imageStream),
                      controllerConfiguration.getTargetImageSizeKB(),
                      controllerConfiguration.isGreyScale());
            }
            errorIfNull(compressedImage);
            setPayloadImageFinal(compressedImage, payload);
            imageStore.updateImageDetails(payload);

          } catch (Exception e) {
            payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_ERROR);
            payload.setImage(null);

            formatAndLogError(e.getClass().getName(), e.getMessage());
            LOG.debug(e.getClass().getName(), e);

            try {
              imageStore.updateImageDetails(payload);

            } catch (ImagePayloadException | IOException | CryptoException e1) {
              formatAndLogError(e1.getClass().getName(), e1.getMessage());
              LOG.debug(e1.getClass().getName(), e1);
            }
          }
        })
        .start();
  }

  private String createSessionOnlyResponseFrom(ImagePayload payload)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    return mapper.writerWithView(Views.SessionOnly.class).writeValueAsString(payload);
  }

  private String createStatusOnlyResponseFrom(
      ImagePayload.Status fitnoteStatus) {
    return String.format(
        "{\"fitnoteStatus\":\"%s\"}", fitnoteStatus);
  }

  private boolean validateAndOcrImageFromInputTypes(ImagePayload payload)
      throws IOException, ImagePayloadException, ImageCompressException {
    boolean validationStatus = false;
    try {
      if (validatePayloadImageJpg(payload)) {
        validationStatus = ocrImage(payload);
      }

    } catch (ImageTransformException e) {
      LOG.debug(e.getClass().getName(), e);

      byte[] pdfImage =
          PdfImageExtractor.extractImage(
              Base64.decodeBase64(payload.getImage()), controllerConfiguration.getPdfScanDPI());
      if (pdfImage != null) {
        try (ByteArrayInputStream pdfStream = new ByteArrayInputStream(pdfImage)) {
          if (validateLandscapeImage(payload, ImageIO.read(pdfStream))) {
            payload.setImage(Base64.encodeBase64String(pdfImage));
            validationStatus = ocrImage(payload);
          }
        }

      } else {
        throw new ImagePayloadException(
            "The encoded string could not be transformed to a BufferedImage");
      }
    }

    return validationStatus;
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

    return validateLandscapeImage(payload, imageBuf);
  }

  private boolean validateLandscapeImage(ImagePayload payload, BufferedImage imageBuf) {
    boolean returnValue = false;
    if (controllerConfiguration.isLandscapeImageEnforced()) {
      if (imageBuf.getHeight() > imageBuf.getWidth()) {
        LOG.error("Image is not landscape (H:{}, W:{})", imageBuf.getHeight(), imageBuf.getWidth());
        payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_SIZE);
      }
    } else {
      payload.setFitnoteCheckStatus(ImagePayload.Status.PASS_IMG_SIZE);
      LOG.info("NO LANDSCAPE ENFORCEMENT FOR IMAGE DIMENSIONS");
      returnValue = true;
    }
    return returnValue;
  }

  private boolean ocrImage(ImagePayload payload) throws IOException, ImageCompressException {
    boolean ocrStatus = false;

    if (!controllerConfiguration.isOcrChecksEnabled()) {
      LOG.info("NO OCR CHECKS OR ROTATION CONFIGURED IMAGES");
      ocrStatus = true;

    } else {
      byte[] compressedImage;
      try (ByteArrayInputStream imageStream =
               new ByteArrayInputStream(Base64.decodeBase64(payload.getImage()))) {
        compressedImage =
            imageCompressor.compressBufferedImage(
                ImageIO.read(imageStream),
                controllerConfiguration.getScanTargetImageSizeKb(),
                false);
      }

      errorIfNull(compressedImage);
      setPayloadImage(compressedImage, payload);

      ExpectedFitnoteFormat expectedFitnoteFormat = ocrChecker.imageContainsReadableText(payload);
      ExpectedFitnoteFormat.Status imageStatus = expectedFitnoteFormat.getStatus();
      if (imageStatus.equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
        payload.setFitnoteCheckStatus(ImagePayload.Status.PASS_IMG_OCR);
        ocrStatus = true;

      } else if (imageStatus.equals(ExpectedFitnoteFormat.Status.PARTIAL)) {
        payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_OCR_PARTIAL);
        payload.setImage(null);

      } else if (imageStatus.equals(ExpectedFitnoteFormat.Status.FAILED)
          || imageStatus.equals(ExpectedFitnoteFormat.Status.INITIALISED)) {
        payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_OCR);
        payload.setImage(null);

        LOG.warn("Unable to OCR the fitnote");
      }
    }

    return ocrStatus;
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
}
