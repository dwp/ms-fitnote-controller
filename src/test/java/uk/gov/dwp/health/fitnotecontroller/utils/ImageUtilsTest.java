package uk.gov.dwp.health.fitnotecontroller.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import nu.pattern.OpenCV;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.im4java.core.IM4JavaException;
import org.junit.Before;
import org.junit.Test;
import uk.gov.dwp.health.fitnotecontroller.domain.DataMatrixResult;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.StringToMatch;


public class ImageUtilsTest extends ImageUtils {
    private static final String IMAGE_FILE = "/DarkPage.jpg";
    private static final String IMAGE_FILE_HEIC = "/DarkPage.heic";
    private static final String PDF_FILE = "/FullPage_Portrait.pdf";
    private static final String TEXT_FILE = "/test-fail-type.txt";
    private static final String DATA_MATRIX_FILE = "/datamatrix.png";
    private static final String EDGE_DETECTION_FILE = "/fitnotes/newSample/IMG_0009.JPG";
    private static final String CROP_FILE = "/fitnotes/newSample/IMG_0015_flipped.JPG";

    private BufferedImage localImage;

    private BufferedImage getTestImage(String file) throws IOException {
        String imageString = getEncodedImage(this.getClass().getResource(file).getPath());
        byte[] decode = org.apache.commons.codec.binary.Base64.decodeBase64(imageString);

        return getBufferedImage(decode);
    }

