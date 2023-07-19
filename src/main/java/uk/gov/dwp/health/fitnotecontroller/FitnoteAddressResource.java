package uk.gov.dwp.health.fitnotecontroller;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.NewAddressException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;

@Path("/")
public class FitnoteAddressResource extends AbstractResource {
  private static final Logger LOG = LoggerFactory.getLogger(FitnoteAddressResource.class.getName());

  public FitnoteAddressResource(ImageStorage imageStore) {
    super(imageStore);
  }

  public FitnoteAddressResource(ImageStorage imageStore, JsonValidator jsonValidator) {
    super(imageStore, jsonValidator);
  }


  @POST
  @Path("/address")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateAddress(String jsonBody) {
    Response serviceResponse;
    try {
      Address receivedAddress = jsonValidator.validateAndTranslateAddress(jsonBody);
      ImagePayload payload = imageStore.getPayload(receivedAddress.getSessionId());
      payload.setClaimantAddress(receivedAddress);
      imageStore.updateAddressDetails(payload);

      serviceResponse = Response.status(HttpStatus.SC_OK).build();

    } catch (NewAddressException e) {
      serviceResponse = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_RESPONSE);
      LOG.error("NewAddressException :: {}", e.getMessage());
      LOG.debug(e.getClass().getName(), e);

    } catch (Exception e) {
      serviceResponse = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_RESPONSE);
      LOG.error("Exception :: {}", e.getMessage());
      LOG.debug(e.getClass().getName(), e);
    }

    return serviceResponse;
  }
}
