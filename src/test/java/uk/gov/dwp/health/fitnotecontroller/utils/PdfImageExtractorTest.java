package uk.gov.dwp.health.fitnotecontroller.utils;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("squid:S1192") // allow string literals
public class PdfImageExtractorTest {

    @Test
    public void nullByteArrayFails() {
        try {
            PdfImageExtractor.extractImage(null, 300);
        } catch (ImagePayloadException e ) {
            assertThat("cannot accept null", e.getMessage(), is(equalTo("incoming byte array cannot be null")));
        } catch (Exception ignored){}
    }

    @Test
    public void passwordDocUnsuccessful() throws IOException, ImagePayloadException {
        try {
            PdfImageExtractor.extractImage(FileUtils.readFileToByteArray(new File("src/test/resources/password.pdf")), 300);
        } catch (InvalidPasswordException e ) {
            assertThat(e.getMessage(), is(equalTo("Cannot decrypt PDF, the password is incorrect")));
        }
    }

    @Test
    public void negativeDPIScanUnsuccessful() throws IOException, ImagePayloadException {
        assertNull("cannot have a negative dpi", PdfImageExtractor.extractImage(FileUtils.readFileToByteArray(new File("src/test/resources/FullPage_Portrait.pdf")), -1));
    }

    @Test
    public void jpegByteArrayFails() throws IOException, ImagePayloadException {
        assertNull("buffered image cannot be a pdf", PdfImageExtractor.extractImage(FileUtils.readFileToByteArray(new File("src/test/resources/FullPage_Portrait.jpg")), 300));
    }

    @Test
    public void otherByteArrayFails() throws IOException, ImagePayloadException {
        assertNull("script file cannot be a pdf", PdfImageExtractor.extractImage(FileUtils.readFileToByteArray(new File("src/test/resources/scripts/01_submitFitnote_TooSmall.sh")), 300));
    }

    @Test
    public void scannedPdfByteArrayIsSuccessfulAtOptimalDPI() throws IOException, ImagePayloadException {
        assertNotNull(PdfImageExtractor.extractImage(FileUtils.readFileToByteArray(new File("src/test/resources/FullPage_Portrait.pdf")), 300));
    }

    @Test
    public void scannedPdfByteArrayIsUnsuccessfulWithBadDPI() throws IOException, ImagePayloadException {
        assertNull(PdfImageExtractor.extractImage(FileUtils.readFileToByteArray(new File("src/test/resources/FullPage_Portrait.pdf")), 0));
    }

    @Test
    public void scannedPdfByteArrayIsUnsuccessfulWithHugeDPI() throws IOException, ImagePayloadException {
        assertNull(PdfImageExtractor.extractImage(FileUtils.readFileToByteArray(new File("src/test/resources/FullPage_Portrait.pdf")), 40000));
    }
}
