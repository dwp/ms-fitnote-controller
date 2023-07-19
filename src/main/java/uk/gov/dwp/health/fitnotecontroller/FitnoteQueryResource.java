package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.Views;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;

import java.io.IOException;

@Path("/")
public class FitnoteQueryResource extends AbstractResource {
  private static final Logger LOG = LoggerFactory.getLogger(FitnoteQueryResource.class.getName());
  private static final String MULTI_ERROR_FORMAT = "Exception :: {}, {}";
  private static final String LOG_STANDARD_REGEX = "[\\u0000-\\u001f]";

  public FitnoteQueryResource(ImageStorage imageStore) {
    super(imageStore);
  }

  enum QueryType {
    NINO, ADDRESS, MOBILE
  }

  @POST
  @Path("/queryNino")
  @Produces(MediaType.APPLICATION_JSON)
  public Response queryNino(String json) {
    return queryForResponse(json, QueryType.NINO);
  }

  @POST
  @Path("/queryAddress")
  @Produces(MediaType.APPLICATION_JSON)
  public Response queryAddress(String json) {
    return queryForResponse(json, QueryType.ADDRESS);
  }

  @POST
  @Path("/queryMobile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response queryMobile(String json) {
    return queryForResponse(json, QueryType.MOBILE);
  }

  private Response queryForResponse(String json, QueryType queryType) {
    Response response;
    try {
      ImagePayload payload = imageStore.getPayload(testAndSanitiseSession(json));

      String serialisedClass;

      if (queryType.equals(QueryType.NINO)) {
        payload.setNino(getNino(payload));
        serialisedClass = serialiseSpecificPayloadItems(Views.QueryNinoDetails.class, payload);
      } else if (queryType.equals(QueryType.ADDRESS)) {
        payload.setClaimantAddress(getAddress(payload));
        serialisedClass = serialiseSpecificPayloadItems(Views.QueryAddressDetails.class, payload);
      } else {
        payload.setMobileNumber(getMobile(payload));
        serialisedClass = serialiseSpecificPayloadItems(Views.QueryMobileDetails.class, payload);
      }

      LOG.info("serialised nino query for {}", payload.getSessionId());

      response = createResponseOf(HttpStatus.SC_OK, serialisedClass);

    } catch (IOException | CryptoException e) {
      response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_RESPONSE);
      LOG.error(MULTI_ERROR_FORMAT, e.getClass().getName(), e.getMessage());
      LOG.debug(ERROR_RESPONSE, e);

    } catch (ImagePayloadException e) {
      return createImage400ErrorResponse(e, LOG);
    }
    return response;
  }

  private String getNino(ImagePayload payload) {
    if (payload.getNino() == null) {
      logEmptyString("nino");
      return "";
    }
    return payload.getNino();
  }

  private Address getAddress(ImagePayload payload) {
    if (payload.getClaimantAddress() == null) {
      logEmptyString("claimantAddress");
      return new Address();
    }
    return payload.getClaimantAddress();
  }

  private String getMobile(ImagePayload payload) {
    if (payload.getMobileNumber() == null) {
      logEmptyString("mobileNumber");
      return "";
    }
    return payload.getMobileNumber();
  }

  private void logEmptyString(String field) {
    LOG.debug("null {} value updated to empty string for inclusion to serialisation", field);
  }

  private String testAndSanitiseSession(String json) throws ImagePayloadException, IOException {
    if (json == null || json.isEmpty()) {
      throw new ImagePayloadException("json cannot be empty or null");
    }

    JsonNode jsonNode = new ObjectMapper().readTree(json);
    return jsonNode.get("sessionId") != null
            ? jsonNode.get("sessionId").textValue().replaceAll(LOG_STANDARD_REGEX, "")
            : null;
  }

  private String serialiseSpecificPayloadItems(Class<?> mapperView, ImagePayload payload)
          throws JsonProcessingException {
    ObjectMapper mapper = JsonMapper
            .builder()
            .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
            .build();
    return mapper.writerWithView(mapperView).writeValueAsString(payload);
  }
}
