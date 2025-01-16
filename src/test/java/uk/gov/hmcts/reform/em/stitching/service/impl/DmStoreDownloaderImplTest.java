package uk.gov.hmcts.reform.em.stitching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.rest.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class DmStoreDownloaderImplTest {

    private static final String PDF_FILENAME = "test-files/annotationTemplate.pdf";

    DmStoreDownloader dmStoreDownloader;

    @Autowired
    DmStoreUriFormatter dmStoreUriFormatter;

    @BeforeEach
    public void setup() {
        OkHttpClient http = new OkHttpClient
            .Builder()
            .addInterceptor(DmStoreDownloaderImplTest::intercept)
            .build();

        dmStoreDownloader = new DmStoreDownloaderImpl(http, () -> "auth", dmStoreUriFormatter, new ObjectMapper());
    }

    private static Response intercept(Interceptor.Chain chain) throws IOException {

        if (chain.request().url().toString().endsWith("/binary")) {
            InputStream file = ClassLoader.getSystemResourceAsStream(PDF_FILENAME);

            return new Response.Builder()
                    .body(ResponseBody.create(IOUtils.toByteArray(file),MediaType.get("application/pdf")))
                    .request(chain.request())
                    .message("")
                    .code(200)
                    .protocol(Protocol.HTTP_2)
                    .build();
        } else {
            return new Response.Builder()
                    .body(ResponseBody.create(
                            "{ \"mimeType\": \"application/pdf\", \"_links\": { \"binary\" : { \"href\": \"http://www.google/documentes/88/binary\" } } }",
                            MediaType.get("application/json")))
                    .request(chain.request())
                    .message("")
                    .code(200)
                    .protocol(Protocol.HTTP_2)
                    .build();
        }

    }

    @Test
    void downloadFile() throws DocumentTaskProcessingException {
        BundleDocument mockBundleDocument1 = new BundleDocument();
        BundleDocument mockBundleDocument2 = new BundleDocument();
        mockBundleDocument1.setDocumentURI("/AAAA");
        mockBundleDocument2.setDocumentURI("/BBBB");

        Stream<BundleDocument> mockBundleStream = Stream.of(mockBundleDocument1, mockBundleDocument2);
        Stream<Pair<BundleDocument, FileAndMediaType>> results = dmStoreDownloader.downloadFiles(mockBundleStream);

        assertNotNull(results);
        assertEquals(2, results.count());
    }

    @Test
    void copyResponseToFile() throws Exception {
        BundleDocument mockBundleDocument1 = new BundleDocument();
        mockBundleDocument1.setDocumentURI("http://localhost/AAAA");
        Stream<Pair<BundleDocument, FileAndMediaType>> results =
                dmStoreDownloader.downloadFiles(Stream.of(mockBundleDocument1));
        Pair<BundleDocument, FileAndMediaType> result = results.toList().get(0);

        assertEquals(result.getFirst(), mockBundleDocument1);
        assertTrue(result.getSecond().getFile().exists());
    }
}
