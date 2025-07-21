package uk.gov.dwp.health.fitnotecontroller.utils;

import static uk.gov.dwp.health.fitnotecontroller.utils.OcrUtils.ocrApplyImageFilters;

import java.awt.image.BufferedImage;
import java.util.concurrent.RecursiveAction;
import org.bytedeco.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;

public class OcrApplyImageFilters extends RecursiveAction {

  private final ExpectedFitnoteFormat fitnoteFormat;
  private final TessBaseAPI ocr;
  private final BufferedImage subImage;
  private final String location;
  private final int targetPercentage;
  private final FitnoteControllerConfiguration configuration;

  private static final Logger LOG = LoggerFactory.getLogger(OcrApplyImageFilters.class.getName());

  public OcrApplyImageFilters(BufferedImage subImage, TessBaseAPI ocr,
                              ExpectedFitnoteFormat fitnoteFormat, String location,
                              int targetPercentage, FitnoteControllerConfiguration configuration) {
    this.subImage = subImage;
    this.ocr = ocr;
    this.fitnoteFormat = fitnoteFormat;
    this.location = location;
    this.targetPercentage = targetPercentage;
    this.configuration = configuration;
  }

  @Override
  protected void compute() {
    if (subImage.getWidth() == fitnoteFormat.getFinalImage().getWidth()) {
      int highPercentage = 0;
      int width = subImage.getWidth() / 2;
      int height = subImage.getHeight() / 2;
      int heightDifferential = subImage.getHeight() / configuration.getOcrVerticalSlice();
      int x = 0;
      int y = 0;
      int firstRegion = heightDifferential * (configuration.getOcrVerticalSlice() - 1);

      if (location.substring(1).equalsIgnoreCase("R")) {
        x = width;
      }
      if (location.substring(0, 1).equalsIgnoreCase("B")) {
        y = firstRegion;
      }


      while (highPercentage < this.targetPercentage) {
        BufferedImage slice = subImage.getSubimage(x, y, width, heightDifferential);
        int tempBottomHeight = fitnoteFormat.getBottomHeight();
        if (location.equalsIgnoreCase("BR") && y < firstRegion) {
          fitnoteFormat.setBottomHeight(y);
        }
        new OcrApplyImageFilters(slice, ocr, fitnoteFormat, location, targetPercentage,
            configuration).invoke();
        if (location.equalsIgnoreCase("BR") && y < firstRegion) {
          fitnoteFormat.setBottomHeight(tempBottomHeight);
        }
        int newHighPercentage = switch (location) {
          case "TL" -> fitnoteFormat.getTopLeftPercentage();
          case "BR" -> fitnoteFormat.getBaseRightPercentage();
          case "BL" -> fitnoteFormat.getBaseLeftPercentage();
          case "TR" -> fitnoteFormat.getTopRightPercentage();
          default -> -1;
        };
        boolean updateHeight = newHighPercentage > highPercentage;
        highPercentage = Math.max(newHighPercentage, highPercentage);
        if (location.substring(0, 1).equalsIgnoreCase("T")) {
          if (updateHeight) {
            fitnoteFormat.setTopHeight(y);
          }
          y += heightDifferential;
          if (y >= height) {
            break;
          }
        } else {
          if (updateHeight) {
            fitnoteFormat.setBottomHeight(y);
          }
          y -= heightDifferential;
          if (y < height) {
            break;
          }
        }
      }

    } else {
      ocrApplyImageFilters(subImage, ocr, fitnoteFormat, location, targetPercentage, configuration);
    }

    LOG.debug("*** END {} CHECKS ***", location);

  }


}
