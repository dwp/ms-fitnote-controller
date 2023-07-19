package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ImagePayloadTest {

    @Test
    public void validateNinoIsCorrectlySerialised() throws JsonProcessingException {
        String expectedResult = "{\"ninoSuffix\":\"A\",\"ninoBody\":\"AA370773\"}";
        ImagePayload instance = new ImagePayload();
        instance.setNino("AA370773A");

        assertThat(new ObjectMapper().writeValueAsString(instance.getNinoObject()), is(equalTo(expectedResult)));
    }

    @Test
    public void nullNinoObjectShouldSerialise() throws IOException {
        ImagePayload instance = new ImagePayload();
        instance.setSessionId("123");

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.readTree(mapper.writeValueAsString(instance));
        assertNull("ninoObject should be null", jsonNode.get("ninoObject").textValue());

        ImagePayload newInstance = mapper.readValue(mapper.writeValueAsString(instance), ImagePayload.class);
        assertNull(newInstance.getNinoObject());
    }

    @Test
    public void ninoObjectSerialisationSucces() throws IOException {
        ImagePayload instance = new ImagePayload();
        instance.setNino("AA123456A");
        instance.setSessionId("123");

        ObjectMapper mapper = new ObjectMapper();

        String serialisedClass = mapper.writeValueAsString(instance);

        ImagePayload newClass = mapper.readValue(serialisedClass, ImagePayload.class);
        assertThat("nino suffix should equal", newClass.getNinoObject().getNinoSuffix(), is(equalTo(instance.getNinoObject().getNinoSuffix())));
        assertThat("nino body should equal", newClass.getNinoObject().getNinoBody(), is(equalTo(instance.getNinoObject().getNinoBody())));
    }

    @Test
    public void testSerialisationWorksCorrectly() throws IOException {
        ImagePayload instance = new ImagePayload();
        instance.setClaimantAddress(new Address());
        instance.setMobileNumber("12345678");
        instance.setNino("AA123456A");
        instance.setSessionId("s");
        instance.setImage("img");

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.readTree(mapper.writeValueAsString(instance));

        assertNotNull("address should not be null", jsonNode.get("claimantAddress").toString());
        assertNotNull("mobile should not be null", jsonNode.get("mobileNumber").textValue());
        assertNotNull("session should not be null", jsonNode.get("sessionId").textValue());
        assertNotNull("image should not be null", jsonNode.get("image").textValue());
        assertNotNull("nino should not be null", jsonNode.get("nino").textValue());
        assertNotNull("ninoObject should not be null", jsonNode.get("ninoObject"));

        assertNull("rawImageSize should be null", jsonNode.get("rawImageSize"));
    }
}
