package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;

import java.io.File;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
class DmStoreUploaderImplTest {

    DmStoreUploader dmStoreUploader;

    @BeforeEach
    public void setup() {
        OkHttpClient http = new OkHttpClient
            .Builder()
            .addInterceptor(DmStoreUploaderImplTest::intercept)
            .build();

        dmStoreUploader = new DmStoreUploaderImpl(
            http,
            () -> "auth",
            "http://localhost/",
            param -> new User("id", new HashSet<>())
        );
    }

    private static Response intercept(Interceptor.Chain chain) {
        return new Response.Builder()
            .body(
                ResponseBody.create(
                        "{ _embedded: { documents: [ { _links: { self: { href: 'docUri' } } } ] } }",
                        MediaType.get("application/json")
                )
            )
            .request(chain.request())
            .message("")
            .code(200)
            .protocol(Protocol.HTTP_2)
            .build();
    }

    @Test
    void uploadFile() {
        DocumentTask task = new DocumentTask();
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setStitchedDocumentURI("derp");
        task.setBundle(bundle);
        task.setJwt("xxx");

        assertThrows(DocumentTaskProcessingException.class, () ->
                dmStoreUploader.uploadFile(new File("xyz.abc"), task));
    }

    @Test
    void upload() throws Exception {
        DocumentTask documentTask = new DocumentTask();
        Bundle bundle = BundleTest.getTestBundle();
        documentTask.setBundle(bundle);

        dmStoreUploader.uploadFile(new File("irrelevant"), documentTask);

        assertEquals("docUri", documentTask.getBundle().getStitchedDocumentURI());
    }

    @Test
    void uploadNewVersion() throws Exception {
        DocumentTask documentTask = new DocumentTask();
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setStitchedDocumentURI("http://localhost/v1");
        documentTask.setBundle(bundle);

        dmStoreUploader.uploadFile(new File("irrelevant"), documentTask);

        assertEquals("http://localhost/v1", documentTask.getBundle().getStitchedDocumentURI());
    }
}
