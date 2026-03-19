package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatusItem {

  @JsonProperty("fitnoteStatus")
  private String fitnoteStatus;

  @JsonProperty("visibleRegion")
  private String visibleRegion;

  @JsonProperty("image")
  private String base64Image;

  public String getFitnoteStatus() {
    return fitnoteStatus;
  }

  public String getVisibleRegion() {
    return visibleRegion;
  }

  public String getBase64Image() {
    return base64Image;
  }

}
