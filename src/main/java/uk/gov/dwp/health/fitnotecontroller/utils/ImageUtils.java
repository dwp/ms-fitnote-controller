package uk.gov.dwp.health.fitnotecontroller.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.tika.Tika;
import org.im4java.core.CommandException;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.core.Stream2BufferedImage;
import org.im4java.process.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.domain.DataMatrixResult;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class ImageUtils {

  private static final Logger LOGGER =
          LoggerFactory.getLogger(ImageUtils.class.getName());


  protected ImageUtils() {
  }

  protected static BufferedImage changeBrightness(BufferedImage src, float val) {
    RescaleOp brighterOp = new RescaleOp(val, 0, null);
    return brighterOp.filter(src, null);
  }

  public static BufferedImage formatGrayScale(BufferedImage inputImage) {
    BufferedImage image =
        new BufferedImage(
            inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    Graphics g = image.getGraphics();

    g.drawImage(inputImage, 0, 0, null);
    g.dispose();

    return image;
  }

  protected static BufferedImage normaliseBrightness(
      BufferedImage inputImage, int targetBrightness, int loseBorderPercentage) {
    int currentBrightness = gatherBrightness(inputImage, loseBorderPercentage);
    if (0 == currentBrightness) {
      currentBrightness++;
    }
    return changeBrightness(inputImage, (float) targetBrightness / currentBrightness);
  }

  /**
   * A function that generates an average of the pixel number. Written for grayscale image closer to
   * 255 = brighter. this will lose the top & bottom 10% as well as the left & right 10% so that the
   * calculation ignores erroneous borders
   *
   * @param inputImage - image to scan
   * @return average brightness of a pixel
   */
  protected static int gatherBrightness(BufferedImage inputImage, int loseBorderPercentage) {
    Raster pixelData = inputImage.getRaster();
    int count = 0;
    int pixelCount = 1;

    // generate offset to ignore edges
    int widthOffSet = (loseBorderPercentage == 0) ? 0 : pixelData.getWidth() / loseBorderPercentage;
    int heightOffSet =
        (loseBorderPercentage == 0) ? 0 : pixelData.getHeight() / loseBorderPercentage;

    // loop through remaining data to get an average - hopefully all document
    for (int x = widthOffSet; x < (pixelData.getWidth() - widthOffSet); x++) {
      for (int y = heightOffSet; y < (pixelData.getHeight() - heightOffSet); y++) {
        count += pixelData.getSample(x, y, 0);
        pixelCount++;
      }
    }
    // return an average of the pixelData
    return pixelCount == 0 ? 0 : count / pixelCount;
  }

  public static BufferedImage increaseContrast(BufferedImage inputImage, int contrastCutOff) {
    BufferedImage contrastImage = deepCopyImage(inputImage);

    Raster pixelData = contrastImage.getRaster();
    for (int x = 0; x < (pixelData.getWidth()); x++) {
      for (int y = 0; y < (pixelData.getHeight()); y++) {
        if (pixelData.getSample(x, y, 0) < contrastCutOff) {
          contrastImage.setRGB(x, y, Color.BLACK.getRGB());
        } else {
          contrastImage.setRGB(x, y, Color.WHITE.getRGB());
        }
      }
    }
    // return an average of the pixelData
    return contrastImage;
  }

  public static byte[] createRotatedCopy(byte[] img, double angleDegrees) throws IOException {
    BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(img));
    BufferedImage rotatedImage = createRotatedCopy(bimg, angleDegrees);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(rotatedImage, "jpg", baos);
    return baos.toByteArray();
  }

  public static BufferedImage createRotatedCopy(BufferedImage img, double angleDegrees) {
    double sin = Math.abs(Math.sin(Math.toRadians(angleDegrees)));
    double cos = Math.abs(Math.cos(Math.toRadians(angleDegrees)));
    int newWidth = (int) Math.floor(img.getWidth() * cos + img.getHeight() * sin);
    int newHeight = (int) Math.floor(img.getHeight() * cos + img.getWidth() * sin);
    int imageType = img.getType();
    if (imageType == 0) {
      imageType = TYPE_INT_RGB;
    }
    BufferedImage result = new BufferedImage(newWidth, newHeight, imageType);


    Graphics2D g = result.createGraphics();
    g.setBackground(Color.WHITE);
    g.clearRect(0, 0, newWidth, newHeight);

    g.translate((newWidth - img.getWidth()) / 2, (newHeight - img.getHeight()) / 2);
    g.rotate(
        Math.toRadians(angleDegrees), (double) img.getWidth() / 2, (double) img.getHeight() / 2);
    g.drawRenderedImage(img, null);
    g.dispose();

    return result;
  }

  private static BufferedImage deepCopyImage(BufferedImage sourceImg) {
    WritableRaster raster =
        sourceImg.copyData(sourceImg.getRaster().createCompatibleWritableRaster());
    return new BufferedImage(
        sourceImg.getColorModel(), raster, sourceImg.getColorModel().isAlphaPremultiplied(), null);
  }

  public static String getImageMimeType(ImagePayload payload) {
    byte[] decodedPayload = Base64.decodeBase64(payload.getImage());
    return getImageMimeType(decodedPayload);
  }

  public static String getImageMimeType(byte[] image) {
    Tika tika = new Tika();
    String fileMimeType = tika.detect(image);
    if (fileMimeType == null) {
      return null;
    }
    fileMimeType = fileMimeType.substring(fileMimeType.indexOf('/') + 1);
    if (fileMimeType.equals("jpeg")) {
      fileMimeType = "jpg";
    }
    return fileMimeType;
  }

  public static byte[] convertImage(byte[] jpegData, double quality)
          throws IOException, InterruptedException, IM4JavaException {
    final long startTime = System.currentTimeMillis();
    try {
      IMOperation op = new IMOperation();
      op.addImage("-");
      op.quality(quality);
      op.strip();
      op.addImage("jpg:-");               // write to stdout in tif-format

      // set up pipe(s): you can use one or two pipe objects
      ByteArrayInputStream fis = new ByteArrayInputStream(jpegData);
      ByteArrayOutputStream fos = new ByteArrayOutputStream();

      Pipe pipeIn  = new Pipe(fis, null);
      Pipe pipeOut = new Pipe(null, fos);

      // set up command
      ConvertCmd convert = new ConvertCmd();
      convert.setInputProvider(pipeIn);
      convert.setOutputConsumer(pipeOut);
      LOGGER.info("ImageMagick command: {}", op);
      convert.run(op);
      fis.close();
      fos.close();

      LOGGER.info("Time taken to convert image = seconds {}",
              (System.currentTimeMillis() - startTime) / 1000);
      return fos.toByteArray();
    } catch (CommandException e) {
      LOGGER.error("Image too large to convert");
      return null;
    }
  }

  public static byte[] convertImage(BufferedImage image, double quality)
          throws IOException, InterruptedException, IM4JavaException {
    final long startTime = System.currentTimeMillis();
    IMOperation op = new IMOperation();
    op.addImage();                   // read from stdin
    op.quality(quality);
    op.strip();
    op.addImage("jpg:-");               // write to stdout in tif-format

    // set up pipe(s): you can use one or two pipe objects
    ByteArrayOutputStream fos = new ByteArrayOutputStream();

    Pipe pipeOut = new Pipe(null, fos);

    // set up command
    ConvertCmd convert = new ConvertCmd();
    convert.setOutputConsumer(pipeOut);
    LOGGER.info("ImageMagick command: {}", op);
    convert.run(op, image);
    fos.close();

    LOGGER.info("Time taken to convert image = seconds {}",
            (System.currentTimeMillis() - startTime) / 1000);
    return fos.toByteArray();
  }

  public static int getImageLength(ImagePayload payload) {
    byte[] jpegData = Base64.decodeBase64(payload.getImage());
    return jpegData.length;
  }

  public static byte[] placeDMOnImage(byte[] compressedImage, DataMatrixResult dataMatrixResult,
                                      boolean isPdf) {
    try {
      BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(compressedImage));
      int wid = bimg.getWidth();
      int height = bimg.getHeight();
      //create a new buffer and draw two image into the new image
      BufferedImage newImage = new BufferedImage(wid, height,
              BufferedImage.TYPE_BYTE_GRAY);
      Graphics2D g2 = drawOriginalImage(compressedImage, newImage);

      byte[] dataMatrixBytes = Base64.decodeBase64(dataMatrixResult.getFinalImage());
      InputStream is = new ByteArrayInputStream(dataMatrixBytes);
      BufferedImage dataMatrix =  ImageIO.read(is);
      dataMatrix = clearUpImage(dataMatrix, isPdf, dataMatrixResult.getMatchAngle());

      Point point = dataMatrixResult.getPosition();
      g2.drawImage(dataMatrix, null, point.x, point.y);
      LOGGER.info("Draw data matrix on top of original image with x: {} and y: {}", point.x,
              point.y);

      return convertToJpg(newImage, g2);
    } catch (Exception e) {
      LOGGER.error("Failed to create dm and place on top of image: {}", e);
    }
    return compressedImage;

  }

  private static Graphics2D drawOriginalImage(byte[] compressedImage, BufferedImage newImage)
          throws IOException {
    BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(compressedImage));
    int wid = bimg.getWidth();
    int height = bimg.getHeight();
    Graphics2D g2 = newImage.createGraphics();
    Color oldColor = g2.getColor();
    //fill background
    g2.setPaint(Color.WHITE);
    g2.fillRect(0, 0, wid, height);
    //draw image
    g2.setColor(oldColor);
    g2.drawImage(bimg, null, 0, 0);
    LOGGER.info("draw original image using width: {} and height: {}", wid, height);
    return g2;
  }

  private static byte[] convertToJpg(BufferedImage newImage, Graphics2D g2) throws IOException {
    g2.dispose();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    BufferedImage imageJPG = new BufferedImage(newImage.getWidth(), newImage.getHeight(),
            BufferedImage.TYPE_BYTE_GRAY);
    imageJPG.createGraphics().drawImage(newImage, 0, 0, Color.BLACK, null);
    ImageIO.write(imageJPG, "jpg", baos);
    return baos.toByteArray();
  }

  public static byte[] layerDMRegionOnImage(byte[] compressedImage, byte[] origImage,
                                            int angle) {
    try {
      BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(compressedImage));
      int wid = bimg.getWidth();
      int height = bimg.getHeight();
      //create a new buffer and draw two image into the new image
      BufferedImage newImage = new BufferedImage(wid, height,
              BufferedImage.TYPE_BYTE_GRAY);
      Graphics2D g2 = drawOriginalImage(compressedImage, newImage);
      BufferedImage origBImg = ImageIO.read(new ByteArrayInputStream(origImage));
      if (angle > 0) {
        origBImg = createRotatedCopy(origBImg, angle);
      }

      cropOrigImage(g2, origBImg);

      return convertToJpg(newImage, g2);
    } catch (IOException e) {
      LOGGER.error("IOException :: {}", e.getMessage());
      LOGGER.info("failed to layer image on top, will continue with compressed image");
    }

    return compressedImage;
  }

  private static void cropOrigImage(Graphics2D g2, BufferedImage bimgg)
          throws IOException {

    int height = bimgg.getHeight();
    int width = bimgg.getWidth();

    BufferedImage croppedImage = bimgg
            .getSubimage(0, height * 3 / 4, width / 2, height / 4);
    g2.drawImage(croppedImage, null, 0, height * 3 / 4);

    LOGGER.info("Draw image on top of original image with x: {} and y: {}", 0, height * 3 / 4);


  }

  public static int calculateMegapixel(byte[] jpegData) throws InfoException {
    Info info = new Info("-", new ByteArrayInputStream(jpegData), true);
    int height = info.getImageHeight();
    int width = info.getImageWidth();

    BigDecimal result = new BigDecimal(height)
            .multiply(new BigDecimal(width))
            .divide(new BigDecimal(1000000));
    int megapixel = result.setScale(0, RoundingMode.UP).intValue();
    LOGGER.debug("Image megapixel {}", megapixel);
    return megapixel;
  }

  private static BufferedImage clearUpImage(BufferedImage image, boolean isPdf, Integer angle)
          throws IOException, InterruptedException, IM4JavaException {
    final long startTime = System.currentTimeMillis();
    IMOperation op = new IMOperation();
    op.addImage();                   // read from stdin
    op.quality(90d);
    op.background("transparent");

    if (!isPdf && angle > 0d) {
      op.rotate(angle.doubleValue());
    }
    op.strip();
    int imageWidth = image.getWidth();
    if (imageWidth >= 300 & !isPdf && angle > 0d) {
      op.alpha("set");
      op.virtualPixel("transparent");
      op.channel("A");
      double blurPerc = 0.01;
      int blurRadus = (int) Math.floor(imageWidth * blurPerc);
      op.morphology("Distance", "Euclidean:1," + blurRadus + "!");
      op.p_channel();
    }
    op.addImage("png:-");               // write to stdout in tif-format

    // set up command
    ConvertCmd convert = new ConvertCmd();
    Stream2BufferedImage s2b = new Stream2BufferedImage();
    convert.setOutputConsumer(s2b);
    LOGGER.info("ImageMagick command: {}", op);
    convert.run(op, image);

    LOGGER.info("Time taken to convert image = seconds {}",
            (System.currentTimeMillis() - startTime) / 1000);
    return s2b.getImage();
  }

}
