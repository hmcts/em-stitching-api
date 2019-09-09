package uk.gov.hmcts.reform.em.stitching.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.Callback;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.CallbackState;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DocumentTaskCallbackProcessorTest {

    DocumentTaskCallbackProcessor documentTaskCallbackProcessor;

    @Autowired
    private DocumentTaskMapper documentTaskMapper;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    public void testCallback200() {

        documentTaskCallbackProcessor = buildProcessorWithHttpClientIntercepted(200, "{}");

        DocumentTask documentTask = new DocumentTask();
        documentTask.setJwt("jwt");
        uk.gov.hmcts.reform.em.stitching.domain.Callback callback = new Callback();
        documentTask.setCallback(callback);
        callback.setCallbackUrl("https://mycallback.com");

        DocumentTask processedDocumentTask =
                documentTaskCallbackProcessor.process(documentTask);

        Assert.assertEquals(CallbackState.SUCCESS, processedDocumentTask.getCallback().getCallbackState());

    }

    @Test
    public void testCallback500() {

        documentTaskCallbackProcessor = buildProcessorWithHttpClientIntercepted(543, "errorx");

        DocumentTask documentTask = new DocumentTask();
        documentTask.setJwt("jwt");
        uk.gov.hmcts.reform.em.stitching.domain.Callback callback = new Callback();
        documentTask.setCallback(callback);
        callback.setCallbackUrl("https://mycallback.com");

        DocumentTask processedDocumentTask =
                documentTaskCallbackProcessor.process(documentTask);

        Assert.assertEquals(CallbackState.FAILURE, processedDocumentTask.getCallback().getCallbackState());
        Assert.assertEquals("HTTP Callback failed.\nStatus: 543.\nResponse Body: errorx", processedDocumentTask.getCallback().getFailureDescription());

    }

    private DocumentTaskCallbackProcessor buildProcessorWithHttpClientIntercepted(int httpStatus, String responseBody) {
        OkHttpClient http = new OkHttpClient
                .Builder()
                .addInterceptor(chain -> new Response.Builder()
                    .body(
                        ResponseBody.create(
                            MediaType.get("application/json"),
                            responseBody
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
