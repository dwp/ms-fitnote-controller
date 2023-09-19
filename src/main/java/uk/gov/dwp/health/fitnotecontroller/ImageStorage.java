package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.crypto.CryptoMessage;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ImageHashStore;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageHashException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class ImageStorage {
  private static final Logger LOG = LoggerFactory.getLogger(ImageStorage.class.getName());
  private static final String NULL_PAYLOAD_MSG = "Null payload object rejected";
  private StatefulRedisClusterConnection<String, String> redisConnection = null;
  private static final String IMAGE_HASHSTORE_NAME = "fitnote:image-hashstore:";
  private static final String IMAGE_PAYLOAD_NAME = "fitnote:image-payload:";
  private final FitnoteControllerConfiguration configuration;
  private final ObjectMapper mapper = new ObjectMapper();
  private final CryptoDataManager cryptoDataManager;
  private RedisClusterClient redisClient;

  public ImageStorage(
      FitnoteControllerConfiguration configuration,
      RedisClusterClient redis,
      CryptoDataManager cryptoDataManager) {
    this.cryptoDataManager = cryptoDataManager;
    this.configuration = configuration;
    this.redisClient = redis;
    this.initialiseMapper();
  }

  private void initialiseMapper() {
    mapper.getFactory().setStreamReadConstraints(StreamReadConstraints
            .builder()
            .maxStringLength(configuration.getObjectMaxStringLength())
            .build());
  }


  private synchronized RedisAdvancedClusterCommands<String, String> getSynchronousCommands() {
    if (redisConnection == null || !redisConnection.isOpen()) {
      redisConnection = redisClient.connect();
    }

    return redisConnection.sync();
  }

  private String encode(ImagePayload imagePayload) throws CryptoException, JsonProcessingException {
    String serialisedClass = mapper.writeValueAsString(imagePayload);
    return configuration.isRedisEncryptMessages()
        ? mapper.writeValueAsString(cryptoDataManager.encrypt(serialisedClass))
        : serialisedClass;
  }

  private String decode(String redisValue) throws IOException, CryptoException {
    return configuration.isRedisEncryptMessages()
        ? cryptoDataManager.decrypt(mapper.readValue(redisValue, CryptoMessage.class))
        : redisValue;
  }

  public ImagePayload getPayload(String sessionId)
      throws ImagePayloadException, IOException, CryptoException {
    if (sessionId == null) {
      throw new ImagePayloadException("Null sessionId rejected");
    }

    synchronized (this) {
      ImagePayload payloadItem;

      if (getSynchronousCommands().exists(IMAGE_PAYLOAD_NAME + sessionId) > 0) {
        payloadItem =
            mapper.readValue(
                decode(getSynchronousCommands().get(IMAGE_PAYLOAD_NAME + sessionId)),
                ImagePayload.class);

      } else {
        LOG.debug("Session id does not exist, created entry for {}", sessionId);
        payloadItem = new ImagePayload();

        payloadItem.setExpiryTime(
            getCurrentTimeMillis() + (configuration.getSessionExpiryTimeInSeconds() * 1000));
        payloadItem.setFitnoteCheckStatus(ImagePayload.Status.CREATED);
        payloadItem.setSessionId(sessionId);

        setAndUpdateImagePayloadItem(IMAGE_PAYLOAD_NAME + sessionId, encode(payloadItem));
      }

      return payloadItem;
    }
  }

  public void updateImageHashStore(ImagePayload incomingPayload) throws ImageHashException,
      ImagePayloadException, CryptoException {
    if (incomingPayload.getImage() == null) {
      throw new ImageHashException("Null sourceImage rejected");
    }
    if (configuration.getImageHashSalt() == null) {
      throw new ImageHashException("Salt cannot be null");
    }

    String sourceImage = incomingPayload.getImage();
    try {
      MessageDigest hash = MessageDigest.getInstance("SHA-256");
      hash.update(configuration.getImageHashSalt().getBytes(StandardCharsets.UTF_8));
      ImageHashStore hashStoreItem;

      String hashImage = Base64
          .encodeBase64String(hash.digest(sourceImage.getBytes(StandardCharsets.UTF_8)));

      if (getSynchronousCommands().exists(IMAGE_HASHSTORE_NAME + hashImage) > 0) {
        hashStoreItem =
            mapper.readValue(
                getSynchronousCommands().get(IMAGE_HASHSTORE_NAME + hashImage),
                ImageHashStore.class);

      } else {
        LOG.debug(
            "new image received, hash value will expire in {} seconds",
            configuration.getImageReplayExpirySeconds());
        hashStoreItem = new ImageHashStore();
        hashStoreItem.initCreateDateTime();
      }

      hashStoreItem.updateLastSubmitted();
      hashStoreItem.incSubmissionCount();

      LOG.info("updating image hash for to redis");
      getSynchronousCommands()
          .set(IMAGE_HASHSTORE_NAME + hashImage, mapper.writeValueAsString(hashStoreItem));
      getSynchronousCommands()
          .expire(IMAGE_HASHSTORE_NAME + hashImage, configuration.getImageReplayExpirySeconds());

      if (hashStoreItem.getSubmissionCount() > configuration.getMaxAllowedImageReplay()) {
        LOG.info(
            "Image hash has been replayed {} times since creation ({}). "
                + "Potential DDOS replay, aborting submission",
            hashStoreItem.getSubmissionCount(),
            hashStoreItem.getCreateDateTime());

        ImagePayload storedPayload = getPayload(incomingPayload.getSessionId());
        storedPayload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_MAX_REPLAY);
        updateImageDetails(storedPayload);

        LOG.error("Image replay limit exceeded, rejected for {} ",
                incomingPayload.getLogMessage());

        throw new ImageHashException("Image replay limited exceeded, rejecting");
      }

    } catch (NoSuchAlgorithmException | IOException e) {
      LOG.debug(e.getClass().getName(), e);
      LOG.error("Exception thrown when updating Image hashstore for {}",
            incomingPayload.getLogMessage());
      LOG.error(e.getMessage());
    }
  }

  public void updateNinoDetails(ImagePayload payload)
      throws ImagePayloadException, IOException, CryptoException {
    if (payload == null) {
      throw new ImagePayloadException(NULL_PAYLOAD_MSG);
    }

    ImagePayload payloadToChange = getPayload(payload.getSessionId());
    LOG.debug("Updating Nino for session id {}", payload.getSessionId());
    payloadToChange.setNino(payload.getNino());

    setAndUpdateImagePayloadItem(
        IMAGE_PAYLOAD_NAME + payload.getSessionId(), encode(payloadToChange));
  }

  public void updateMobileDetails(ImagePayload payload)
      throws ImagePayloadException, IOException, CryptoException {
    if (payload == null) {
      throw new ImagePayloadException(NULL_PAYLOAD_MSG);
    }

    ImagePayload payloadToChange = getPayload(payload.getSessionId());
    LOG.debug("Updating Nino for session id {}", payload.getSessionId());
    payloadToChange.setMobileNumber(payload.getMobileNumber());

    setAndUpdateImagePayloadItem(
        IMAGE_PAYLOAD_NAME + payload.getSessionId(), encode(payloadToChange));
  }

  public void updateImageDetails(ImagePayload payload)
      throws ImagePayloadException, IOException, CryptoException {
    if (payload == null) {
      throw new ImagePayloadException(NULL_PAYLOAD_MSG);
    }

    ImagePayload payloadToChange = getPayload(payload.getSessionId());
    LOG.debug("Updating image data for session id {}", payload.getSessionId());
    payloadToChange.setFitnoteCheckStatus(payload.getFitnoteCheckStatus());
    payloadToChange.setImage(payload.getImage());

    setAndUpdateImagePayloadItem(
        IMAGE_PAYLOAD_NAME + payload.getSessionId(), encode(payloadToChange));
  }

  public void updateAddressDetails(ImagePayload payload)
      throws ImagePayloadException, IOException, CryptoException {
    if (payload == null) {
      throw new ImagePayloadException(NULL_PAYLOAD_MSG);
    }

    ImagePayload payloadToChange = getPayload(payload.getSessionId());
    LOG.debug("Updating address data for session id {}", payload.getSessionId());
    payloadToChange.setClaimantAddress(payload.getClaimantAddress());

    setAndUpdateImagePayloadItem(
        IMAGE_PAYLOAD_NAME + payload.getSessionId(), encode(payloadToChange));
  }

  public void clearSession(String sessionId) {
    LOG.info("Removing sessionId {} from redis store", sessionId);
    getSynchronousCommands().del(IMAGE_PAYLOAD_NAME + sessionId);
  }

  public void extendSessionTimeout(String sessionId)
      throws ImagePayloadException, IOException, CryptoException {
    if (sessionId == null) {
      throw new ImagePayloadException("SessionId cannot be null");
    }

    ImagePayload payloadToChange = getPayload(sessionId);
    LOG.info(
        "extending timeout for session id {} to {}s",
        sessionId,
        configuration.getSessionExpiryTimeInSeconds());

    setAndUpdateImagePayloadItem(IMAGE_PAYLOAD_NAME + sessionId, encode(payloadToChange));
  }

  private void setAndUpdateImagePayloadItem(String compositeKey, String contents) {
    getSynchronousCommands().set(compositeKey, contents);
    getSynchronousCommands().expire(compositeKey, configuration.getSessionExpiryTimeInSeconds());
  }

  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }
}
