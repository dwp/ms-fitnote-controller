package uk.gov.dwp.health.fitnotecontroller.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.lettuce.core.cluster.RedisClusterClient;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.gov.dwp.health.crypto.CryptoConfig;
import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.crypto.CryptoMessage;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.messageq.EventConstants;
import uk.gov.dwp.health.messageq.amazon.items.AmazonConfigBase;
import uk.gov.dwp.health.messageq.amazon.items.messages.SnsMessageClassItem;
import uk.gov.dwp.health.messageq.amazon.utils.AmazonQueueUtilities;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("squid:S1192") // all string literals
public class FitnoteCucumberSteps {
    private static final Logger LOG = LoggerFactory.getLogger(FitnoteCucumberSteps.class.getName());
    private static final int IMAGE_STATUS_QUERY_TIMEOUT_MILLIS = 240000;

    private ObjectMapper mapper = new ObjectMapper();
    private AmazonQueueUtilities queueUtilities;
    private CryptoDataManager awsKmsCryptoClass;
    private List<Message> queueMessages;
    private Pattern regexFileExtension;
    private HttpClient httpClient;
    private HttpResponse response;
    private String jsonResponse;
    private static final String LOCALSTACK_HOST = "http://localstack:4566";


    @Before
    public void startServiceMocks() throws CryptoException, InvalidKeyException, IllegalBlockSizeException,
            NoSuchPaddingException, NoSuchAlgorithmException, IOException {
        String redisHost = "redis-cluster";
        regexFileExtension = Pattern.compile("\\.(\\w+)");

        // create local properties to negate KMS from needing to access Metadata Service for IAM role privs
        System.setProperty("aws.accessKeyId", "this_is_my_system_property_key");
        System.setProperty("aws.secretAccessKey", "abcd123456789");

        LOG.info("Flushing redis contents : {}", RedisClusterClient.create("redis://" + redisHost + ":6379")
                .connect()
                .sync()
                .flushall());

        AmazonConfigBase snsConfig = new AmazonConfigBase();
        snsConfig.setEndpointOverride(LOCALSTACK_HOST);
        snsConfig.setS3EndpointOverride(LOCALSTACK_HOST);
        snsConfig.setLargePayloadSupportEnabled(false);
        snsConfig.setPathStyleAccessEnabled(true);
        snsConfig.setS3BucketName("sns-bucket");
        snsConfig.setRegion(Region.US_EAST_1);

        AmazonConfigBase sqsConfig = new AmazonConfigBase();
        sqsConfig.setEndpointOverride(LOCALSTACK_HOST);
        sqsConfig.setS3EndpointOverride(LOCALSTACK_HOST);
        sqsConfig.setLargePayloadSupportEnabled(false);
        sqsConfig.setPathStyleAccessEnabled(true);
        sqsConfig.setS3BucketName("sqs-bucket");
        sqsConfig.setRegion(Region.US_EAST_1);

        queueUtilities = new AmazonQueueUtilities(sqsConfig, snsConfig);

        CryptoConfig cryptoConfig = new CryptoConfig("alias/test_request_id");
        cryptoConfig.setKmsEndpointOverride(LOCALSTACK_HOST);
        awsKmsCryptoClass = new CryptoDataManager(cryptoConfig);
    }

    @DataTableType(replaceWithEmptyString = "[blank]")
    public String stringType(String cell) {
        return cell;
    }

    @Given("^the http client is up$")
    public void theControllerIsUp() {
        httpClient = HttpClientBuilder.create().build();
    }

    @When("^I hit the service url \"([^\"]*)\" with the following json body$")
    public void hitServiceUrlWithSpecifiedJson(String url, Map<String, String> jsonValues) throws IOException {
        String jsonRequestBody = buildJsonBody(jsonValues);
        performHttpPostWithUriOf(url, jsonRequestBody);
    }

    @And("^I wait (\\d+) seconds to guarantee message delivery$")
    public void iWait(int seconds) throws InterruptedException {
        TimeUnit.SECONDS.sleep(seconds);
    }


    @And("^I hit the service url \"([^\"]*)\" with session id \"([^\"]*)\" getting return status (\\d+) and finally containing the following json body$")
    public void iHitTheServiceUrlWithSessionIdGettingReturnStatusAndFinallyContainingTheFollowingJsonBody(String url, String sessionId, int status, Map<String, String> expectedValues) throws IOException, InterruptedException {
        String fullUrl = String.format("%s?sessionId=%s", url, sessionId);
        long startTime = System.currentTimeMillis();
        int timeout = expectedValues.get("fitnoteStatus").equals("NEVER_GONNA_HAPPEN") ? 60000 : IMAGE_STATUS_QUERY_TIMEOUT_MILLIS;

        do {
            if ((System.currentTimeMillis() - startTime) > timeout) {
                throw new IOException(String.format("TIMING OUT :: '%s' request after %d milliseconds with no match on response body", url, IMAGE_STATUS_QUERY_TIMEOUT_MILLIS));
            }

            TimeUnit.SECONDS.sleep(1);
            performHttpGetWithUriOf(fullUrl);
            checkHTTPResponseStatusCode(status);

        } while (!httpResponseBodyContainsEntries(expectedValues));
    }

