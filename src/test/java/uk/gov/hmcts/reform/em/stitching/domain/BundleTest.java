package uk.gov.hmcts.reform.em.stitching.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

public class BundleTest {
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    public void serializesToJson() throws JsonProcessingException {
        Bundle bundle = BundleTest.getTestBundle();

        String result = mapper.writeValueAsString(bundle);

        assertThat(result, containsString("My bundle"));
        assertThat(result, containsString("2019-01-09T14:00:00Z"));
    }

    public static Bundle getTestBundle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("My bundle");
        bundle.setVersion(1);
        bundle.setDescription("Bundle description");
        bundle.setCreatedDate(Instant.parse("2019-01-09T14:00:00Z"));
        bundle.setCreatedBy("Billy Bob");

        return bundle;

    }

}