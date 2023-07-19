package uk.gov.dwp.health.fitnotecontroller.integration;

import com.amazonaws.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.cluster.RedisClusterClient;

import org.junit.Before;
import org.junit.Test;
import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.crypto.CryptoMessage;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.ImageStorage;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageHashException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("squid:S1192") // allow string literals
public class ImageStorageTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final long SYSTEM_TIME_FAKE_VALUE = 0;
  private static final String REDIS_HOST = "redis-cluster";

  @Mock private FitnoteControllerConfiguration configuration;

  @Mock private CryptoDataManager cryptoDataManager;

  private  RedisClusterClient redisClient;

  private ImageStorage instance;


  @Before
  public void setup() throws CryptoException {
    when(configuration.getImageReplayExpirySeconds()).thenReturn(3L);
    when(configuration.getSessionExpiryTimeInSeconds()).thenReturn(3L);
    when(configuration.getMaxAllowedImageReplay()).thenReturn(2);
    when(configuration.getImageHashSalt()).thenReturn("salt");

    CryptoMessage testCryptoResponse = new CryptoMessage();
    testCryptoResponse.setKey(Base64.encodeAsString("i-am-a-key".getBytes()));
    testCryptoResponse.setMessage("xxxx-secret-xxxx");
    testCryptoResponse.setHash("hash-hash");
    testCryptoResponse.setSalt("salty");


    redisClient = RedisClusterClient.create("redis://" + REDIS_HOST + ":7000");
    redisClient.connect().sync().flushall();

    when(cryptoDataManager.encrypt(anyString())).thenReturn(testCryptoResponse);
    when(configuration.isRedisEncryptMessages()).thenReturn(true);

    instance =
            new ImageStorage(configuration, redisClient, cryptoDataManager) {
              @Override
              protected long getCurrentTimeMillis() {
                return SYSTEM_TIME_FAKE_VALUE;
              }
            };
  }

  @Test
  public void imageCanBePersistedAndRetrieved()
          throws ImagePayloadException, IOException, CryptoException {
    String sessionId = UUID.randomUUID().toString();

    ImagePayload testObject = new ImagePayload();
    testObject.setExpiryTime(configuration.getSessionExpiryTimeInSeconds());
    testObject.setSessionId(sessionId);

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(testObject));
    instance.getPayload(sessionId);

    assertThat(
            instance.getPayload(sessionId).getExpiryTime(),
            is(equalTo(configuration.getSessionExpiryTimeInSeconds())));
    assertThat(instance.getPayload(sessionId).getSessionId(), is(equalTo((sessionId))));
  }

  @Test
  public void invalidSessionIdsReturnNullImagePayload() throws IOException, CryptoException {
    try {
      instance.getPayload(null);
      fail("should fail with null payload");

    } catch (ImagePayloadException e) {
      assertThat(e.getMessage(), containsString("Null sessionId"));
    }
  }

  @Test
  public void updateNullImagePayload() throws IOException, CryptoException {
    try {
      instance.updateNinoDetails(null);
      fail("cannot update a null payload");

    } catch (ImagePayloadException e) {
      assertThat(e.getMessage(), containsString("Null payload"));
    }
  }

  @Test
  public void updateUnknownPayloadENCRYPTED()
          throws ImagePayloadException, IOException, CryptoException {
    when(configuration.isRedisEncryptMessages()).thenReturn(true);
    ImagePayload payload = new ImagePayload();
    payload.setSessionId(UUID.randomUUID().toString());

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(payload));

    instance.updateNinoDetails(payload);

    assertThat(
            instance.getPayload(payload.getSessionId()).getSessionId(),
            is(equalTo(payload.getSessionId())));
  }

  @Test
  public void updateUnknownPayloadPLAIN()
          throws ImagePayloadException, IOException, CryptoException {
    when(configuration.isRedisEncryptMessages()).thenReturn(false);
    ImagePayload payload = new ImagePayload();
    payload.setSessionId(UUID.randomUUID().toString());
    instance.updateNinoDetails(payload);

    assertThat(
            instance.getPayload(payload.getSessionId()).getSessionId(),
            is(equalTo(payload.getSessionId())));
  }

  @Test
  public void updateNinoToStoredImageENCRYPTED()
          throws ImagePayloadException, IOException, CryptoException {
    String sessionId = UUID.randomUUID().toString();
    String nino = "AA370773A";

    ImagePayload newPayloadWithSameSessionId = new ImagePayload();
    newPayloadWithSameSessionId.setSessionId(sessionId);
    newPayloadWithSameSessionId.setNino(nino);

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(newPayloadWithSameSessionId));

    instance.getPayload(sessionId);
    instance.updateNinoDetails(newPayloadWithSameSessionId);

    assertNotNull(instance.getPayload(sessionId).getNino());
    assertThat(instance.getPayload(sessionId).getNino(), is(equalTo(nino)));
  }

  @Test
  public void updateNinoToStoredImagePLAIN()
          throws ImagePayloadException, IOException, CryptoException {
    when(configuration.isRedisEncryptMessages()).thenReturn(false);
    String sessionId = UUID.randomUUID().toString();
    String nino = "AA370773A";

    ImagePayload newPayloadWithSameSessionId = new ImagePayload();
    newPayloadWithSameSessionId.setSessionId(sessionId);
    newPayloadWithSameSessionId.setNino(nino);

    instance.getPayload(sessionId);
    instance.updateNinoDetails(newPayloadWithSameSessionId);

    assertNotNull(instance.getPayload(sessionId).getNino());
    assertThat(instance.getPayload(sessionId).getNino(), is(equalTo(nino)));
  }

  @Test
  public void updateMobileNumberToStoredImage()
          throws ImagePayloadException, IOException, CryptoException {
    String sessionId = UUID.randomUUID().toString();
    String mobileNumber = "0777767676766";

    ImagePayload newPayloadWithSameSessionId = new ImagePayload();
    newPayloadWithSameSessionId.setSessionId(sessionId);
    newPayloadWithSameSessionId.setMobileNumber(mobileNumber);

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(newPayloadWithSameSessionId));

    instance.getPayload(sessionId);
    instance.updateMobileDetails(newPayloadWithSameSessionId);

    assertNotNull(instance.getPayload(sessionId).getMobileNumber());
    assertThat(instance.getPayload(sessionId).getMobileNumber(), is(equalTo(mobileNumber)));
  }

  @Test
  public void updateAddressInformation()
          throws IOException, ImagePayloadException, CryptoException {
    String sessionId = UUID.randomUUID().toString();
    Address address =
            new ObjectMapper()
                    .readValue(
                            String.format(
                                    "{ \"sessionId\" :\"%s\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}",
                                    sessionId),
                            Address.class);

    ImagePayload newPayloadWithSameSessionId = new ImagePayload();
    newPayloadWithSameSessionId.setClaimantAddress(address);
    newPayloadWithSameSessionId.setSessionId(sessionId);

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(newPayloadWithSameSessionId));

    instance.getPayload(sessionId);
    instance.updateAddressDetails(newPayloadWithSameSessionId);

    assertNotNull(instance.getPayload(sessionId).getClaimantAddress());
    assertThat(
            instance.getPayload(sessionId).getClaimantAddress().getHouseNameOrNumber(),
            is(equalTo(address.getHouseNameOrNumber())));
    assertThat(
            instance.getPayload(sessionId).getClaimantAddress().getPostcode(),
            is(equalTo(address.getPostcode())));
    assertThat(
            instance.getPayload(sessionId).getClaimantAddress().getStreet(),
            is(equalTo(address.getStreet())));
    assertThat(
            instance.getPayload(sessionId).getClaimantAddress().getCity(),
            is(equalTo(address.getCity())));
  }

  @Test
  public void updateImageDetails() throws IOException, ImagePayloadException, CryptoException {
    String sessionId = UUID.randomUUID().toString();

    ImagePayload newPayload = new ImagePayload();
    newPayload.setFitnoteCheckStatus(ImagePayload.Status.CHECKING);
    newPayload.setSessionId(sessionId);
    newPayload.setImage("i-am-an-image");

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(newPayload));

    instance.getPayload(sessionId);
    instance.updateImageDetails(newPayload);

    assertNotNull(instance.getPayload(sessionId).getImage());
    assertThat(
            instance.getPayload(sessionId).getFitnoteCheckStatus(),
            is(equalTo(ImagePayload.Status.CHECKING)));
    assertThat(instance.getPayload(sessionId).getImage(), is(equalTo("i-am-an-image")));
  }


  @Test
  public void expiredPayloadIsDeleted()
          throws ImagePayloadException, CryptoException, InterruptedException, IOException {
    String sessionId = UUID.randomUUID().toString();

    ImageStorage localInstance = new ImageStorage(configuration, redisClient, cryptoDataManager);
    String nino = "AA370773A";

    ImagePayload newPayloadWithSameSessionId = new ImagePayload();
    newPayloadWithSameSessionId.setSessionId(sessionId);
    newPayloadWithSameSessionId.setNino(nino);

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(newPayloadWithSameSessionId));

    localInstance.updateNinoDetails(newPayloadWithSameSessionId);
    newPayloadWithSameSessionId = localInstance.getPayload(sessionId);

    TimeUnit.MILLISECONDS.sleep((configuration.getSessionExpiryTimeInSeconds() * 1000) + 3000);

    assertThat(
            "should not be the original object",
            localInstance.getPayload(sessionId).getNino(),
            is(not(equalTo(newPayloadWithSameSessionId.getNino()))));
  }

  @Test
  public void unexpiredPayloadPersists()
          throws ImagePayloadException, CryptoException, IOException {
    String sessionId = UUID.randomUUID().toString();
    String nino = "AA370773A";

    ImagePayload newPayloadWithSameSessionId = new ImagePayload();
    newPayloadWithSameSessionId.setSessionId(sessionId);
    newPayloadWithSameSessionId.setNino(nino);

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(newPayloadWithSameSessionId));

    instance.updateNinoDetails(newPayloadWithSameSessionId);

    assertThat(
            "should not be the original object",
            instance.getPayload(sessionId).getNino(),
            is(equalTo(newPayloadWithSameSessionId.getNino())));
  }

  @Test
  public void updateDetailsAddsNinoWithSuffixToStoredObject()
          throws ImagePayloadException, CryptoException, IOException {
    String sessionId = UUID.randomUUID().toString();
    String nino = "AA370773A";

    ImagePayload newPayloadWithSameSessionId = new ImagePayload();
    newPayloadWithSameSessionId.setSessionId(sessionId);
    newPayloadWithSameSessionId.setNino(nino);

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(newPayloadWithSameSessionId));

    instance.updateNinoDetails(newPayloadWithSameSessionId);

    ImagePayload updatedPayload = instance.getPayload(sessionId);

    assertThat(updatedPayload.getNinoObject().getNinoBody(), is(equalTo(nino.substring(0, 8))));
    assertThat(updatedPayload.getNinoObject().getNinoSuffix(), is(equalTo(nino.substring(8))));
  }

  @Test
  public void updateDetailsAddsNinoWithoutSuffixToStoredImage()
          throws ImagePayloadException, CryptoException, IOException {
    String sessionId = UUID.randomUUID().toString();
    String nino = "AA370773";

    ImagePayload newPayloadWithSameSessionId = new ImagePayload();
    newPayloadWithSameSessionId.setSessionId(sessionId);
    newPayloadWithSameSessionId.setNino(nino);

    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
            .thenReturn(mapper.writeValueAsString(newPayloadWithSameSessionId));

    instance.updateNinoDetails(newPayloadWithSameSessionId);

    ImagePayload updatedPayload = instance.getPayload(sessionId);

    assertThat(updatedPayload.getNinoObject().getNinoBody(), is(equalTo(nino.substring(0, 8))));
    assertThat(updatedPayload.getNinoObject().getNinoSuffix(), is(equalTo("")));
  }

  @Test
  public void updateTimeoutForSessionIsExtendedPLAIN()
          throws CryptoException, IOException, ImagePayloadException, InterruptedException {
    when(configuration.getSessionExpiryTimeInSeconds()).thenReturn(10L);
    when(configuration.isRedisEncryptMessages()).thenReturn(false);

    String sessionId = UUID.randomUUID().toString();
    String nino = "i-am-a-nino";

    ImagePayload newPayload = new ImagePayload();
    newPayload.setSessionId(sessionId);
    newPayload.setNino(nino);

    instance.updateNinoDetails(newPayload);

    TimeUnit.SECONDS.sleep(6); // 2/3 session timeout
    instance.extendSessionTimeout(sessionId);

    TimeUnit.SECONDS.sleep(8); // push over the timeout limit
    ImagePayload savedPayload = instance.getPayload(sessionId);
    assertThat(savedPayload.getNino(), is(equalTo(nino)));
  }

  @Test(expected = ImageHashException.class)
  public void testNullSubmissionFails() throws ImageHashException, ImagePayloadException, CryptoException {
    instance.updateImageHashStore(null);
  }

  @Test
  @SuppressWarnings({"squid:S2699"}) // allow string literals and non-standard variable names for clarity
  public void testHashGetsExpiredInsteadOfReplayLimit()
          throws ImageHashException, ImagePayloadException, CryptoException, InterruptedException {
    ImageStorage localInstance = new ImageStorage(configuration, redisClient, cryptoDataManager);
    String input = "i-am-an-image";

    ImagePayload incomingPayload = new ImagePayload();
    incomingPayload.setImage(input);

    localInstance.updateImageHashStore(incomingPayload);
    localInstance.updateImageHashStore(incomingPayload);

    TimeUnit.MILLISECONDS.sleep((configuration.getImageReplayExpirySeconds() * 1000) + 3000);

    localInstance.updateImageHashStore(incomingPayload);

  }

  @Test(expected = ImageHashException.class)
  public void testExceptionForNullSalt() throws ImageHashException, ImagePayloadException, CryptoException {
    when(configuration.getImageHashSalt()).thenReturn(null);
    ImagePayload incomingPayload = new ImagePayload();
    incomingPayload.setImage("i-am-an-iage");
    instance.updateImageHashStore(incomingPayload);
  }

  @Test(expected = ImageHashException.class)
  public void testHashLimitsIsReached() throws ImageHashException, ImagePayloadException, CryptoException {
    ImagePayload incomingPayload = new ImagePayload();
    incomingPayload.setImage("i-am-an-image");
    instance.updateImageHashStore(incomingPayload);
    instance.updateImageHashStore(incomingPayload);
    instance.updateImageHashStore(incomingPayload);
  }
}
