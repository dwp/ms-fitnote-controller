package uk.gov.dwp.health.fitnotecontroller.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.awt.Point;

public class DataMatrixResult {

  private String finalImage;
  private int matchAngle;
  private Point position;

  public DataMatrixResult() {}

  public String getFinalImage() {
    return finalImage;
  }

  public int getMatchAngle() {
    return matchAngle;
  }

  public void setMatchAngle(int angle) {
    matchAngle = angle;
  }

  public void setFinalImage(String finalImage) {
    this.finalImage = finalImage;
  }

  public Point getPosition() {
    return position;
  }

  public void setPosition(Point position) {
    this.position = position;
  }
}
