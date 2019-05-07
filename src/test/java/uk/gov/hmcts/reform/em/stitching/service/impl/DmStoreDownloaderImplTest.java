package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.*;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DmStoreDownloaderImplTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    DmStoreDownloader dmStoreDownloader;

    @Value("${dm-store-app.base-url}")
    private String dmStoreAppBaseUrl;

    @Before
    public void setup() {
        OkHttpClient http = new OkHttpClient
            .Builder()
            .addInterceptor(DmStoreDownloaderImplTest::intercept)
            .build();

        dmStoreDownloader = new DmStoreDownloaderImpl(http, () -> "auth", this.dmStoreAppBaseUrl);
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
    public void doesAppendBinary() throws Exception {
        BundleDocument mockBundleDocument = new BundleDocument();
        mockBundleDocument.setDocumentURI("/AAAA");

        Method binarySuffixAdder = DmStoreDownloaderImpl.class.getDeclaredMethod("uriWithBinarySuffix", String.class);
        binarySuffixAdder.setAccessible(true);
        String processedURI = (String) binarySuffixAdder.invoke(dmStoreDownloader, mockBundleDocument.getDocumentURI());
        Assert.assertEquals(processedURI, "/AAAA/binary");
    }

    @Test
    public void doesNotAppendBinary() throws Exception {
        BundleDocument mockBundleDocument = new BundleDocument();
        mockBundleDocument.setDocumentURI("/AAAA/binary");

        Method binarySuffixAdder = DmStoreDownloaderImpl.class.getDeclaredMethod("uriWithBinarySuffix", String.class);
        binarySuffixAdder.setAccessible(true);
        String processedURI = (String) binarySuffixAdder.invoke(dmStoreDownloader, mockBundleDocument.getDocumentURI());
        Assert.assertEquals(processedURI, "/AAAA/binary");
    }

    @Test
    public void formatDmStoreUriTest() throws Exception {
        BundleDocument mockBundleDocument = new BundleDocument();
        mockBundleDocument.setDocumentURI("genericUriSection/documents/docSpecific");

        Method formatDmStoreUriMethod = DmStoreDownloaderImpl.class.getDeclaredMethod("formatDmStoreUri", String.class);
        formatDmStoreUriMethod.setAccessible(true);
        String processedURI = (String) formatDmStoreUriMethod.invoke(dmStoreDownloader, mockBundleDocument.getDocumentURI());
        Assert.assertEquals(this.dmStoreAppBaseUrl.concat("/documents/docSpecific/binary"), processedURI);
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
