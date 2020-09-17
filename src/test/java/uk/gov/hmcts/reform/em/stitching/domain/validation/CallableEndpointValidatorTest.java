package uk.gov.hmcts.reform.em.stitching.domain.validation;

import okhttp3.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class CallableEndpointValidatorTest {

    @Test
    public void isValidReturn200() {
        CallableEndpointValidator callableEndpointValidator = createValidatorWithMockHttp((Interceptor.Chain chain) -> new Response.Builder()
                .body(ResponseBody.create("", MediaType.parse("plain/text")))
                .request(chain.request())
                .message("")
                .code(200)
                .protocol(Protocol.HTTP_2)
                .build());

        assertTrue(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }

    @Test
    public void isValidReturn400() {
        CallableEndpointValidator callableEndpointValidator = createValidatorWithMockHttp((Interceptor.Chain chain) -> new Response.Builder()
                .body(ResponseBody.create("", MediaType.parse("plain/text")))
                .request(chain.request())
                .message("")
                .code(400)
                .protocol(Protocol.HTTP_2)
                .build());

        assertTrue(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }

    @Test
    public void isValidReturn500() {
        CallableEndpointValidator callableEndpointValidator = createValidatorWithMockHttp((Interceptor.Chain chain) -> new Response.Builder()
                .body(ResponseBody.create("", MediaType.parse("plain/text")))
                .request(chain.request())
                .message("")
                .code(500)
                .protocol(Protocol.HTTP_2)
                .build());

        assertFalse(callableEndpointValidator.isValid("http://localhost:8089/my/callback/resource", null));
    }

    @Test
    public void isValidUnreachable() {
        CallableEndpointValidator callableEndpointValidator = createValidatorWithMockHttp((Interceptor.Chain chain) -> {
            throw new RuntimeException("x");
        });

        assertFalse(callableEndpointValidator.isValid("http://localhost:9999/my/callback/resource", null));
    }

    private CallableEndpointValidator createValidatorWithMockHttp(Interceptor interceptor) {
        OkHttpClient http = new OkHttpClient
                .Builder()
                .addInterceptor(interceptor)
                .build();

        return new CallableEndpointValidator(http);
    }
}



