package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.*;
import org.apache.pdfbox.io.*;
import org.junit.*;
import org.junit.runner.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.data.util.*;
import org.springframework.test.context.junit4.*;
import uk.gov.hmcts.reform.em.stitching.*;
import uk.gov.hmcts.reform.em.stitching.domain.*;
import uk.gov.hmcts.reform.em.stitching.service.*;

import java.io.*;
import java.util.stream.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DmStoreDownloaderImplTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    private DmStoreDownloader dmStoreDownloader;

    private OkHttpClient http;

    @Autowired
    private DmStoreUriFormatter dmStoreUriFormatter;

    @Before
    public void setup() {
        http = new OkHttpClient
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

    private static Response interceptException(Interceptor.Chain chain) {

        return new Response.Builder()
                .request(chain.request())
                .message("")
                .code(400)
                .protocol(Protocol.HTTP_2)
                .build();
    }

    @Test
    public void downloadFileException() {
        http = new OkHttpClient
                .Builder()
                .addInterceptor(DmStoreDownloaderImplTest::interceptException)
                .build();

        dmStoreDownloader = new DmStoreDownloaderImpl(http, () -> "auth", dmStoreUriFormatter);

        BundleDocument mockBundleDocument1 = new BundleDocument();
        BundleDocument mockBundleDocument2 = new BundleDocument();
        mockBundleDocument1.setDocumentURI("/AAAA");
        mockBundleDocument2.setDocumentURI("/BBBB");
        try {
            dmStoreDownloader.downloadFiles(Stream.of(mockBundleDocument1, mockBundleDocument2));
        } catch (DocumentTaskProcessingException ex) {
            Assert.assertTrue(ex instanceof DocumentTaskProcessingException);
        }

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
