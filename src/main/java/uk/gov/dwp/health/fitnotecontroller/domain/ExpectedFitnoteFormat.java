package uk.gov.dwp.health.fitnotecontroller.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.exception.FuzzyStringMatchException;
import uk.gov.dwp.health.fitnotecontroller.utils.FuzzyStringMatch;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpectedFitnoteFormat {
  private static final Logger LOG = LoggerFactory.getLogger(ExpectedFitnoteFormat.class.getName());

  public enum StringLocation {
    TOP_LEFT,
    TOP_RIGHT,
    BASE_LEFT,
    BASE_RIGHT,
    BASE_RIGHT_ALT
  }

  public enum Status {
    INITIALISED,
    FAILED,
    PARTIAL,
    SUCCESS
  }

  private Map<StringLocation, StringToMatch> matchingStrings = new EnumMap<>(StringLocation.class);
  private String baseLeftStringFound;
  private String baseRightStringFound;
  private String topLeftStringFound;
  private String topRightStringFound;
  private BufferedImage finalImage;
  private Status localStatus;
  private String failureReason;
  private int diagonalTarget;
  private int diagonalTargetStrict;
  private int highTarget;
  private int matchAngle;
  private int topHeight;
  private int bottomHeight;

  public ExpectedFitnoteFormat(FitnoteControllerConfiguration config) {
    initialise(config);
  }

  public ExpectedFitnoteFormat(Status status, String failureReason) {
    setStatus(status);
    setFailureReason(failureReason);
  }

  @SuppressWarnings("squid:S1611")
  // Isolating the item(s) being acted on in () for a forEach reads easier than removing them. ()
  // are used for a Map (k,v) with
  // the -> acting on the contents of the brackets. Whilst they are required in that case it makes
  // sense to standardise the forEach syntax.
  public void initialise(FitnoteControllerConfiguration config) {
    matchingStrings.clear();
    config.getTopLeftText().forEach((v) -> addString(StringLocation.TOP_LEFT, v));
    config.getTopRightText().forEach((v) -> addString(StringLocation.TOP_RIGHT, v));
    config.getBaseLeftText().forEach((v) -> addString(StringLocation.BASE_LEFT, v));
    config.getBaseRightText().forEach((v) -> addString(StringLocation.BASE_RIGHT, v));
    config.getBaseRightAltText().forEach((v) -> addString(StringLocation.BASE_RIGHT_ALT, v));
    setStatus(Status.INITIALISED);
    setDiagonalTarget(config.getDiagonalTarget());
    setDiagonalTargetStrict(config.getDiagonalTargetStrict());
    setHighTarget(config.getHighTarget());
    setFinalImage(null);
    setMatchAngle(0);
  }

  public BufferedImage getFinalImage() {
    return finalImage;
  }

  private Map<StringLocation, StringToMatch> getMatchingStrings() {
    return matchingStrings;
  }

  public int getMatchAngle() {
    return matchAngle;
  }

  public void setMatchAngle(int angle) {
    matchAngle = angle;
  }

  public void setFinalImage(BufferedImage finalImage) {
    this.finalImage = finalImage;
  }

  private void addString(StringLocation inputLocation, String inputString) {
    if (matchingStrings.get(inputLocation) == null) {
      matchingStrings.put(inputLocation, new StringToMatch(inputString.toUpperCase()));
    } else {
      matchingStrings.get(inputLocation).setupString(inputString.toUpperCase());
    }
  }

  private int getHighTarget() {
    return highTarget;
  }

  private void setHighTarget(int input) {
    highTarget = input;
  }

  private int getDiagonalTarget() {
    return diagonalTarget;
  }

  private void setDiagonalTarget(int diagonalTarget) {
    this.diagonalTarget = diagonalTarget;
  }

  private int getDiagonalTargetStrict() {
    return diagonalTargetStrict;
  }

  private void setDiagonalTargetStrict(int diagonalTargetStrict) {
    this.diagonalTargetStrict = diagonalTargetStrict;
  }

  private void setStatus(Status input) {
    localStatus = input;
  }

  public Status getStatus() {
    return localStatus;
  }

  private void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public int getTopHeight() {
    return topHeight;
  }

  public void setTopHeight(int topHeight) {
    this.topHeight = topHeight;
  }

  public int getBottomHeight() {
    return bottomHeight;
  }

  public void setBottomHeight(int bottomHeight) {
    this.bottomHeight = bottomHeight;
  }

  public void resetHeight() {
    this.topHeight = 0;
    this.bottomHeight = 0;
  }

  public void scanTopLeft(String topLeftString) {
    topLeftStringFound = topLeftString.toUpperCase();
    scan(topLeftString, StringLocation.TOP_LEFT, null);
  }

  public void scanTopRight(String topRightString) {
    topRightStringFound = topRightString.toUpperCase();
    scan(topRightString, StringLocation.TOP_RIGHT, null);
  }

  public void scanBaseLeft(String baseLeftString) {
    baseLeftStringFound = baseLeftString.toUpperCase();
    scan(baseLeftString, StringLocation.BASE_LEFT, null);
  }

  public void scanBaseRight(String baseRightString) {
    baseRightStringFound = baseRightString.toUpperCase();
    StringLocation stringMatchLocation = null;
    if (this.topHeight > 0 || this.bottomHeight > 0) {
      stringMatchLocation = StringLocation.BASE_RIGHT_ALT;
    }
    scan(baseRightString, StringLocation.BASE_RIGHT, stringMatchLocation);
  }

  private void scan(String baseString, StringLocation stringLocation,
                    StringLocation stringMatchLocation) {
    try {
      for (String temp : matchingStrings.get(
          stringMatchLocation == null ? stringLocation : stringMatchLocation).getStringToFind()) {
        matchingStrings.get(stringLocation)
            .setupPercentage(FuzzyStringMatch.fuzzyStringContains(baseString, temp));
      }
    } catch (FuzzyStringMatchException e) {
      LOG.debug(e.getClass().getName(), e);
    }
  }

  public int getTopLeftPercentage() {
    return getItemPercentage(StringLocation.TOP_LEFT);
  }

  public int getBaseLeftPercentage() {
    return getItemPercentage(StringLocation.BASE_LEFT);
  }

  public int getTopRightPercentage() {
    return getItemPercentage(StringLocation.TOP_RIGHT);
  }

  public int getBaseRightPercentage() {
    return getItemPercentage(StringLocation.BASE_RIGHT);
  }

  public String getLoggingString(boolean nhs, int ocrVerticalSlice) {
    String loggingString = String.format(
        "%s TL:%d BR:%d BL:%d TR:%d REASON:%s",
        getStatus().toString(),
        getTopLeftPercentage(),
        getBaseRightPercentage(),
        getBaseLeftPercentage(),
        getTopRightPercentage(),
        getFailureReason());
    if (!nhs && getStatus() == Status.SUCCESS) {
      int verticalSliceHeight = finalImage.getHeight()
          / ocrVerticalSlice;
      int bottomSlice = verticalSliceHeight * 5;
      if (getTopHeight() > 0 || (getBottomHeight() > 0
          && getBottomHeight() < bottomSlice)) {
        loggingString = String.format(
            "%s TL:%s BR:%s BL:%s TR:%s REASON:%s",
            getStatus().toString(),
            getTopLeftPercentage() + getCorner(StringLocation.TOP_LEFT, verticalSliceHeight),
            getBaseRightPercentage() + getCorner(StringLocation.BASE_RIGHT, verticalSliceHeight),
            getBaseLeftPercentage() + getCorner(StringLocation.BASE_LEFT, verticalSliceHeight),
            getTopRightPercentage() + getCorner(StringLocation.TOP_RIGHT, verticalSliceHeight),
            getFailureReason());
      }
    }
    return loggingString;
  }

  private String getCorner(StringLocation corner, int verticalSliceHeight) {
    int region = 0;
    String regionString = "(%d)";
    if (validateTopLeftBaseRight()) {
      if (corner.equals(StringLocation.TOP_LEFT)) {
        region = (getTopHeight() / verticalSliceHeight) + 1;
      } else if (corner.equals(StringLocation.BASE_RIGHT)) {
        region = (getBottomHeight() / verticalSliceHeight) + 1;
      }
    } else if (corner.equals(StringLocation.BASE_LEFT)) {
      regionString = "(%da)";
      region = (getBottomHeight() / verticalSliceHeight) + 4;
    } else if (corner.equals(StringLocation.TOP_RIGHT)) {
      regionString = "(%db)";
      region = (getTopHeight() / verticalSliceHeight) + 7;
    }
    return region > 0 ? String.format(regionString, region) : "";
  }

  private int getItemPercentage(StringLocation itemLocation) {
    return matchingStrings.get(itemLocation).getPercentageFound();
  }

  public String getTopLeftStringToLog() {
    return topLeftStringFound == null ? "" : topLeftStringFound;
  }

  public String getBaseLeftStringToLog() {
    return baseLeftStringFound == null ? "" : baseLeftStringFound;
  }

  public String getTopRightStringToLog() {
    return topRightStringFound == null ? "" : topRightStringFound;
  }

  public String getBaseRightStringToLog() {
    return baseRightStringFound == null ? "" : baseRightStringFound;
  }

  public Status validateFitnotePassed(boolean nhs, boolean strictMatch, int strictTarget) {
    if (checkHighMarks() > 0) {
      if (nhs && getBaseRightPercentage() < strictTarget && getBaseLeftPercentage()
          < strictTarget) {
        setStatus(Status.FAILED);
        setFailureReason("FAILED - Not NHS");
        return getStatus();
      }
      if (validateDiagonals(strictMatch)) {
        setStatus(Status.SUCCESS);
        return getStatus();
      } else {
        setStatus(Status.PARTIAL);
        setFailureReason("PARTIAL - validateDiagonals");
      }

      if (getTopRightPercentage() >= getTopLeftPercentage()
          && getBaseRightPercentage() >= getBaseLeftPercentage()) {
        setStatus(Status.PARTIAL);
        setFailureReason("PARTIAL - rightHandSide");
      } else {
        setStatus(Status.PARTIAL);
        setFailureReason("PARTIAL - leftHandSide");
      }
    } else {
      setStatus(Status.FAILED);
      setFailureReason("FAILED - checkHighMarks");
    }
    return getStatus();
  }

  private int checkHighMarks() {
    AtomicInteger zeroCount = new AtomicInteger(0);
    matchingStrings.forEach((k, v) -> {
      if (v.getPercentageFound() >= getHighTarget()) {
        zeroCount.incrementAndGet();
      }
    });
    return zeroCount.get();
  }

  private boolean validateDiagonals(boolean strictMatch) {
    return validateDiagonal(getTopLeftPercentage(), getBaseRightPercentage(), strictMatch)
        || validateDiagonal(getTopRightPercentage(), getBaseLeftPercentage(), strictMatch);
  }

  private boolean validateDiagonal(int input1, int input2, boolean strictMatch) {
    return ((input1 >= getHighTarget()) && (input2 >= (strictMatch ? getDiagonalTargetStrict() :
        getDiagonalTarget()))) || ((input2 >= getHighTarget()) && (input1 >= (strictMatch
        ? getDiagonalTargetStrict() : getDiagonalTarget())));
  }

  public boolean validateTopLeftBaseRight() {
    return validateDiagonal(getTopLeftPercentage(), getBaseRightPercentage(), true);
  }
}
