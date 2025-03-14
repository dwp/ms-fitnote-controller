package uk.gov.dwp.health.fitnotecontroller.handlers;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.DataMatrixResult;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import uk.gov.dwp.tls.TLSConnectionBuilder;
import uk.gov.dwp.tls.TLSGeneralException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class MsDataMatrixCreatorHandler {
  private static final String FULL_JSON = "{\"sessionId\": \"%s\", \"image\": \"%s\", "
          + "\"pdf\": \"%s\"}";
  private static final String LOG_STANDARD_REGEX = "[\\u0000-\\u001f]";
  private static final String ERROR_MSG = "MsDataMatrixCreator Failure with StatusCode %s and "
          + "Message %s";
  private static final Logger LOG =
          LoggerFactory.getLogger(MsDataMatrixCreatorHandler.class.getName());
  private final FitnoteControllerConfiguration configuration;
  private final TLSConnectionBuilder tlsBuilder;
  private JsonValidator jsonValidator;

  public MsDataMatrixCreatorHandler(TLSConnectionBuilder tlsConnectionBuilder,
                                    FitnoteControllerConfiguration config) {
    this.tlsBuilder = tlsConnectionBuilder;
    this.configuration = config;
    this.jsonValidator = new JsonValidator(config);
  }

  public DataMatrixResult generateBase64DataMatrixFromImage(String sessionId, String image,
                                                            boolean isPdf) throws IOException {
    DataMatrixResult dataMatrixResult = null;

    HttpPost postMethod = new HttpPost(configuration.getDataMatrixCreatorServiceUrl());

    LOG.info("Service url {}" + postMethod);
    LOG.info("creating json request with base64 encoded image for session {}",
            sessionId);
    postMethod.setEntity(new StringEntity(String.format(FULL_JSON, sessionId,
        image, isPdf)));

    try {
      CloseableHttpResponse response = getTlsBuilder().configureSSLConnection().execute(postMethod);
      LOG.debug("received {} from {}", response.getCode(),
          configuration.getDataMatrixCreatorServiceUrl());

      if (response.getCode() == HttpStatus.SC_OK) {
        LOG.info("successfully received base64 encoded data matrix image for session {}",
            sessionId);
        String json = EntityUtils.toString(response.getEntity());
        dataMatrixResult =
                jsonValidator.validateAndDataMatrixResult(json.replaceAll(LOG_STANDARD_REGEX,
                        ""));


      } else {
        throw new IOException(String.format(ERROR_MSG, response.getCode(),
            EntityUtils.toString(response.getEntity())));
      }

    } catch (TLSGeneralException
             | NoSuchAlgorithmException
             | KeyManagementException
             | CertificateException
             | KeyStoreException
             | UnrecoverableKeyException
             | ImagePayloadException
             | ParseException
             | ClientProtocolException e) {
      LOG.error(e.getMessage(), e);
    }

    return dataMatrixResult;
  }

  private TLSConnectionBuilder getTlsBuilder() {
    return tlsBuilder;
  }
}
