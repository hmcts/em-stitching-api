package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;

import java.io.File;
import java.util.HashSet;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class DmStoreUploaderImplTest {

    DmStoreUploader dmStoreUploader;

    @Before
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
                    MediaType.get("application/json"),
                    "{ _embedded: { documents: [ { _links: { self: { href: 'docUri' } } } ] } }"
                )
            )
            .request(chain.request())
            .message("")
            .code(200)
            .protocol(Protocol.HTTP_2)
            .build();
    }

    @Test(expected = DocumentTaskProcessingException.class)
    public void uploadFile() throws Exception {
        DocumentTask task = new DocumentTask();
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setStitchedDocumentURI("derp");
        task.setBundle(bundle);
        task.setJwt("xxx");

        dmStoreUploader.uploadFile(new File("xyz.abc"), task);
    }

    @Test
    public void upload() throws Exception {
        DocumentTask documentTask = new DocumentTask();
        Bundle bundle = BundleTest.getTestBundle();
        documentTask.setBundle(bundle);

        dmStoreUploader.uploadFile(new File("irrelevant"), documentTask);

        Assert.assertEquals("docUri", documentTask.getBundle().getStitchedDocumentURI());
    }

    @Test
    public void uploadNewVersion() throws Exception {
        DocumentTask documentTask = new DocumentTask();
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setStitchedDocumentURI("http://localhost/v1");
        documentTask.setBundle(bundle);

        dmStoreUploader.uploadFile(new File("irrelevant"), documentTask);

        Assert.assertEquals("http://localhost/v1", documentTask.getBundle().getStitchedDocumentURI());
    }
}
