package uk.gov.dwp.health.fitnotecontroller.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class PdfImageExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(PdfImageExtractor.class.getName());

  private PdfImageExtractor() {
  }

  public static byte[] extractImage(byte[] incomingObject, int scanDPI)
          throws ImagePayloadException, IOException {
    byte[] extractedImage = null;
    PDDocument document = null;

    if (incomingObject == null) {
      throw new ImagePayloadException("incoming byte array cannot be null");
    }
    if (scanDPI <= 0) {
      LOG.debug("scanDPI must be a positive integer, {} is not valid", scanDPI);
      return extractedImage;
    }

    try {

      LOG.debug("loading and rendering pdf in order to copy page to image");
      document = Loader.loadPDF(incomingObject);
      PDFRenderer renderer = new PDFRenderer(document);

      BufferedImage imageBuffer = renderer.renderImageWithDPI(0, scanDPI, ImageType.RGB);
      LOG.info("scanned page 1 to BufferedImage at {} DPI", scanDPI);

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        ImageIO.write(imageBuffer, "jpg", outputStream);
        extractedImage = outputStream.toByteArray();
      }

    } catch (InvalidPasswordException e) {
      LOG.error(e.getMessage());
      throw e;

    } catch (IOException e) {
      LOG.error("Incoming byte array is not a pdf file :: {}", e.getMessage());
      LOG.debug(e.getClass().getName(), e);

    } catch (IllegalArgumentException e) {
      LOG.error("Error with pdf scanning operation :: {}", e.getMessage());
      LOG.debug(e.getClass().getName(), e);

    } finally {
      if (document != null) {
        document.close();
      }
    }

    return extractedImage;
  }
}
