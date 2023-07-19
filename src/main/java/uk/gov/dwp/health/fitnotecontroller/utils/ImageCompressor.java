package uk.gov.dwp.health.fitnotecontroller.utils;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.im4java.core.IM4JavaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;

public class ImageCompressor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageCompressor.class.getName());
  private boolean rejectingOversizeImages;

  public ImageCompressor(FitnoteControllerConfiguration config) {
    this.rejectingOversizeImages = config.isRejectingOversizeImages();
  }

  public byte[] compressBufferedImage(String fileMimeType,
                                      BufferedImage inputImage, int targetImageSizeKB,
                                      boolean useGrayScale)
          throws ImageCompressException {
    LOGGER.info(
            "Starting image compression :: "
                    + "RejectingOversizedImage = {}, TargetSizeKB = {}, GreyScale = {}",
            isRejectingOversizeImages(),
            targetImageSizeKB,
            useGrayScale);
    BufferedImage workingImage = useGrayScale ? turnGreyscale(inputImage) : inputImage;
    BigDecimal compressionQuality = BigDecimal.valueOf(1);
    try {

      byte[] jpegData = compressImage(workingImage, compressionQuality.floatValue(), fileMimeType,
              null);
      LOGGER.info("initial file size (bytes) = {}", jpegData.length);

      while (jpegData.length > (targetImageSizeKB * 1000)
              && compressionQuality.doubleValue() > 0) {

        if (compressImage(workingImage, Double.valueOf(0.01).floatValue(), fileMimeType,
                jpegData).length > (targetImageSizeKB * 1000)) {
          break;
        }

        if (compressionQuality.doubleValue() > 0.1) {
          compressionQuality = compressionQuality.subtract(BigDecimal.valueOf(0.1));
        } else {
          compressionQuality = compressionQuality.subtract(BigDecimal.valueOf(0.01));
        }
        try {
          jpegData = compressImage(workingImage, compressionQuality.floatValue(), fileMimeType,
                  jpegData);
          LOGGER.debug("jpg {}, compression {}", jpegData.length, compressionQuality.floatValue());
        } catch (IOException | ImageCompressException e) {
          LOGGER.debug(e.getClass().getName(), e);
          LOGGER.info("failed to compress, will attempt to use ImageMagick");
          break;
        }

        LOGGER.debug("jpg {}, compression {}", jpegData.length, compressionQuality.floatValue());
        LOGGER.debug("Failed to compress: {}", jpegData.length > targetImageSizeKB * 1000);
      }

      if (jpegData.length > (targetImageSizeKB * 1000)
              && isRejectingOversizeImages()) {
        jpegData = compressUsingImageMagick(jpegData, targetImageSizeKB);
      }

      if (jpegData.length > (targetImageSizeKB * 1000) && isRejectingOversizeImages()) {
        throw new ImageCompressException(
                "Image is too large for processing, "
                        + "try with less quality or turn 'rejectingOversizeImages' off");
      }

      LOGGER.info("Successfully completed image compression, result = {} bytes", jpegData.length);
      return jpegData;

    } catch (IOException | InterruptedException | IM4JavaException e) {
      throw new ImageCompressException(e.getMessage());
    }
  }

  private byte[] compressUsingImageMagick(byte[] bimg, int targetImageSizeKB) throws IOException,
          InterruptedException, IM4JavaException {
    double max = 100;
    double lower = 0;
    double numberToTest = 1d;
    byte[] jpegData;
    jpegData = ImageUtils.convertImage(bimg, numberToTest);
    if (jpegData.length > (targetImageSizeKB * 1000)) {
      LOGGER.info("final jpg {}, quality {}", jpegData.length, numberToTest);
      return jpegData;
    }
    while (max - lower >= 1) {
      double newNumberToTest = Math.round((max + lower) / 2d);
      numberToTest = newNumberToTest == numberToTest ? max = lower : newNumberToTest;
      jpegData = ImageUtils.convertImage(bimg, numberToTest);
      if (jpegData.length > (targetImageSizeKB * 1000)) {
        max = numberToTest;
      } else {
        lower = numberToTest;
      }
      LOGGER.debug("jpg {}, quality {}", jpegData.length, numberToTest);
    }
    LOGGER.info("final jpg {}, quality {}", jpegData.length, numberToTest);
    return jpegData;

  }

  private byte[] compressImage(BufferedImage image, float compressionQuality, String fileMimeType,
                               byte[] jpegData) throws IOException, ImageCompressException {
    ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName(fileMimeType).next();
    ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
    jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    String[] compressionTypeList = jpgWriteParam.getCompressionTypes();
    ByteArrayOutputStream compressed = new ByteArrayOutputStream();
    int i = 0;
    while (i < compressionTypeList.length) {
      jpgWriteParam.setCompressionType(compressionTypeList[i]);
      jpgWriteParam.setCompressionQuality(compressionQuality);

      try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed)) {
        jpgWriter.setOutput(outputStream);
        jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
        if (jpegData == null || compressed.toByteArray().length
                < jpegData.length) {
          break;
        } else if (i + 1 == compressionTypeList.length) {
          throw new ImageCompressException(
                  "Image is too large for processing, "
                          + "attempt to compress using ImageMagick");
        }
      } catch (IOException ioException) {
        LOGGER.error("failed compression method: {}", ioException);
        //attempt another compression method
      }

      i++;
    }

    return  compressed.toByteArray();

  }

  private BufferedImage turnGreyscale(BufferedImage inputImage) {
    BufferedImage image =
            new BufferedImage(
                    inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    Graphics g = image.getGraphics();

    g.drawImage(inputImage, 0, 0, null);
    g.dispose();

    return image;
  }

  private boolean isRejectingOversizeImages() {
    return rejectingOversizeImages;
  }
}
