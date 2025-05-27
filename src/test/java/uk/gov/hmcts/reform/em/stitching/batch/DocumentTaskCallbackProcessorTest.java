package uk.gov.hmcts.reform.em.stitching.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.Callback;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.CallbackState;
import uk.gov.hmcts.reform.em.stitching.rest.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class DocumentTaskCallbackProcessorTest {

    DocumentTaskCallbackProcessor documentTaskCallbackProcessor;

    DocumentTask documentTask;

    @Autowired
    private DocumentTaskMapper documentTaskMapper;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        documentTask = new DocumentTask();
        documentTask.setJwt("jwt");
        uk.gov.hmcts.reform.em.stitching.domain.Callback callback = new Callback();
        documentTask.setCallback(callback);
        callback.setCallbackUrl("https://mycallback.com");
        Bundle bundle = new Bundle();
        bundle.setId(1234L);
        documentTask.setBundle(bundle);
    }

    @Test
    void testCallback200() {

        documentTaskCallbackProcessor = buildProcessorWithHttpClientIntercepted(200, "{}");
        documentTaskCallbackProcessor.callBackMaxAttempts = 3;

        DocumentTask processedDocumentTask =
                documentTaskCallbackProcessor.process(documentTask);

        assertNotNull(processedDocumentTask);
        assertEquals(CallbackState.SUCCESS, processedDocumentTask.getCallback().getCallbackState());

    }

    @Test
    void testCallback500FirstAttempt() {

        documentTaskCallbackProcessor = buildProcessorWithHttpClientIntercepted(543, "errorx");
        documentTaskCallbackProcessor.callBackMaxAttempts = 3;

        documentTask.getCallback().setAttempts(0);
        DocumentTask processedDocumentTask =
                documentTaskCallbackProcessor.process(documentTask);

        assertNotNull(processedDocumentTask);
        assertEquals(CallbackState.NEW, processedDocumentTask.getCallback().getCallbackState());

    }

    @Test
    void testCallback500ThirdAttempt() {

        documentTaskCallbackProcessor = buildProcessorWithHttpClientIntercepted(543, "errorx");
        documentTaskCallbackProcessor.callBackMaxAttempts = 3;

        documentTask.getCallback().setAttempts(2);
        DocumentTask processedDocumentTask =
            documentTaskCallbackProcessor.process(documentTask);

        assertNotNull(processedDocumentTask);
        assertEquals(CallbackState.FAILURE, processedDocumentTask.getCallback().getCallbackState());

    }

    @Test
    void testCallbackIOException() {
        OkHttpClient http = new OkHttpClient
            .Builder()
            .addInterceptor(chain -> {
                throw new IOException("Simulated IOException");
            })
            .build();

        documentTaskCallbackProcessor = new DocumentTaskCallbackProcessor(
            http, () -> "auth", documentTaskMapper, objectMapper);
        documentTaskCallbackProcessor.callBackMaxAttempts = 3;

        DocumentTask processedDocumentTask =
            documentTaskCallbackProcessor.process(documentTask);

        assertNotNull(processedDocumentTask);
        assertEquals(CallbackState.FAILURE, processedDocumentTask.getCallback().getCallbackState());
    }

    private DocumentTaskCallbackProcessor buildProcessorWithHttpClientIntercepted(int httpStatus, String responseBody) {
        OkHttpClient http = new OkHttpClient
                .Builder()
                .addInterceptor(chain -> new Response.Builder()
                    .body(
                        ResponseBody.create(
                            responseBody,
                            MediaType.get("application/json")
                        )
                    )
                    .request(chain.request())
                    .message("")
                    .code(httpStatus)
                    .protocol(Protocol.HTTP_2)
                    .build())
                .build();

        return new DocumentTaskCallbackProcessor(http, () -> "auth", documentTaskMapper, objectMapper);

    }

}