    @And("^I hit the service url \"([^\"]*)\" with a POST and session id \"([^\"]*)\" getting return status (\\d+) the following json body$")
    public void iHitTheServiceUrlWithAPOSTAndSessionIdGettingReturnStatusTheFollowingJsonBody(String url, String sessionId, int statusCode, Map<String, String> expectedValues) throws IOException {
        performHttpPostWithUriOf(url, String.format("{\"sessionId\":\"%s\"}", sessionId));
        checkHTTPResponseStatusCode(statusCode);

        assertTrue(httpResponseBodyContainsEntries(expectedValues));
    }

    @And("^I hit the service url \"([^\"]*)\" with session id \"([^\"]*)\" getting return status (\\d+) and finally timing out trying to match the following body$")
    public void iHitTheServiceUrlWithSessionIdGettingReturnStatusAndFinallyContainingTheFollowingJsonBodyException(String url, String sessionId, int status, Map<String, String> expectedValues) throws InterruptedException {
        try {
            iHitTheServiceUrlWithSessionIdGettingReturnStatusAndFinallyContainingTheFollowingJsonBody(url, sessionId, status, expectedValues);

        } catch (IOException e) {
            LOG.error(e.getClass().getName(), e);
            assertThat("Must have timed out", e.getMessage(), containsString("TIMING OUT ::"));
        }
    }

    @Then("^I receive a HTTP response of (\\d+)$")
    public void checkHTTPResponseStatusCode(int expectedStatusCode) {
        int actualStatusCode = response.getStatusLine().getStatusCode();
        assertThat(actualStatusCode, is(expectedStatusCode));
    }

    @Then("^I receive a HTTP response of (\\d+) with the following json body$")
    public void iReceiveAHTTPResponseOfWithFollowingJsonBody(int expectedStatusCode,
                                                             Map<String, String> jsonValues)
            throws IOException {
        String expectedJsonResponseBody = buildJsonBody(jsonValues);
        int actualStatusCode = response.getStatusLine().getStatusCode();
        assertThat(actualStatusCode, is(expectedStatusCode));
        assertThat(jsonResponse, is(equalTo(expectedJsonResponseBody)));
    }

    @And("^I create a sns topic named \"([^\"]*)\"$")
    public void iCreateAnSnsExchangeNamed(String topicName) {
        queueUtilities.createTopic(topicName);
    }

    @And("^I create a catch all subscription for queue name \"([^\"]*)\" binding to topic \"([^\"]*)\" with msg visibility timeout of (\\d+) seconds$")
    public void iCreateACatchAllSubscriptionForQueueNameBindingToExchange(String queueName, String topicName, int timeout) {
        queueUtilities.createQueue(queueName, timeout);
        queueUtilities.purgeQueue(queueName);
        queueUtilities.subscribeQueueToTopic(queueName, topicName);
    }

    @And("^I clear all content for queue name \"([^\"]*)\"$")
    public void clearQueueContents(String queueName){
        queueUtilities.deleteQueue(queueName);
    }

    @And("^I wait (\\d+) seconds for the visibility timeout to expire$")
    public void iWaitSecondsForVisibiliyTimout(long timeout) throws InterruptedException {
        TimeUnit.SECONDS.sleep(timeout);
    }

    @And("^a message is successfully removed from the queue, there were a total of (\\d+) messages on queue \"([^\"]*)\"$")
    public void thereIsPendingMessageOnQueue(int totalMessages, String queueName) throws IOException {
        queueMessages = queueUtilities.receiveMessages(queueName, queueUtilities.getS3Sqs());

        assertThat("mismatched messages", queueMessages.size(), is(equalTo(totalMessages)));

        assertNotNull("queue contents are null", queueMessages);
        queueUtilities.deleteMessageFromQueue(queueName, queueMessages.get(0).receiptHandle());
    }

    @And("^there are no pending messages on queue \"([^\"]*)\"$")
    public void thereAreNoPendingMessageOnQueue(String queueName) throws IOException {
        queueMessages = queueUtilities.receiveMessages(queueName, queueUtilities.getS3Sqs());
        assertNotNull(queueMessages);
        assertTrue(queueMessages.isEmpty());
    }

