package uk.gov.hmcts.reform.em.stitching.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Objects;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
public class WelcomeResourceTest {

    private final WelcomeResource welcomeResource = new WelcomeResource();

    @Test
    public void testEndPointResponseCode() {

        ResponseEntity<Object> responseEntity = welcomeResource.welcome();

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
    @Test
    public void testEndpointResponseMessage() throws JsonProcessingException {

        ResponseEntity<Object> responseEntity = welcomeResource.welcome();

        ObjectMapper mapper = new ObjectMapper();
        String expectedResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("message" , "Welcome to Stitching API!"));

        String actualResponse = (String) responseEntity.getBody();
        String cacheHeader = responseEntity.getHeaders().getCacheControl();

        assertNotNull(responseEntity);
        assertEquals("no-cache",cacheHeader);
        assertEquals(expectedResponse, actualResponse);
    }
}