    private BufferedImage getBufferedImage(byte[] byteArray) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
        return ImageIO.read(inputStream);
    }

    private ImagePayload getImagePayload(String file) throws IOException {
        String imageString = getEncodedImage(this.getClass().getResource(file).getPath());
        ImagePayload payload = new ImagePayload();
        payload.setImage(imageString);
        return payload;
    }

    public byte[] getObjectAsByte(String file) throws IOException {
        String imageString = getEncodedImage(this.getClass().getResource(file).getPath());
        return org.apache.commons.codec.binary.Base64.decodeBase64(imageString);
    }

    @Before
    public void setUp() throws IOException {
        localImage = getTestImage(IMAGE_FILE);
        // needed for testing locally, please comment out when committing code
        // you need ImageMagick installed (https://imagemagick.org)
        // String myPath="/Users/dillon.vaghela/.homebrew/Cellar/imagemagick/7.1.1-4_1/bin";
        // ProcessStarter.setGlobalSearchPath(myPath);
        // -------------
        OpenCV.loadLocally();
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
        byte[] compressedImage = layerDMRegionOnImage(origImage, baos.toByteArray());
        assertNotEquals(compressedImage, origImage);
    }

    @Test
    public void validateImageDMOverlay() throws IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE);
        byte[] origImage = org.apache.commons.codec.binary.Base64.decodeBase64(payload.getImage());
        DataMatrixResult dataMatrixResult = new DataMatrixResult();
        String base64Img = getEncodedImage(this.getClass().getResource(DATA_MATRIX_FILE).getPath());
        dataMatrixResult.setFinalImage(base64Img);
        dataMatrixResult.setPosition(new Point(400,400));

        byte[] newImage = placeDMOnImage(origImage, dataMatrixResult, false);
        assertNotEquals(newImage, origImage);
    }

    @Test
    public void validateImageDMOverlaySmall() throws IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE);
        byte[] origImage = org.apache.commons.codec.binary.Base64.decodeBase64(payload.getImage());
        DataMatrixResult dataMatrixResult = new DataMatrixResult();
        BufferedImage image = new BufferedImage(50,50, 1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        dataMatrixResult.setFinalImage(Base64.encodeBase64String(baos.toByteArray()));
        dataMatrixResult.setMatchAngle(90);
        dataMatrixResult.setPosition(new Point(400,400));

        byte[] newImage = placeDMOnImage(origImage, dataMatrixResult, false);
        assertNotEquals(newImage, origImage);
    }

    @Test
    public void validateImageDMOverlayLarge() throws IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE);
        byte[] origImage = org.apache.commons.codec.binary.Base64.decodeBase64(payload.getImage());
        DataMatrixResult dataMatrixResult = new DataMatrixResult();
        BufferedImage image = new BufferedImage(300,300, 1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        dataMatrixResult.setFinalImage(Base64.encodeBase64String(baos.toByteArray()));
        dataMatrixResult.setMatchAngle(90);
        dataMatrixResult.setPosition(new Point(400,400));

        byte[] newImage = placeDMOnImage(origImage, dataMatrixResult, false);
        assertNotEquals(newImage, origImage);
    }

    @Test
    public void validateImageDMOverlayMediumPdf() throws IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE);
        byte[] origImage = org.apache.commons.codec.binary.Base64.decodeBase64(payload.getImage());
        DataMatrixResult dataMatrixResult = new DataMatrixResult();
        BufferedImage image = new BufferedImage(150,150, 1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        dataMatrixResult.setFinalImage(Base64.encodeBase64String(baos.toByteArray()));
        dataMatrixResult.setMatchAngle(90);
        dataMatrixResult.setPosition(new Point(400,400));

        byte[] newImage = placeDMOnImage(origImage, dataMatrixResult, true);
        assertNotEquals(newImage, origImage);
    }

    @Test
    public void testImageLength() throws IOException {
        ImagePayload payload = getImagePayload(IMAGE_FILE);
        int length = getImageLength(payload);
        byte[] decode = org.apache.commons.codec.binary.Base64.decodeBase64(payload.getImage());
        assertEquals(decode.length, length);
    }

    @Test
    public void convertImageByteJPG() throws IOException, InterruptedException, IM4JavaException {
        byte[] response  = convertImage(getObjectAsByte(IMAGE_FILE), 100d);
        String mimeType = getImageMimeType(response);
        assertEquals("jpg", mimeType);
    }

    @Test
    public void convertImageHEIC() throws IOException, InterruptedException, IM4JavaException {
        byte[] response  = convertImage(getObjectAsByte(IMAGE_FILE_HEIC), 100d);
        String mimeType = getImageMimeType(response);
        assertEquals("jpg", mimeType);
    }

    @Test
    public void convertImageJPG() throws IOException, InterruptedException, IM4JavaException {
        byte[] convertedImage = convertImage(localImage, 100d);
        String mimeType = getImageMimeType(convertedImage);
        assertEquals("jpg", mimeType);
    }

    @Test
    public void testImageCompression() throws IOException, InterruptedException, IM4JavaException {
        byte[] image  = getObjectAsByte(IMAGE_FILE);
        int targetSize = 1000;
        byte[] response  = compressImage(image, targetSize);
        assertTrue(image.length > response.length);
        assertTrue(response.length < 1000 * 1000);
    }

    @Test
    public void convertTxtFail() throws IOException, InterruptedException, IM4JavaException {
        byte[] response = convertImage(getObjectAsByte(TEXT_FILE), 100d);
        assertEquals(null, response);
    }

    @Test
    public void calculatedPixel() throws IOException, IM4JavaException {
        int pixel = calculateMegapixel(getObjectAsByte(IMAGE_FILE));
        assertEquals(13, pixel);
    }

    @Test
    public void rotateImage() {
        BufferedImage rotatedImage = createRotatedCopy(localImage, 90);
        assertEquals(36578304, rotatedImage.getData().getDataBuffer().getSize());
    }

    @Test
    public void rotateImageBytes() throws IOException {
        byte[] image = getObjectAsByte(IMAGE_FILE);
        byte[] rotatedImage = createRotatedCopy(image, 90);
        assertEquals(1760716, rotatedImage.length);
    }

    @Test
    public void testImageLandscape() {
        assertTrue(isLandscape(localImage));
    }

    @Test
    public void testImagePortrait() {
        BufferedImage rotatedImage = createRotatedCopy(localImage, 90);
        assertFalse(isLandscape(rotatedImage));
    }

    @Test
    public void testCompressImage1MB() throws IOException, InterruptedException, IM4JavaException {
        byte[] image = getObjectAsByte(IMAGE_FILE);
        int length = Objects.requireNonNull(compressImage(image, 1000)).length;
        assertTrue(length < (1000 * 1000));
    }

    @Test
    public void testCompressImage500KB() throws IOException, InterruptedException, IM4JavaException {
        byte[] image = getObjectAsByte(IMAGE_FILE);
        int length = Objects.requireNonNull(compressImage(image, 500)).length;
        assertTrue(length < (500 * 1000));
    }

    @Test
    public void testCropImage() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        BufferedImage image = getTestImage(CROP_FILE);
        ImagePayload payload = getImagePayload(CROP_FILE);
        ExpectedFitnoteFormat fitnoteFormat = new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.SUCCESS, null);
        fitnoteFormat.setFinalImage(image);
        fitnoteFormat.setTopHeight(504);
        fitnoteFormat.setBottomHeight(2520);
        Map<ExpectedFitnoteFormat.StringLocation, StringToMatch> strings =
                (Map<ExpectedFitnoteFormat.StringLocation, StringToMatch>) getMatchingStringsMethod().invoke(fitnoteFormat);
        StringToMatch stringToMatch = new StringToMatch(null);
        strings.put(ExpectedFitnoteFormat.StringLocation.TOP_LEFT, stringToMatch);
        strings.put(ExpectedFitnoteFormat.StringLocation.BASE_RIGHT, stringToMatch);
        stringToMatch.setupPercentage(100);
        strings.put(ExpectedFitnoteFormat.StringLocation.TOP_RIGHT, stringToMatch);
        stringToMatch.setupPercentage(61);
        strings.put(ExpectedFitnoteFormat.StringLocation.BASE_LEFT, stringToMatch);


        byte[] newImage = ImageUtils.cropOrExtractImage(image, fitnoteFormat,
                payload, image.getHeight()/6, 70);
        BufferedImage cropImage = getBufferedImage(newImage);
        assertEquals(4032, cropImage.getWidth());
        assertEquals(2520, cropImage.getHeight());
    }

    public static Method getMatchingStringsMethod() throws NoSuchMethodException {
        Method method = ExpectedFitnoteFormat.class.getDeclaredMethod("getMatchingStrings");
        method.setAccessible(true);
        return method;
    }

    private String getEncodedImage(String imageFileName) throws IOException {
        File file = new File(imageFileName);
        return Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
    }
}