    @And("^the message has a correlation id, delivery mode (PERSISTENT|NON-PERSISTENT)? and a valid ImagePayload with the following NINO, House number, Postcode and matching SessionId$")
    public void theMessageIsTakenFromTheQueueHasACorrelationIdAndAValidImagePayloadWithTheFollowingNINOHouseNumberAndPostcode(String msgPersistence, Map<String, String> jsonValues) throws IOException, CryptoException {
        assertNotNull(queueMessages);
        assertFalse(queueMessages.isEmpty());

        SnsMessageClassItem snsMessageClass = new SnsMessageClassItem().buildMessageClassItem(queueMessages.get(0).body());
        String msgContents = snsMessageClass.getMessage();

        if (snsMessageClass.getMessageAttributes().get(EventConstants.KMS_DATA_KEY_MARKER) != null) {
            CryptoMessage cryptoMessage = new CryptoMessage();
            cryptoMessage.setKey(snsMessageClass.getMessageAttributes().get(EventConstants.KMS_DATA_KEY_MARKER).stringValue());
            cryptoMessage.setMessage(msgContents);

            msgContents = awsKmsCryptoClass.decrypt(cryptoMessage);
        }

        JsonNode payload = new ObjectMapper().readTree(msgContents).path("payload");

        for (Map.Entry<String, String> field : jsonValues.entrySet()) {
            switch (field.getKey()) {
                case "nino":
                    assertThat("Mismatched NINO", payload.path(field.getKey()).toString(), is(equalTo(field.getValue())));
                    break;
                case "houseNameOrNumber":
                    assertThat("Mismatched House Name/Number", payload.path("claimantAddress").path(field.getKey()).toString(), is(equalTo(field.getValue())));
                    break;
                case "postcode":
                    assertThat("Mismatched Postcode", payload.path("claimantAddress").path(field.getKey()).toString(), is(equalTo(field.getValue())));
                    break;
                case "sessionId":
                    assertThat("Wrong sessionId", payload.path("sessionId").toString(), is(equalTo(field.getValue())));
                    break;
                default:
                    throw new IOException(String.format("unknown field '%s'", field.getKey()));
            }
        }
    }

    private boolean httpResponseBodyContainsEntries(Map<String, String> expectedJsonValues) {
        boolean matchResult = true;
        try {
            JsonNode responseBody = mapper.readTree(jsonResponse);
            for (Map.Entry<String, String> expectedJsonKeyValue : expectedJsonValues.entrySet()) {
                JsonNode jsonKey = responseBody.get(expectedJsonKeyValue.getKey());
                if (jsonKey != null) {
                    String actualValue = jsonKey.isObject()? jsonKey.toString() : jsonKey.textValue();
                    LOG.info("Found json element '{}' with expected value '{}'.  Actual value was '{}'", expectedJsonKeyValue.getKey(), expectedJsonKeyValue.getValue(), actualValue);
                    if (!expectedJsonKeyValue.getValue().equals(actualValue)) {
                        matchResult = false;
                    }

                } else {
                    matchResult = false;
                }
            }

        } catch (IOException e) {
            matchResult = false;
            LOG.error(e.getClass().getName(), e);
        }

        return matchResult;
    }

    private String buildJsonBody(Map<String, String> jsonValues) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        String delimiter = "";
        for (Map.Entry<String, String> jsonKeyValue : jsonValues.entrySet()) {
            if (!jsonKeyValue.getKey().isEmpty()) {
                builder.append(delimiter);
                builder.append("\"" + jsonKeyValue.getKey() + "\":");
                String value = jsonKeyValue.getValue();
                Matcher matcher = regexFileExtension.matcher(value);
                if (matcher.find()) {
                    builder.append("\"" + getEncodedImage(value) + "\"");
                } else {
                    builder.append(value);
                }
                delimiter = ",";
            }
        }
        builder.append("}");
        return builder.toString();
    }

    private String getEncodedImage(String imageFileName) throws IOException {
        File file = new File(this.getClass().getResource(imageFileName).getPath());
        return Base64.encodeBase64String(FileUtils.readFileToByteArray(file));
    }

    private void performHttpPostWithUriOf(String uri, String body) throws IOException {
        HttpPost httpUriRequest = new HttpPost(uri);
        HttpEntity entity = new StringEntity(body);
        httpUriRequest.setEntity(entity);
        response = httpClient.execute(httpUriRequest);
        HttpEntity responseEntity = response.getEntity();
        jsonResponse = EntityUtils.toString(responseEntity);
    }

    private void performHttpGetWithUriOf(String uri) throws IOException {
        HttpGet httpUriRequest = new HttpGet(uri);
        response = httpClient.execute(httpUriRequest);
        HttpEntity responseEntity = response.getEntity();
        jsonResponse = EntityUtils.toString(responseEntity);
        LOG.info("RESPONSE FROM HTTP GET : {}", jsonResponse);
    }
}
