package uk.gov.hmcts.reform.em.stitching.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.Callback;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.CallbackState;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTaskCallbackProcessorTest {

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private DocumentTaskMapper documentTaskMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Call httpCall;

    @Mock
    private DocumentTaskDTO documentTaskDTO;

    @InjectMocks
    private DocumentTaskCallbackProcessor documentTaskCallbackProcessor;

    private DocumentTask documentTask;

    @BeforeEach
    void setUp() {
        documentTaskCallbackProcessor.callBackMaxAttempts = 3;

        documentTask = new DocumentTask();
        documentTask.setId(100L);
        documentTask.setJwt("jwt-token");

        Callback callback = new Callback();
        callback.setCallbackUrl("http://localhost/callback");
        callback.setId(200L);
        callback.setCallbackState(CallbackState.NEW);
        documentTask.setCallback(callback);

        Bundle bundle = new Bundle();
        bundle.setId(1234L);
        documentTask.setBundle(bundle);
    }

    @Test
    void shouldHandleSuccessfulCallback() throws IOException {
        mockDependencies();
        Response response = createResponse(200, "{}");
        when(httpCall.execute()).thenReturn(response);

        DocumentTask result = documentTaskCallbackProcessor.process(documentTask);

        assertEquals(CallbackState.SUCCESS, result.getCallback().getCallbackState());
        verify(okHttpClient).newCall(any(Request.class));
    }

    @Test
    void shouldIncrementAttemptsOnFailure() throws IOException {
        mockDependencies();
        documentTask.getCallback().setAttempts(0);
        Response response = createResponse(500, "Internal Server Error");
        when(httpCall.execute()).thenReturn(response);

        DocumentTask result = documentTaskCallbackProcessor.process(documentTask);

        assertEquals(CallbackState.NEW, result.getCallback().getCallbackState());
        assertEquals(1, result.getCallback().getAttempts());
        assertTrue(result.getCallback().getFailureDescription().contains("HTTP Callback failed"));
    }

    @Test
    void shouldFailTaskWhenMaxAttemptsReached() throws IOException {
        mockDependencies();
        documentTask.getCallback().setAttempts(2); 
        Response response = createResponse(503, "Service Unavailable");
        when(httpCall.execute()).thenReturn(response);

        DocumentTask result = documentTaskCallbackProcessor.process(documentTask);

        assertEquals(CallbackState.FAILURE, result.getCallback().getCallbackState());
        assertEquals(3, result.getCallback().getAttempts());
    }

    @Test
    void shouldHandleIoExceptionAsFailure() throws IOException {
        mockDependencies();
        when(httpCall.execute()).thenThrow(new IOException("Connection reset"));

        DocumentTask result = documentTaskCallbackProcessor.process(documentTask);

        assertEquals(CallbackState.FAILURE, result.getCallback().getCallbackState());
    }

    @Test
    void shouldHandleJsonProcessingExceptionAsFailure() throws IOException {
        when(authTokenGenerator.generate()).thenReturn("auth-token");
        when(documentTaskMapper.toDto(any(DocumentTask.class))).thenReturn(documentTaskDTO);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Json Error") {});

        DocumentTask result = documentTaskCallbackProcessor.process(documentTask);

        assertEquals(CallbackState.FAILURE, result.getCallback().getCallbackState());
    }

    private void mockDependencies() throws JsonProcessingException {
        when(authTokenGenerator.generate()).thenReturn("auth-token");
        when(documentTaskMapper.toDto(any(DocumentTask.class))).thenReturn(documentTaskDTO);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(okHttpClient.newCall(any(Request.class))).thenReturn(httpCall);
    }

    private Response createResponse(int code, String body) {
        return new Response.Builder()
            .request(new Request.Builder().url("http://localhost/callback").build())
            .protocol(Protocol.HTTP_2)
            .code(code)
            .message("")
            .body(ResponseBody.create(body, MediaType.get("application/json")))
            .build();
    }
}