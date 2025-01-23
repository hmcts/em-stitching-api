package uk.gov.hmcts.reform.em.stitching.template;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;


class DocmosisClientWatermarkTest {

    private DocmosisClient client;
    private static final String RENDERED_WATERMARK_FILE = "test-files/watermark_rendered.pdf";
    private static final String WATERMARK_FILE = "test-files/schmcts.png";

    @BeforeEach
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(DocmosisClientWatermarkTest::intercept)
                .build();

        client = new DocmosisClient(okHttpClient);
        ReflectionTestUtils.setField(client, "docmosisRenderEndpoint", "http://example.org");
        ReflectionTestUtils.setField(client, "docmosisAccessKey", "key");
    }

    private static Response intercept(Interceptor.Chain chain) throws IOException {
        InputStream file = ClassLoader.getSystemResourceAsStream(RENDERED_WATERMARK_FILE);

        return new Response.Builder()
                .body(ResponseBody.create(IOUtils.toByteArray(file),MediaType.get("application/pdf")))
                .request(chain.request())
                .message("")
                .code(200)
                .protocol(Protocol.HTTP_2)
                .build();
    }

    @Test
    void getDocmosisImage() throws IOException, DocumentTaskProcessingException {
        File input = new File(ClassLoader.getSystemResource(WATERMARK_FILE).getPath());
        File output = client.getDocmosisImage(WATERMARK_FILE);

        assertNotEquals(input.getName(), output.getName());
    }
}
