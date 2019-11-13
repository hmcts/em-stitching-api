package uk.gov.hmcts.reform.em.stitching.domain.validation;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class CallableEndpointValidatorTest {

    CallableEndpointValidator callableEndpointValidator = new CallableEndpointValidator();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Test
    public void isValidReturn200() {
        stubFor(post(urlEqualTo("/my/callback/resource"))
                .willReturn(aResponse()
                        .withStatus(200)));

        assertTrue(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }

    @Test
    public void isValidReturn400() {
        stubFor(post(urlEqualTo("/my/callback/resource"))
                .willReturn(aResponse()
                        .withStatus(400)));

        assertTrue(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }

    @Test
    public void isValidReturn500() {
        stubFor(post(urlEqualTo("/my/callback/resource"))
                .willReturn(aResponse()
                        .withStatus(500)));

        assertFalse(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }

    @Test
    public void isValidUnreachable() {
        assertFalse(callableEndpointValidator.isValid("http://localhost:9999/my/callback/resource", null));
    }

    @Test
    public void isValidDomainDoesNotExist() {
        assertFalse(callableEndpointValidator.isValid("http://thisdomain.surely.does.not.exist12365675.com/my/callback/resource", null));
    }

    @Test
    public void isValidDIncorrectUrl() {
        assertFalse(callableEndpointValidator.isValid("fsduhfiusdhfds", null));
    }

    @Test
    public void isValidConnectionReset() {

        stubFor(post(urlEqualTo("/my/callback/resource"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertFalse(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }
}
