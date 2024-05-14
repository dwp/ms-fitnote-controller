package uk.gov.dwp.health.fitnotecontroller.handlers;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.codec.binary.Base64;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.DataMatrixResult;
import uk.gov.dwp.tls.TLSConnectionBuilder;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.awt.Point;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MsDataMatrixCreatorHandlerTest {

  private TLSConnectionBuilder connectionBuilder;

  @Mock
  FitnoteControllerConfiguration mockConfiguration;

  @Rule
  public WireMockRule dataMatrixService = new WireMockRule(wireMockConfig().port(3099));

  @Before
  public void setup() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, InvalidKeyException {
    when(mockConfiguration.getDataMatrixCreatorServiceUrl()).thenReturn("http://localhost:3099/generate-data-matrix");

    connectionBuilder = new TLSConnectionBuilder(null, null);

    dataMatrixService.start();
  }

  @Test
  public void testSuccessfulHtmlToPdf() throws IOException {
    MsDataMatrixCreatorHandler instance = new MsDataMatrixCreatorHandler(connectionBuilder, mockConfiguration);

    String returnValue = "{\"finalImage\":\"image123\",\"position\":{\"x\":1317.0,\"y\":2085.0}}";


    dataMatrixService.stubFor(post(urlEqualTo("/generate-data-matrix"))
                                     .willReturn(aResponse()
                                                         .withBody(returnValue)
                                                         .withStatus(200)
                                     ));

    DataMatrixResult response = instance.generateBase64DataMatrixFromImage("123", "image", false);

    Point position = new Point(1317, 2085);
    assertThat(response.getFinalImage(), is(equalTo("image123")));
    assertThat(response.getPosition(), is(equalTo(position)));
    assertThat(response.getMatchAngle(), is(equalTo(0)));
  }

  @Test
  public void testFailure404WithHtmlToPdf() {
    MsDataMatrixCreatorHandler instance = new MsDataMatrixCreatorHandler(connectionBuilder, mockConfiguration);

    dataMatrixService.stubFor(post(urlEqualTo("/generate-data-matrix"))
                                     .willReturn(aResponse()
                                                         .withStatus(404)
                                     ));

    try {
      instance.generateBase64DataMatrixFromImage("123", "image", false);
      fail("should throw exception");

    } catch (IOException e) {
      assertTrue(e.getMessage().startsWith("MsDataMatrixCreator Failure with StatusCode 404"));
    }
  }

  @Test
  public void testFailure500WithHtmlToPdf() {
    MsDataMatrixCreatorHandler instance = new MsDataMatrixCreatorHandler(connectionBuilder, mockConfiguration);

    dataMatrixService.stubFor(post(urlEqualTo("/generate-data-matrix"))
            .willReturn(aResponse()
                    .withStatus(500)
            ));

    try {
      instance.generateBase64DataMatrixFromImage("123", "image", false);
      fail("should throw exception");

    } catch (IOException e) {
      assertTrue(e.getMessage().startsWith("MsDataMatrixCreator Failure with StatusCode 500"));
    }
  }

  @Test
  public void testFailure415WithHtmlToPdf() {
    MsDataMatrixCreatorHandler instance = new MsDataMatrixCreatorHandler(connectionBuilder, mockConfiguration);

    dataMatrixService.stubFor(post(urlEqualTo("/generate-data-matrix"))
            .willReturn(aResponse()
                    .withStatus(415)
            ));

    try {
      instance.generateBase64DataMatrixFromImage("123", "image", false);
      fail("should throw exception");

    } catch (IOException e) {
      assertTrue(e.getMessage().startsWith("MsDataMatrixCreator Failure with StatusCode 415"));
    }
  }

  @Test
  public void testFailure419WithHtmlToPdf() {
    MsDataMatrixCreatorHandler instance = new MsDataMatrixCreatorHandler(connectionBuilder, mockConfiguration);

    dataMatrixService.stubFor(post(urlEqualTo("/generate-data-matrix"))
            .willReturn(aResponse()
                    .withStatus(419)
            ));

    try {
      instance.generateBase64DataMatrixFromImage("123", "image", false);
      fail("should throw exception");

    } catch (IOException e) {
      assertTrue(e.getMessage().startsWith("MsDataMatrixCreator Failure with StatusCode 419"));
    }
  }

  @Test
  public void testFailure422WithHtmlToPdf() {
    MsDataMatrixCreatorHandler instance = new MsDataMatrixCreatorHandler(connectionBuilder, mockConfiguration);

    dataMatrixService.stubFor(post(urlEqualTo("/generate-data-matrix"))
            .willReturn(aResponse()
                    .withStatus(422)
            ));

    try {
      instance.generateBase64DataMatrixFromImage("123", "image", false);
      fail("should throw exception");

    } catch (IOException e) {
      assertTrue(e.getMessage().startsWith("MsDataMatrixCreator Failure with StatusCode 422"));
    }
  }
}
