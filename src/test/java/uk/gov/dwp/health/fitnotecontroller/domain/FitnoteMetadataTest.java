package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FitnoteMetadataTest {
    private static final String EXPECTED_SERIALISED_CLASS = "{\"businessUnitID\":null,\"classification\":0,\"documentType\":0,\"documentSource\":0,\"postCode\":\"LS6 4PT\",\"nino\":null,\"benefitType\":99,\"customerMobileNumber\":\"0113 25678886\"}";

    @Test
    public void classCanBeBuiltUsingSerialisedJson() throws IOException {
        FitnoteMetadata metadata = new ObjectMapper().readValue(EXPECTED_SERIALISED_CLASS, FitnoteMetadata.class);
        assertNull(metadata.getBusinessUnitID());
        assertEquals(0, metadata.getClassification());
        assertEquals(0, metadata.getDocumentType());
        assertEquals(0, metadata.getDocumentSource());
        assertEquals("LS6 4PT", metadata.getPostCode());
        assertNull(metadata.getNino());
        assertEquals(99, metadata.getBenefitType());
        assertEquals("0113 25678886", metadata.getCustomerMobileNumber());
    }

    @Test
    public void serialisationOfBuiltClassDoesNotEqual() throws JsonProcessingException {
        FitnoteMetadata instance = new FitnoteMetadata();
        instance.setCustomerMobileNumber("0113 25678886");
        instance.setPostCode("LS6 4PT");
        instance.setBenefitType(99);

        assertThat("json output should match", new ObjectMapper().writeValueAsString(instance), is(equalTo(EXPECTED_SERIALISED_CLASS)));
    }
}
