package uk.gov.hmcts.reform.em.stitching.rest;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WelcomeResourceTest {

    private final WelcomeResource welcomeResource = new WelcomeResource();

    @Test
    public void test_should_return_welcome_response() {

        ResponseEntity<Object> responseEntity = welcomeResource.welcome();
        String expectedJson = "{\"message\":\"Welcome to Stitching API!\"}";

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        JSONAssert.assertEquals(expectedJson, String.valueOf(responseEntity.getBody()),true);
    }
}
