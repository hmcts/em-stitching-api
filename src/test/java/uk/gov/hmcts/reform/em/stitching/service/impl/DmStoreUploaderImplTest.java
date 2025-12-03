package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DmStoreUploaderImplTest {

    private DmStoreUploader dmStoreUploader;
    private int httpResponseCode;
    private boolean throwIOExceptionInInterceptor;
    private String responseBodyContent;

    @BeforeEach
    void setup() {
        httpResponseCode = 200;
        throwIOExceptionInInterceptor = false;
        responseBodyContent = "{ _embedded: { documents: [ { _links: { self: { href: 'docUri' } } } ] } }";

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(this::intercept)
            .build();

        IdamClient idamClient = mock(IdamClient.class);
        UserInfo userInfo = mock(UserInfo.class);
        when(idamClient.getUserInfo(anyString())).thenReturn(userInfo);
        when(userInfo.getUid()).thenReturn("mockUserId");

        dmStoreUploader = new DmStoreUploaderImpl(
            okHttpClient,
            () -> "mocked-s2s-token",
            idamClient,
            "http://localhost:4406"
        );
    }

    private Response intercept(Interceptor.Chain chain) throws IOException {
        if (throwIOExceptionInInterceptor) {
            throw new IOException("Simulated IOException from OkHttp client");
        }
        Request request = chain.request();
        return new Response.Builder()
            .body(ResponseBody.create(responseBodyContent, MediaType.get("application/json")))
            .request(request)
            .message(httpResponseCode == 200 ? "OK" : "Error")
            .code(httpResponseCode)
            .protocol(Protocol.HTTP_2)
            .build();
    }

    @Test
    void uploadNewDocument() throws DocumentTaskProcessingException {
        DocumentTask documentTask = new DocumentTask();
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setStitchedDocumentURI(null);
        documentTask.setBundle(bundle);
        documentTask.setJwt("mockJwt");

        File dummyFile = new File("irrelevant_for_mock.pdf");

        dmStoreUploader.uploadFile(dummyFile, documentTask);

        assertEquals("docUri", documentTask.getBundle().getStitchedDocumentURI());
    }

    @Test
    void uploadNewDocumentVersion() throws DocumentTaskProcessingException {

        DocumentTask documentTask = new DocumentTask();
        Bundle bundle = BundleTest.getTestBundle();
        String existingUri = "http://localhost:4406/documents/some-existing-id";
        bundle.setStitchedDocumentURI(existingUri);
        documentTask.setBundle(bundle);
        documentTask.setJwt("mockJwt");

        File dummyFile = new File("irrelevant_for_mock.pdf");

        dmStoreUploader.uploadFile(dummyFile, documentTask);

        assertEquals(existingUri, documentTask.getBundle().getStitchedDocumentURI());
    }

    static Stream<Arguments> uploadErrorScenarios() {
        String newDocUri = null;
        String existingDocUri = "http://localhost:4406/documents/existing-id";
        String existingDocUriForFailure = "http://localhost:4406/documents/existing-id-for-failure-test";

        Consumer<DocumentTaskProcessingException> http500Assertion = e ->
            assertTrue(e.getMessage().contains("Upload failed. Response code: 500"));

        Consumer<DocumentTaskProcessingException> ioExceptionAssertion = e -> {
            assertTrue(e.getMessage().contains("Upload failed:  Simulated IOException from OkHttp client")
                || e.getMessage().equals("Upload failed"));
            assertNotNull(e.getCause());
            assertInstanceOf(IOException.class, e.getCause());
            assertEquals("Simulated IOException from OkHttp client", e.getCause().getMessage());
        };

        Consumer<DocumentTaskProcessingException> malformedJsonAssertion = e -> {
            assertTrue(e.getMessage().startsWith("Upload failed:  A JSONObject text must begin with '{' at 1"));
            assertNotNull(e.getCause());
            assertInstanceOf(JSONException.class, e.getCause());
        };

        Consumer<DocumentTaskProcessingException> http403Assertion = e ->
            assertTrue(e.getMessage().contains("Upload failed. Response code: 403"));

        Consumer<DocumentTaskProcessingException> http503Assertion = e ->
            assertTrue(e.getMessage().contains("Upload failed. Response code: 503"));


        return Stream.of(
            Arguments.of("NewDoc: HTTP 500 Error", newDocUri, 500, false, "Error Body", http500Assertion),
            Arguments.of("NewDoc: IOException", newDocUri, 200, true, "N/A", ioExceptionAssertion),
            Arguments.of("NewDoc: Malformed JSON", newDocUri, 200, false,
                "this is not valid json", malformedJsonAssertion),

            Arguments.of("Version: HTTP 403 Error", existingDocUri, 403, false,
                "Error Body", http403Assertion),
            Arguments.of("Version: IOException", existingDocUri, 200, true, "N/A", ioExceptionAssertion),
            Arguments.of("Version: HTTP 503 Error", existingDocUriForFailure, 503, false,
                "Error Body", http503Assertion)
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("uploadErrorScenarios")
    void errorScenariosThrowDocumentTaskProcessingException(
        String scenarioName,
        String initialStitchedDocumentUri,
        int httpResponseCode,
        boolean throwIOException,
        String responseBody,
        Consumer<DocumentTaskProcessingException> specificAssertions) {

        this.httpResponseCode = httpResponseCode;
        this.throwIOExceptionInInterceptor = throwIOException;
        this.responseBodyContent = responseBody;

        DocumentTask task = new DocumentTask();
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setStitchedDocumentURI(initialStitchedDocumentUri);
        task.setBundle(bundle);
        task.setJwt("mockJwt");

        File dummyFile = new File("dummy.pdf");

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class, () ->
            dmStoreUploader.uploadFile(dummyFile, task));

        specificAssertions.accept(exception);
    }
}