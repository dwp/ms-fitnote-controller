package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.regex.NinoValidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImagePayload {
  private static final Logger LOG = LoggerFactory.getLogger(ImagePayload.class.getName());

  public enum Status {
    CREATED,
    UPLOADED,
    CHECKING,
    FAILED_IMG_SIZE,
    PASS_IMG_SIZE,
    FAILED_IMG_OCR,
    FAILED_IMG_OCR_PARTIAL,
    PASS_IMG_OCR,
    SUCCEEDED,
    FAILED_ERROR
  }

  @JsonView({
      Views.SessionOnly.class,
      Views.QueryNinoDetails.class,
      Views.QueryAddressDetails.class,
      Views.QueryMobileDetails.class
  })
  private String sessionId;

  private byte[] image;
  private long expiryTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonView(Views.QueryNinoDetails.class)
  private String nino;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonView(Views.QueryMobileDetails.class)
  private String mobileNumber;

  private Status fitnoteCheckStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonView(Views.QueryAddressDetails.class)
  private Address claimantAddress;

  public long getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(long expiryTime) {
    this.expiryTime = expiryTime;
  }

  public NinoValidator getNinoObject() {
    if (nino == null) {
      return null;
    } else {
      return (nino.length() == 9)
          ? new NinoValidator(nino.substring(0, 8), nino.substring(8))
          : new NinoValidator(nino, "");
    }
  }

  @JsonIgnore
  public int getRawImageSize() {
    return this.image != null ? this.image.length : 0;
  }

  public String getNino() {
    return nino;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public void setNino(String nino) {
    this.nino = nino;
  }

  public String getMobileNumber() {
    return mobileNumber;
  }

  public void setMobileNumber(String mobileNumber) {
    this.mobileNumber = mobileNumber;
  }

  public String getImage() {
    return uncompress(image);
  }

  public void setImage(String image) {
    this.image = compress(image);
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setFitnoteCheckStatus(Status fitnoteCheckStatus) {
    this.fitnoteCheckStatus = fitnoteCheckStatus;
  }

  public Status getFitnoteCheckStatus() {
    return fitnoteCheckStatus;
  }

  public Address getClaimantAddress() {
    return claimantAddress;
  }

  public void setClaimantAddress(Address claimantAddress) {
    this.claimantAddress = claimantAddress;
  }

  private byte[] compress(String str) {
    if (str != null) {
      try {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          GZIPOutputStream gzip = new GZIPOutputStream(out);
          gzip.write(str.getBytes(StandardCharsets.UTF_8));
          gzip.close();

          return out.toByteArray();
        }

      } catch (IOException e) {
        LOG.error("Error compressing string : {}", e.getMessage());
        LOG.debug(e.getClass().getName(), e);
      }
    }

    return null; // NOSONAR - we want to return null
  }

  private String uncompress(byte[] str) {
    if (str != null) {
      GZIPInputStream gzipIn = null;
      try {

        try (ByteArrayInputStream inSteam = new ByteArrayInputStream(str)) {
          gzipIn = new GZIPInputStream(inSteam);
        }

        return IOUtils.toString(gzipIn);
      } catch (IOException e) {
        LOG.error("Error uncompressing string : {}", e.getMessage());
        LOG.debug(e.getClass().getName(), e);
      }
    }
    return null;
  }
}
