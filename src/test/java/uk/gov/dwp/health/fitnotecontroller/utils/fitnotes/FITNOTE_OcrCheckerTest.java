package uk.gov.dwp.health.fitnotecontroller.utils.fitnotes;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.utils.OcrChecker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1192", "squid:S00101"})
// all string literals and non-standard naming convention of FITNOTE_xxx
public class FITNOTE_OcrCheckerTest {
  private OcrChecker checker;

  @Mock
  private FitnoteControllerConfiguration mockConfig;

  @Before
  public void setup() {
    List<String> topLeftList = new LinkedList<>();
    List<String> topRightList = new LinkedList<>();
    List<String> baseRightList = new LinkedList<>();
    List<String> baseLeftList = new LinkedList<>();
    topLeftList.add("Fitness for work");
    topRightList.add("what to do now");
    topRightList.add("may be collected");
    baseLeftList.add("Unique ID: Med 3");
    baseRightList.add("signed this");
    baseRightList.add("make a claim");

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

  private String getEncodedImage(String imageFileName) throws IOException {
    File file = new File(imageFileName);
    return Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
  }

  @Test
  public void confirmNewSampleBehaviour() throws IOException {
    HashMap<String, ExpectedFitnoteFormat.Status> samplePictures = new LinkedHashMap<>();
    samplePictures.put("/fitnotes/newSample/IMG_0002.JPG", ExpectedFitnoteFormat.Status.SUCCESS);
    samplePictures.put("/fitnotes/newSample/IMG_0006.JPG", ExpectedFitnoteFormat.Status.SUCCESS);
    samplePictures.put("/fitnotes/newSample/IMG_0012.JPG", ExpectedFitnoteFormat.Status.SUCCESS);
    samplePictures.put("/fitnotes/newSample/IMG_0015.JPG", ExpectedFitnoteFormat.Status.FAILED);

    for (Map.Entry<String, ExpectedFitnoteFormat.Status> item : samplePictures.entrySet()) {
      assertThat(String.format("%s :: expected result was %s", item.getKey(), item.getValue()), checker.imageContainsReadableText(getTestImage(item.getKey())).getStatus(), is(equalTo(item.getValue())));
    }
  }

  @Test
  public void confirmOcrCheckerCanRecogniseText() throws IOException {
    assertThat(checker.imageContainsReadableText(getTestImage("/fitnotes/FullPage.jpg")).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmOldStyleFitnoteIsUnsuccessfulWithStandardSixthSlice() throws IOException {
    assertThat(checker.imageContainsReadableText(getTestImage("/fitnotes/oldStyleSubmittedFitnote.jpg")).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.PARTIAL)));
  }

  @Test
  public void confirmOldStyleFitnoteIsSuccessfulWithVerticalQuarterSlice() throws IOException {
    when(mockConfig.getOcrVerticalSlice()).thenReturn(4);

    assertThat(checker.imageContainsReadableText(getTestImage("/fitnotes/oldStyleSubmittedFitnote.jpg")).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmOcrCheckerCanRecognisesACrumpledSheet() throws IOException {
    assertThat(checker.imageContainsReadableText(getTestImage("/fitnotes/test_new.jpg")).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmOcrCheckerCanRecogniseHiResColour() throws IOException {
    assertThat(checker.imageContainsReadableText(getTestImage("/fitnotes/Fitnote_Colour.jpg")).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmOcrCheckerCanRecogniseUpsideDownText() throws IOException {
    assertThat(checker.imageContainsReadableText(getTestImage("/fitnotes/FullPageUpsideDown.jpg")).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmOcrCheckerDoesNotRecogniseNonExistentText() throws IOException {
    ExpectedFitnoteFormat format = checker.imageContainsReadableText(getTestImage("/fitnotes/partialfitnote.jpg"));
    assertThat(format.getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.FAILED)));
    assertThat(format.getFailureReason(), is(equalTo("{0=FAILED - checkHighMarks, 90=FAILED - checkHighMarks, 180=FAILED - checkHighMarks, 270=FAILED - checkHighMarks}")));
  }

  @Test
  public void confirmOcrCheckerPartialOnHalfPage() throws IOException {
    ExpectedFitnoteFormat format = checker.imageContainsReadableText(getTestImage("/fitnotes/HalfPage.jpg"));
    assertThat(format.getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.PARTIAL)));
    assertThat(format.getFailureReason(), is(equalTo("PARTIAL - leftHandSide")));

  }

  @Test
  public void rightHandSidePageIsNotAccepted() throws IOException {
    ExpectedFitnoteFormat format = checker.imageContainsReadableText(getTestImage("/fitnotes/right_hand_side_only.jpg"));

    assertThat(format.getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.FAILED)));
    assertThat(format.getFailureReason(), is(equalTo("{0=FAILED - checkHighMarks, 90=FAILED - checkHighMarks, 180=FAILED - checkHighMarks, 270=FAILED - checkHighMarks}")));

  }

  @Test
  public void confirmImagePayloadDoesNotContainOriginalImageAfterSuccessfulTextRecognition() throws IOException {
    ImagePayload payload = getTestImage("/fitnotes/FullPage.jpg");
    String incorrectImage = payload.getImage();
    checker.imageContainsReadableText(payload);
    assertNotEquals(incorrectImage, payload.getImage());
  }

  @Test
  public void confirmTestFitnoteWorked() throws IOException {
    ImagePayload payload = getTestImage("/fitnotes/FullPage.jpg");
    assertThat(checker.imageContainsReadableText(payload).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmNewFitnoteFormatWorked() throws IOException {
    ImagePayload payload = getTestImage("/fitnotes/NewFormat.jpg");
    assertThat(checker.imageContainsReadableText(payload).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  @Test
  public void confirmPixi3ImageFails() throws IOException {
    ImagePayload payload = getTestImage("/fitnotes/Pixi3.jpg");
    ExpectedFitnoteFormat format =checker.imageContainsReadableText(payload);
    assertThat(format.getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.FAILED)));
    assertThat(format.getFailureReason(), is(equalTo("{0=FAILED - checkHighMarks, 90=FAILED - checkHighMarks, 180=FAILED - checkHighMarks, 270=FAILED - checkHighMarks}")));
  }

  @Test
  public void confirmOriginalPartialMatchImageIsNowAPass() throws IOException {
    ImagePayload payload = getTestImage("/fitnotes/PartialMatch.jpg");
    assertThat("expecting new return status to be SUCCESS", checker.imageContainsReadableText(payload).getStatus(), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
  }

  // Ignoring this test as it conflicts with pi tests
  // Ticket has been raised
  @Test
  public void testTesseractFailure() throws IOException {
    String tessdataLabel = "TESSDATA_PREFIX";
    String tessdataPrefix = System.getProperty(tessdataLabel);
    System.setProperty(tessdataLabel, "");
    when(mockConfig.getTesseractFolderPath()).thenReturn("/Missing-Tesseract-File");
    ImagePayload payload = getTestImage("/fitnotes/FullPage.jpg");
    try {
      checker.imageContainsReadableText(payload);
      assertFalse(true);//should never get here!
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("the tessdata configuration file could not be found in"));
    } finally {
      if(tessdataPrefix != null){
        System.setProperty(tessdataLabel, tessdataPrefix);
      }
    }
  }
}
