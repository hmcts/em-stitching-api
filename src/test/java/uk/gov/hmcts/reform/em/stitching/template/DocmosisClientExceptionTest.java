package uk.gov.hmcts.reform.em.stitching.template;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DocmosisClientExceptionTest {

    private DocmosisClient client;
    private static final String COVER_PAGE_TEMPLATE_FILE = "test-files/FL-FRM-GOR-ENG-12345.pdf";

    @BeforeEach
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(DocmosisClientExceptionTest::intercept)
                .build();

        client = new DocmosisClient(okHttpClient);
        ReflectionTestUtils.setField(client, "docmosisRenderEndpoint", "http://example.org");
        ReflectionTestUtils.setField(client, "docmosisAccessKey", "key");
    }

    private static Response intercept(Interceptor.Chain chain) throws IOException {
        InputStream file = ClassLoader.getSystemResourceAsStream(COVER_PAGE_TEMPLATE_FILE);

        return new Response.Builder()
                .body(ResponseBody.create(IOUtils.toByteArray(file), MediaType.get("application/pdf")))
                .request(chain.request())
                .message("Error!")
                .code(400)
                .protocol(Protocol.HTTP_2)
                .build();
    }

    @Test
    void testRenderTemplateAndDocumentTaskProcessingException() {
        assertThrows(DocumentTaskProcessingException.class, () ->
                client.renderDocmosisTemplate(COVER_PAGE_TEMPLATE_FILE,
                JsonNodeFactory.instance.objectNode().put("caseNo", "12345")));
    }

    @Test
    void testGetDocmosisImageAndDocumentTaskProcessingException() {
        assertThrows(DocumentTaskProcessingException.class, () ->
                client.getDocmosisImage(COVER_PAGE_TEMPLATE_FILE));
    }
}
