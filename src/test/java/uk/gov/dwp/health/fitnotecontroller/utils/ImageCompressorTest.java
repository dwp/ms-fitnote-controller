package uk.gov.dwp.health.fitnotecontroller.utils;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImageCompressorTest {
    private static final int TARGET_IMAGE_SIZE = 500;
    private static BufferedImage baseImageNormal;
    private static byte[] standardPdfDoc;

    @Mock
    private FitnoteControllerConfiguration config;

    @BeforeClass
    public static void init() throws IOException {
        // needed for testing locally, please comment out when committing code
        // you need ImageMagick installed (https://imagemagick.org)
        // String myPath="imagemagickinstalledbinlocation";
        //ProcessStarter.setGlobalSearchPath(myPath);
        // -------------
        standardPdfDoc = FileUtils.readFileToByteArray(new File("src/test/resources/FullPage_Portrait.pdf"));
        baseImageNormal = ImageIO.read(new File("src/test/resources/FullPage_Portrait.jpg"));
    }

    @Test
    public void testWithGreyScaleNormal() throws ImageCompressException, IOException {
        writeToFile("FullPage_Portrait.jpg", "CompressorTest_GS.jpg", true);
    }

    @Test
    public void testWithColourNormal() throws ImageCompressException, IOException {
        writeToFile("FullPage_Portrait.jpg", "CompressorTest_Colour.jpg", false);
    }

    @Test
    public void testWithColourNormalPngImageMagick() throws ImageCompressException, IOException {
        writeToFile("CompressorTest_Colour.png", "CompressorTest_Colour.png", false);
    }

    private void writeToFile(String inputFileName, String ouputFileName, boolean useGreyScale) throws ImageCompressException, IOException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        String filePath = "src/test/resources/" + ouputFileName;

        ImageCompressor instance = new ImageCompressor(config);
        String fileMimeType = inputFileName.substring(inputFileName.indexOf(".") + 1);
        BufferedImage buffImg = ImageIO.read(new File("src/test/resources/" + inputFileName));
        FileUtils.writeByteArrayToFile(new File("src/test/resources/" + ouputFileName),
                instance.compressBufferedImage(fileMimeType, buffImg, TARGET_IMAGE_SIZE, useGreyScale));

        File file = new File(filePath);
        assertTrue(file.exists());
    }

    @Test
    public void testWithColourHiResReject() {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        try {
            instance.compressBufferedImage("jpg", baseImageNormal, 100, false);
            fail("should have thrown an error");

        } catch (ImageCompressException e) {
            assertThat("expecting a custom exception", e.getMessage(), containsString("Image is too large for processing"));
        }
    }

    @Test
    public void successWithColourHiResUsingImageMagick() throws ImageCompressException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        assertNotNull(instance.compressBufferedImage("jpg",
                baseImageNormal,
                200, false));

    }

    @Test
    public void successWithScanPdfDPI150() throws ImagePayloadException, IOException, ImageCompressException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        assertNotNull(instance.compressBufferedImage("jpg",
                ImageIO.read(
                        new ByteArrayInputStream(PdfImageExtractor.extractImage(standardPdfDoc, 150))),
                500, false));

    }

    @Test
    public void successWithScanPdfDPI300() throws ImagePayloadException, IOException, ImageCompressException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        assertNotNull(instance.compressBufferedImage("jpg",
                ImageIO.read(
                        new ByteArrayInputStream(PdfImageExtractor.extractImage(standardPdfDoc, 300))),
                500, false));

    }

    @Test
    public void successWithScanPdfDPI600UsingImageMagick() throws ImagePayloadException, IOException, ImageCompressException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        assertNotNull(instance.compressBufferedImage("jpg",
                ImageIO.read(
                        new ByteArrayInputStream(PdfImageExtractor.extractImage(standardPdfDoc, 600))),
                600, true));

    }

    @Test
    public void failureWithScanPdfDPI600() throws ImagePayloadException, IOException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        try {

            instance.compressBufferedImage("jpg", ImageIO.read(new ByteArrayInputStream(PdfImageExtractor.extractImage(standardPdfDoc, 600))), 280, false);
            fail("should have thrown an error");

        } catch (ImageCompressException e) {
            assertThat("expecting a custom exception", e.getMessage(), containsString("Image is too large for processing"));
        }
    }
}
