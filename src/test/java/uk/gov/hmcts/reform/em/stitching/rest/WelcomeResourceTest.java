package uk.gov.hmcts.reform.em.stitching.rest;

import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WelcomeResourceTest {

    private final WelcomeResource welcomeResource = new WelcomeResource();

    @Test
    public void test_should_return_welcome_response() {

        ResponseEntity<Object> responseEntity = welcomeResource.welcome();
        String expectedMessage = "Welcome to Stitching API!";

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertThat(Objects.equals(responseEntity.getBody(), expectedMessage));
    }
}
