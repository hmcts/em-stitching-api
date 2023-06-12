package uk.gov.hmcts.reform.em.stitching.template;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import okhttp3.*;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.IOException;
import java.io.InputStream;

public class DocmosisClientExceptionTest {

    private DocmosisClient client;
    private static final String COVER_PAGE_TEMPLATE_FILE = "test-files/FL-FRM-GOR-ENG-12345.pdf";

    @Before
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
                .body(ResponseBody.create(MediaType.get("application/pdf"), IOUtils.toByteArray(file)))
                .request(chain.request())
                .message("Error!")
                .code(400)
                .protocol(Protocol.HTTP_2)
                .build();
    }

    @Test(expected = DocumentTaskProcessingException.class)
    public void renderTemplate() throws IOException, DocumentTaskProcessingException {
        client.renderDocmosisTemplate(COVER_PAGE_TEMPLATE_FILE, JsonNodeFactory.instance.objectNode().put("caseNo", "12345"));
    }

    @Test(expected = DocumentTaskProcessingException.class)
    public void getDocmosisImage() throws IOException, DocumentTaskProcessingException {
        client.getDocmosisImage(COVER_PAGE_TEMPLATE_FILE);
    }
}
