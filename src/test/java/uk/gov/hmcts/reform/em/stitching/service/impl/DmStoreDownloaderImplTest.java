package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.*;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DmStoreDownloaderImplTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    DmStoreDownloader dmStoreDownloader;

    @Autowired
    DmStoreUriFormatter dmStoreUriFormatter;

    @Before
    public void setup() {
        OkHttpClient http = new OkHttpClient
            .Builder()
            .addInterceptor(DmStoreDownloaderImplTest::intercept)
            .build();

        dmStoreDownloader = new DmStoreDownloaderImpl(http, () -> "auth", dmStoreUriFormatter);
    }

    private static Response intercept(Interceptor.Chain chain) throws IOException {
        InputStream file = ClassLoader.getSystemResourceAsStream(PDF_FILENAME);

        return new Response.Builder()
            .body(ResponseBody.create(MediaType.get("application/pdf"), IOUtils.toByteArray(file)))
            .request(chain.request())
            .message("")
            .code(200)
            .protocol(Protocol.HTTP_2)
            .build();
    }

    @Test(expected = RuntimeException.class)
    public void downloadFile() throws Exception {
        BundleDocument mockBundleDocument1 = new BundleDocument();
        BundleDocument mockBundleDocument2 = new BundleDocument();
        mockBundleDocument1.setDocumentURI("/AAAA");
        mockBundleDocument2.setDocumentURI("/BBBB");
        Stream<Pair<BundleDocument, File>> results = dmStoreDownloader.downloadFiles(Stream.of(mockBundleDocument1, mockBundleDocument2));

        results.collect(Collectors.toList());
    }

    @Test
    public void copyResponseToFile() throws Exception {
        BundleDocument mockBundleDocument1 = new BundleDocument();
        mockBundleDocument1.setDocumentURI("http://localhost/AAAA");
        Stream<Pair<BundleDocument, File>> results = dmStoreDownloader.downloadFiles(Stream.of(mockBundleDocument1));
        Pair<BundleDocument, File> result = results.collect(Collectors.toList()).get(0);

        Assert.assertEquals(result.getFirst(), mockBundleDocument1);
        Assert.assertTrue(result.getSecond().exists());
    }
}
