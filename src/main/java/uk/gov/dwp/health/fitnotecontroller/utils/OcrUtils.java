package uk.gov.dwp.health.fitnotecontroller.utils;

import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixReadMem;
import static org.bytedeco.tesseract.global.tesseract.PSM_SPARSE_TEXT;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;

public class OcrUtils {

  private static final Logger LOG = LoggerFactory.getLogger(OcrUtils.class.getName());

  public static void ocrApplyImageFilters(
      BufferedImage subImage,
      TessBaseAPI ocr,
      ExpectedFitnoteFormat fitnoteFormat,
      String location,
      int targetPercentage,
      FitnoteControllerConfiguration configuration) {
    BufferedImage workingImage = null;
    int filterApplications = 0;
    int highPercentage = 0;
    String filter = "";

    LOG.info(
        "*** START {} CHECKS, AIMING FOR {} PERCENTAGE @ {} ROTATION ***",
        location,
        targetPercentage,
        fitnoteFormat.getMatchAngle());
    while (highPercentage < targetPercentage && filterApplications < 4) {
      switch (filterApplications) {
        case 0:
          workingImage =
              ImageUtils.normaliseBrightness(
                  subImage,
                  configuration.getTargetBrightness(),
                  configuration.getBorderLossPercentage());
          filter = "brightness";
          break;
        case 1:
          workingImage = subImage;
          filter = "plain";
          break;
        case 2:
          workingImage = ImageUtils.increaseContrast(subImage, configuration.getContrastCutOff());
          filter = "contrast";
          break;
        case 3:
          workingImage =
              ImageUtils.increaseContrast(
                  ImageUtils.formatGrayScale(subImage), configuration.getContrastCutOff());
          filter = "gr contrast";
          break;
        default:
          LOG.info("No filter available for {}", filterApplications);
          break;
      }

      if (workingImage != null) {
        LOG.info(
            "OCR checking using filter '{}' on {} page location at {} rotation",
            filter,
            location,
            fitnoteFormat.getMatchAngle());

        try {
          if ("TL".equalsIgnoreCase(location)) {
            fitnoteFormat.scanTopLeft(ocrScanSubImage(workingImage, ocr));
            highPercentage = fitnoteFormat.getTopLeftPercentage();

          } else if ("TR".equalsIgnoreCase(location)) {
            fitnoteFormat.scanTopRight(ocrScanSubImage(workingImage, ocr));
            highPercentage = fitnoteFormat.getTopRightPercentage();

          } else if ("BL".equalsIgnoreCase(location)) {
            fitnoteFormat.scanBaseLeft(ocrScanSubImage(workingImage, ocr));
            highPercentage = fitnoteFormat.getBaseLeftPercentage();

          } else {
            fitnoteFormat.scanBaseRight(ocrScanSubImage(workingImage, ocr));
            highPercentage = fitnoteFormat.getBaseRightPercentage();
          }
        } catch (IOException e) {
          throw new RuntimeException("Error processing image", e);
        }
      }

      if (filterApplications == 1 && highPercentage < configuration.getDiagonalTarget()) {
        LOG.info(
            "Abandoned time-costly checks after 2 filters with < {} "
                + "percentage OCR for location {} at rotation {}",
            configuration.getDiagonalTarget(),
            location,
            fitnoteFormat.getMatchAngle());
        return;
      }

      filterApplications++;
    }

    LOG.info("*** END {} CHECKS ***", location);
  }

  private static String ocrScanSubImage(BufferedImage read, TessBaseAPI instance)
      throws IOException {
    String returnString = "";

    BytePointer bytePointer = null;
    PIX imageObject = null;

    try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
      ImageIO.write(read, "jpg", outStream);
      instance.SetPageSegMode(PSM_SPARSE_TEXT);

      imageObject = pixReadMem(outStream.toByteArray(), outStream.toByteArray().length);
      instance.SetImage(imageObject);
      bytePointer = instance.GetUTF8Text();

      if (null != bytePointer) {
        returnString = bytePointer.getString().toUpperCase();
      }

      instance.Clear();

    } finally {
      if (bytePointer != null) {
        bytePointer.deallocate();
      }
      if (imageObject != null) {
        pixDestroy(imageObject);
      }
    }
    return returnString;
  }
}
