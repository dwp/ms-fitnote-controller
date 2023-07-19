package uk.gov.dwp.health.fitnotecontroller.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.im4java.core.IM4JavaException;
import org.junit.Before;
import org.junit.Test;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageTransformException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ImageUtilsTest extends ImageUtils {
    private static final String IMAGE_FILE = "/DarkPage.jpg";
    private static final String IMAGE_FILE_HEIC = "/DarkPage.heic";
    private static final String PDF_FILE = "/FullPage_Portrait.pdf";
    private static final String TEXT_FILE = "/test-fail-type.txt";

    private BufferedImage localImage;

    private BufferedImage getTestImage(String file) throws IOException {
        String imageString = getEncodedImage(this.getClass().getResource(file).getPath());
        byte[] decode = org.apache.commons.codec.binary.Base64.decodeBase64(imageString);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(decode);
        return ImageIO.read(inputStream);
    }

    private ImagePayload getImagePayload(String file) throws IOException {
        String imageString = getEncodedImage(this.getClass().getResource(file).getPath());
        ImagePayload payload = new ImagePayload();
        payload.setImage(imageString);
        return payload;
    }


    @Before
    public void setUp() throws IOException {
        localImage = getTestImage(IMAGE_FILE);
        // needed for testing locally, please comment out when committing code
        // you need ImageMagick installed (https://imagemagick.org)
        // String myPath="imagemagickinstalledbinlocation";
        //ProcessStarter.setGlobalSearchPath(myPath);
        // -------------
    }

    @Test
    public void validateChangeBrightness() {
        int brightness = gatherBrightness(localImage, 0);
        BufferedImage alteredLocalImage = changeBrightness(localImage, 1.1f);
        int brighter = gatherBrightness(alteredLocalImage, 0);
        assertTrue(String.format("%d < %d", brightness, brighter), brightness < brighter);
    }

    @Test
    public void validateNoExceptionWhenChangeBrightnessWithNegativeValue() {
        int origBrightness = gatherBrightness(localImage, 0);

        BufferedImage updatedImage = changeBrightness(localImage, -10);
        int newBrightness = gatherBrightness(updatedImage, 0);

        assertTrue(origBrightness > newBrightness);
    }

    @Test
    public void validateNormaliseBrightness() {
        int brightness = gatherBrightness(localImage, 0);
        BufferedImage alteredLocalImage = normaliseBrightness(localImage, brightness + 10, 0);//
        assertTrue(brightness < gatherBrightness(alteredLocalImage, 0));
    }

    @Test
    public void validateGatherBrightness() {
        int brightness = gatherBrightness(localImage, 0);
        assertTrue(brightness > 0);
        assertTrue(256 > brightness);
    }

    @Test
    public void validateFormatGrayScale() {
        BufferedImage alteredLocalImage = formatGrayScale(localImage);
        assertTrue(localImage.getColorModel().getColorSpace().isCS_sRGB());
        assertFalse(alteredLocalImage.getColorModel().getColorSpace().isCS_sRGB());
    }

    @Test
    public void testMimeTypeJPG() throws IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE);
        String fileMimeType = getImageMimeType(payload);
        assertEquals("jpg", fileMimeType);
    }

    @Test
    public void testMimeTypePDF() throws IOException {
        ImagePayload payload = getImagePayload(PDF_FILE);
        String fileMimeType = getImageMimeType(payload);
        assertEquals("pdf", fileMimeType);
    }

    @Test
    public void validateImageDMRegion() throws IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE);
        byte[] origImage = org.apache.commons.codec.binary.Base64.decodeBase64(payload.getImage());
        BufferedImage updatedImage = changeBrightness(localImage, -10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(updatedImage, "jpg", baos);
        byte[] compressedImage = layerDMRegionOnImage(origImage, baos.toByteArray(), 0);
        assertNotEquals(compressedImage, origImage);
    }

    @Test
    public void testImageLength() throws IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE);
        int length = getImageLength(payload);
        byte[] decode = org.apache.commons.codec.binary.Base64.decodeBase64(payload.getImage());
        assertEquals(decode.length, length);
    }

    @Test
    public void convertImagePayloadJPG() throws ImageTransformException, IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE);
        convertImage(payload, 100d, "jpg");
        String mimeType = getImageMimeType(payload);
        assertEquals("jpg", mimeType);
    }

    @Test
    public void convertImagePayloadHEIC() throws ImageTransformException, IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE_HEIC);
        convertImage(payload, 100d, "heic");
        String mimeType = getImageMimeType(payload);
        assertEquals("jpg", mimeType);
    }

    @Test
    public void convertImageJPG() throws IOException, InterruptedException, IM4JavaException {
        byte[] convertedImage = convertImage(localImage, 100d);
        String mimeType = getImageMimeType(convertedImage);
        assertEquals("jpg", mimeType);
    }

    @Test
    public void convertTxtFail() throws IOException {
        ImagePayload payload = getImagePayload(TEXT_FILE);
        try {
            convertImage(payload, 100d, "txt");
            fail("should have thrown an error");

        } catch (ImageTransformException e) {
            assertThat("expecting a custom exception", e.getMessage(), containsString("Failed to convert image to jpg from"));
        }
    }

    private String getEncodedImage(String imageFileName) throws IOException {
        File file = new File(imageFileName);
        return Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
    }
}
