package uk.gov.hmcts.reform.em.stitching.domain.validation;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
class CallableEndpointValidatorTest {

    @Test
    void isValidReturn200() {
        CallableEndpointValidator callableEndpointValidator =
                createValidatorWithMockHttp((Interceptor.Chain chain) -> new Response.Builder()
                .body(ResponseBody.create("", MediaType.parse("plain/text")))
                .request(chain.request())
                .message("")
                .code(200)
                .protocol(Protocol.HTTP_2)
                .build());

        assertTrue(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }

    @Test
    void isValidReturn400() {
        CallableEndpointValidator callableEndpointValidator =
                createValidatorWithMockHttp((Interceptor.Chain chain) -> new Response.Builder()
                .body(ResponseBody.create("", MediaType.parse("plain/text")))
                .request(chain.request())
                .message("")
                .code(400)
                .protocol(Protocol.HTTP_2)
                .build());

        assertTrue(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }

    @Test
    void isValidReturn500() {
        CallableEndpointValidator callableEndpointValidator =
                createValidatorWithMockHttp((Interceptor.Chain chain) -> new Response.Builder()
                .body(ResponseBody.create("", MediaType.parse("plain/text")))
                .request(chain.request())
                .message("")
                .code(500)
                .protocol(Protocol.HTTP_2)
                .build());

        assertFalse(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }

    @Test
    void isValidUnreachable() {
        CallableEndpointValidator callableEndpointValidator = createValidatorWithMockHttp((Interceptor.Chain chain) -> {
            throw new RuntimeException("x");
        });

        assertFalse(callableEndpointValidator.isValid("http://localhost:9999/my/callback/resource", null));
    }


    @Test
    void isValidHttpUrlWithoutExplicitPortUsesDefaultPort() {
        CallableEndpointValidator callableEndpointValidator =
            createValidatorWithMockHttp((Interceptor.Chain chain) -> {
                String requestedUrl = chain.request().url().toString();
                assertEquals("http://somehost.com/", requestedUrl);
                return new Response.Builder()
                    .body(ResponseBody.create("", MediaType.parse("plain/text")))
                    .request(chain.request())
                    .message("")
                    .code(200)
                    .protocol(Protocol.HTTP_2)
                    .build();
            });

        assertTrue(callableEndpointValidator.isValid("http://somehost.com/some/path", null));
    }

    private CallableEndpointValidator createValidatorWithMockHttp(Interceptor interceptor) {
        OkHttpClient http = new OkHttpClient
                .Builder()
                .addInterceptor(interceptor)
                .build();

        return new CallableEndpointValidator(http);
    }
}



