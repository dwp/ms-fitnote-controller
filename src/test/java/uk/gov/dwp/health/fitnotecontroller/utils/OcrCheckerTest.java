package uk.gov.dwp.health.fitnotecontroller.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;

@RunWith(MockitoJUnitRunner.class)
public class OcrCheckerTest {
  private OcrChecker checker;

  @Mock
  private FitnoteControllerConfiguration mockConfig;

  @Before
  public void setup() {
    List<String> topLeftList = new LinkedList<>();
    List<String> topRightList = new LinkedList<>();
    List<String> baseRightList = new LinkedList<>();
    List<String> baseLeftList = new LinkedList<>();
    topLeftList.add("Understand user needs");
    topRightList.add("end-to-end service");
    topRightList.add("all common browsers");
    baseLeftList.add("all new source code open");
    baseRightList.add("Performance Platform");
    baseRightList.add("beginning to end");

    when(mockConfig.getTesseractFolderPath()).thenReturn("src/main/properties/tessdata");
    when(mockConfig.getBorderLossPercentage()).thenReturn(10);
    when(mockConfig.getMaxLogChars()).thenReturn(2000);
    when(mockConfig.getTargetBrightness()).thenReturn(179);
    when(mockConfig.getDiagonalTarget()).thenReturn(20);
    when(mockConfig.getHighTarget()).thenReturn(100);
    when(mockConfig.getContrastCutOff()).thenReturn(105);
    when(mockConfig.getOcrVerticalSlice()).thenReturn(6);
    when(mockConfig.getTopLeftText()).thenReturn(topLeftList);
    when(mockConfig.getTopRightText()).thenReturn(topRightList);
    when(mockConfig.getBaseLeftText()).thenReturn(baseLeftList);
    when(mockConfig.getBaseRightText()).thenReturn(baseRightList);

    checker = new OcrChecker(mockConfig);
  }

  private ImagePayload getTestImage(String imageFile) throws IOException {
    String imageString = getEncodedImage(this.getClass().getResource(imageFile).getPath());
    ImagePayload payload = new ImagePayload();
    payload.setImage(imageString);
    payload.setSessionId(UUID.randomUUID().toString());
    return payload;
  }

  @Test
  public void confirmOcrCheckerCanRecogniseText() throws IOException {
    assertThat(checker.imageContainsReadableText(getTestImage("/OcrTest.jpg")).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmOcrCheckertestTesseractFailureCanRecogniseUpsideDownText() throws IOException {
    assertThat(checker.imageContainsReadableText(getTestImage("/OcrTest_UpsideDown.jpg")).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmOcrCheckerDoesNotRecogniseNonExistentText() throws IOException {
    ExpectedFitnoteFormat format = checker.imageContainsReadableText(getTestImage("/EmptyPage.jpg"));
    assertThat(format.getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.FAILED)));
    assertThat(format.getFailureReason(), is(equalTo("{0=FAILED - Portrait image, 90=FAILED - checkHighMarks, 180=FAILED - checkHighMarks, 270=FAILED - checkHighMarks}")));
  }

  @Test
  public void confirmOcrCheckerPartialOnHalfPage() throws IOException {
    ExpectedFitnoteFormat format = checker.imageContainsReadableText(getTestImage("/OcrTest_LHS.jpg"));
    assertThat(format.getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.PARTIAL)));
    assertThat(format.getFailureReason(), is(equalTo("PARTIAL - leftHandSide")));

  }

  @Test
  public void rightHandSidePageIsNotAccepted() throws IOException {
    ExpectedFitnoteFormat format = checker.imageContainsReadableText(getTestImage("/OcrTest_RHS.jpg"));
    assertThat(format.getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.FAILED)));
    assertThat(format.getFailureReason(), is(equalTo("{0=FAILED - checkHighMarks, 90=FAILED - checkHighMarks, 180=FAILED - checkHighMarks, 270=FAILED - checkHighMarks}")));

  }

  @Test
  public void confirmImagePayloadDoesNotContainOriginalImageAfterSuccessfulTextRecognition() throws IOException {
    ImagePayload payload = getTestImage("/OcrTest.jpg");
    String incorrectImage = payload.getImage();
    checker.imageContainsReadableText(payload);
    assertNotEquals(incorrectImage, payload.getImage());
  }

  @Test
  public void confirmNewFitnoteFormatWorked() throws IOException {
    ImagePayload payload = getTestImage("/OcrTest_New.jpg");
    assertThat(checker.imageContainsReadableText(payload).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmNhsFitnoteFormatWorked() throws IOException {
    ImagePayload payload = getTestImage("/NHS_fitnote.jpg");
    assertThat(checker.imageContainsReadableText(payload).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmPixi3ImageFails() throws IOException {
    ImagePayload payload = getTestImage("/Pixi3.jpg");
    assertThat(checker.imageContainsReadableText(payload).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.FAILED)));
  }

  @Test
  public void testTesseractFailure() throws IOException {
    when(mockConfig.getTesseractFolderPath()).thenReturn("/Missing-Tesseract-File");
    String tessdataLabel = "TESSDATA_PREFIX";
    String tessdataPrefix = System.getProperty(tessdataLabel);
    System.setProperty(tessdataLabel, "");
    ImagePayload payload = getTestImage("/FullPage_Portrait.jpg");
    try {
      checker.imageContainsReadableText(payload);
      fail("should throw an error");

    } catch (IOException e) {
      assertTrue(e.getMessage().contains("the tessdata configuration file could not be found in"));
    } finally {
      if (tessdataPrefix != null) {
        System.setProperty(tessdataLabel, tessdataPrefix);
      }
    }
  }

  private String getEncodedImage(String imageFileName) throws IOException {
    File file = new File(imageFileName);
    return Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
  }
}
