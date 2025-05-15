package uk.gov.dwp.health.fitnotecontroller.utils;

import static uk.gov.dwp.health.fitnotecontroller.utils.ImageUtils.formatGrayScale;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.im4java.core.IM4JavaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;

public class ImageCompressor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageCompressor.class.getName());
  private boolean rejectingOversizeImages;

  public ImageCompressor(FitnoteControllerConfiguration config) {
    this.rejectingOversizeImages = config.isRejectingOversizeImages();
  }

  public byte[] compressBufferedImage(String fileMimeType, BufferedImage inputImage,
                                      int targetImageSizeKB, boolean useGrayScale)
      throws ImageCompressException {
    final long startTime = System.currentTimeMillis();
    LOGGER.info("Starting image compression :: "
            + "RejectingOversizedImage = {}, TargetSizeKB = {}, GreyScale = {}",
        isRejectingOversizeImages(), targetImageSizeKB, useGrayScale);
    BufferedImage workingImage = useGrayScale ? formatGrayScale(inputImage) : inputImage;
    boolean origCompress = true;
    try {
      byte[] jpegData = compressImage(workingImage, 0, fileMimeType);

      if (jpegData.length > (targetImageSizeKB * 1000)) {
        LOGGER.info("ImageWriter compression failed to reduce to target size");
        origCompress = false;
      }

      if (origCompress) {
        try {
          jpegData =
              compressUsingImageWriter(workingImage, fileMimeType, (targetImageSizeKB * 1000));
        } catch (IOException | ImageCompressException e) {
          LOGGER.debug(e.getClass().getName(), e);
          LOGGER.info("failed to compress, will attempt to use ImageMagick");
        }
      }

      if (jpegData == null
          || jpegData.length > (targetImageSizeKB * 1000) && isRejectingOversizeImages()) {
        jpegData = compressUsingImageMagick(jpegData, targetImageSizeKB);
      }

      if (jpegData == null || (jpegData.length > (targetImageSizeKB * 1000)
          && isRejectingOversizeImages())) {
        LOGGER.info("Time taken to fail image compression = seconds {}",
            (System.currentTimeMillis() - startTime) / 1000);
        throw new ImageCompressException("Image is too large for processing, "
            + "try with less quality or turn 'rejectingOversizeImages' off");
      }

      LOGGER.info("Successfully completed image compression, result = {} bytes", jpegData.length);
      LOGGER.info("Time taken to complete image compression = seconds {}",
          (System.currentTimeMillis() - startTime) / 1000);
      return jpegData;

    } catch (IOException | InterruptedException | IM4JavaException e) {
      LOGGER.info("Time taken to fail image compression = seconds {}",
          (System.currentTimeMillis() - startTime) / 1000);
      throw new ImageCompressException(e.getMessage());
    }
  }

  private byte[] compressUsingImageWriter(BufferedImage workingImage, String fileMimeType,
                                          int targetImageSizeB)
      throws IOException, ImageCompressException {
    BigDecimal compressionQuality = BigDecimal.valueOf(0.75);
    byte[] jpegData = compressImage(workingImage, compressionQuality.floatValue(), fileMimeType);
    if (jpegData.length < targetImageSizeB) {
      return jpegData;
    }
    LOGGER.info("initial file size (bytes) = {}", jpegData.length);

    double numberToTest = 1d;
    double max = 75;
    double lower = 0;
    while (max - lower >= 1) {
      double newNumberToTest = Math.round((max + lower) / (numberToTest == 1d ? 3d : 2d));
      newNumberToTest =
          newNumberToTest < 10 ? newNumberToTest : Math.round(newNumberToTest / 10) * 10;
      if (newNumberToTest == max) {
        break;
      }
      numberToTest = newNumberToTest == numberToTest ? max = lower : newNumberToTest;
      jpegData = compressImage(workingImage,
          BigDecimal.valueOf(numberToTest).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
              .floatValue(), fileMimeType);
      if (jpegData.length > targetImageSizeB) {
        max = numberToTest;
      } else {
        lower = numberToTest;
      }
      LOGGER.debug("jpg {}, quality {}", jpegData.length, numberToTest);
    }

    LOGGER.debug("jpg {}, compression {}", jpegData.length,
        BigDecimal.valueOf(numberToTest).divide(BigDecimal.valueOf(100)).floatValue());
    return jpegData;
  }

  private byte[] compressUsingImageMagick(byte[] bimg, int targetImageSizeKB)
      throws IOException, InterruptedException, IM4JavaException {
    double numberToTest = 1d;
    byte[] jpegData;
    jpegData = ImageUtils.compressImage(bimg, targetImageSizeKB);
    LOGGER.info("final jpg {}, quality {}", jpegData.length, numberToTest);
    return jpegData;

  }

  private byte[] compressImage(BufferedImage image, float compressionQuality, String fileMimeType)
      throws IOException {
    ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName(fileMimeType).next();
    ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
    jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    jpgWriteParam.setCompressionQuality(compressionQuality);

    try (ByteArrayOutputStream compressed = new ByteArrayOutputStream()) {
      try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed)) {
        jpgWriter.setOutput(outputStream);

        jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
        jpgWriter.dispose();
      }

      return compressed.toByteArray();
    }

  }

  private boolean isRejectingOversizeImages() {
    return rejectingOversizeImages;
  }
}
