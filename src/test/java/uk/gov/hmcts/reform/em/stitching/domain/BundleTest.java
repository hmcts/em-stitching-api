package uk.gov.hmcts.reform.em.stitching.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

public class BundleTest {
    private static final String DEFAULT_DOCUMENT_ID = "AAAAAAAAAA";

    private final ObjectMapper mapper = new ObjectMapper();

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
        System.out.println(result);

        assertThat(result, containsString("My bundle"));
        assertThat(result, containsString("2019-01-09T14:00:00Z"));
    }

    public static Bundle getTestBundle() {
        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentId(DEFAULT_DOCUMENT_ID);

        Bundle bundle = new Bundle(new ArrayList<>(), new ArrayList<>());
        bundle.setBundleTitle("My bundle");
        bundle.setVersion(1);
        bundle.setDescription("Bundle description");
        bundle.setCreatedDate(Instant.parse("2019-01-09T14:00:00Z"));
        bundle.setCreatedBy("Billy Bob");
        bundle.setDocuments(Collections.singletonList(bundleDocument));

        return bundle;

    }

}