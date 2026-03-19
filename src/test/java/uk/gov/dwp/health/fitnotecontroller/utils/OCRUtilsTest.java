package uk.gov.dwp.health.fitnotecontroller.utils;

import static org.junit.Assert.assertEquals;
import static uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload.SubStatus.BASE_HALF;
import static uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload.SubStatus.BASE_LEFT;
import static uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload.SubStatus.BASE_RIGHT;
import static uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload.SubStatus.LEFT_SIDE;
import static uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload.SubStatus.TOP_HALF;
import static uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload.SubStatus.TOP_LEFT;
import static uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload.SubStatus.TOP_RIGHT;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.StringToMatch;

public class OCRUtilsTest {

  private ImagePayload imagePayload;

  @Before
  public void setUp() throws IOException {
    imagePayload = new ImagePayload();

  }

  @Test
  public void testSingleCornerError()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat = new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, new StringToMatch(null));
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(TOP_LEFT, imagePayload.getVisibleRegion());
  }

  @Test
  public void testTopHalfError()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat =
        new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, new StringToMatch(null));
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(TOP_HALF, imagePayload.getVisibleRegion());
  }

  @Test
  public void testBaseHalfError()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat =
        new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, stringToMatch);
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(BASE_HALF, imagePayload.getVisibleRegion());
  }

  @Test
  public void testLeftHalfError()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat =
        new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, stringToMatch);
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(LEFT_SIDE, imagePayload.getVisibleRegion());
  }

  @Test
  public void testTopLeftError90()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat =
        new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, new StringToMatch(null));
    fitnoteFormat.setMatchAngle(90);
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(TOP_LEFT, imagePayload.getVisibleRegion());
  }

  @Test
  public void testbaseLeftError90()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat =
        new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, stringToMatch);
    fitnoteFormat.setMatchAngle(90);
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(BASE_LEFT, imagePayload.getVisibleRegion());
  }

  @Test
  public void testTopLeftError180()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat =
        new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, new StringToMatch(null));
    fitnoteFormat.setMatchAngle(180);
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(TOP_LEFT, imagePayload.getVisibleRegion());
  }

  @Test
  public void testTopRightError180()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat =
        new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, new StringToMatch(null));
    fitnoteFormat.setMatchAngle(180);
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(TOP_RIGHT, imagePayload.getVisibleRegion());
  }

  @Test
  public void testTopLEftError270()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat =
        new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, new StringToMatch(null));
    fitnoteFormat.setMatchAngle(270);
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(TOP_LEFT, imagePayload.getVisibleRegion());
  }

  @Test
  public void testBaseRightError270()
      throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    ExpectedFitnoteFormat fitnoteFormat =
        new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.PARTIAL, null);
    Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
        (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
    StringToMatch stringToMatch = new StringToMatch(null);
    stringToMatch.setupPercentage(100);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, stringToMatch);
    strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, new StringToMatch(null));
    strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, new StringToMatch(null));
    fitnoteFormat.setMatchAngle(270);
    OcrUtils.setPartialErrorMessage(imagePayload, fitnoteFormat);
    assertEquals(BASE_RIGHT, imagePayload.getVisibleRegion());
  }

  public static Method getMatchingStringsMethod() throws NoSuchMethodException {
    Method method = ExpectedFitnoteFormat.class.getDeclaredMethod("getMatchingStrings");
    method.setAccessible(true);
    return method;
  }

}
