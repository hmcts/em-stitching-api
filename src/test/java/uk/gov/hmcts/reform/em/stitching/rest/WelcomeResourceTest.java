package uk.gov.hmcts.reform.em.stitching.rest;

import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WelcomeResourceTest {

    private final WelcomeResource welcomeResource = new WelcomeResource();

    @Test
    public void testEndPointResponseCode() {
        ResponseEntity<Object> responseEntity = welcomeResource.welcome();

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testEndpointResponseMessage() {
        ResponseEntity<Object> responseEntity = welcomeResource.welcome();

        Map<String, String> expectedResponse = new HashMap<>();
        expectedResponse.put("message", "Welcome to Stitching API!");

        String cacheHeader = responseEntity.getHeaders().getCacheControl();

        assertNotNull(responseEntity);
        assertEquals("no-cache", cacheHeader);
        assertEquals(expectedResponse, responseEntity.getBody());
    }
}

