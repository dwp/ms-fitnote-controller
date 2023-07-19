package uk.gov.dwp.health.fitnotecontroller;

import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;

import java.io.IOException;


public abstract class AbstractResource {

  static final String ERROR_RESPONSE = "Unable to process request";
  private static final String CRYPTO_ERROR = "CryptoException :: {}";
  private static final String UNABLE_ENCRYPT = "Unable to encrypt payload";
  private static final String IMAGE_ERROR = "ImagePayloadException :: {}";
  private static final String IO_ERROR = "IOException :: {}";


  JsonValidator jsonValidator;
  ImageStorage imageStore;

  public AbstractResource(ImageStorage imageStore) {
    this(imageStore, new JsonValidator());
  }

  public AbstractResource(ImageStorage imageStore, JsonValidator jsonValidator) {
    this.jsonValidator = jsonValidator;
    this.imageStore = imageStore;
  }

  Response createResponseOf(int status, String message) {
    return Response.status(status).entity(message).build();
  }

  Response createImage400ErrorResponse(ImagePayloadException e, Logger log) {
    log.error(IMAGE_ERROR, e.getMessage());
    log.debug(ERROR_RESPONSE, e);
    return createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_RESPONSE);

  }

  Response createImage500ErrorResponse(ImagePayloadException e, Logger log) {
    log.error(IMAGE_ERROR, e.getMessage());
    log.debug(ERROR_RESPONSE, e);
    return createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_RESPONSE);
  }

  Response createIOErrorResponse(IOException e, Logger log) {
    log.error(IO_ERROR, e.getMessage());
    log.debug(ERROR_RESPONSE, e);
    return createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_RESPONSE);
  }

  Response createCrypto400Response(CryptoException e, Logger log) {
    log.debug(UNABLE_ENCRYPT, e);
    log.error(CRYPTO_ERROR, e.getMessage());
    return createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_RESPONSE);
  }

  Response createCrypto500Response(CryptoException e, Logger log) {
    log.debug(UNABLE_ENCRYPT, e);
    log.error(CRYPTO_ERROR, e.getMessage());
    return createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_RESPONSE);
  }
}
