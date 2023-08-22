package uk.gov.hmcts.reform.em.stitching.template;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DocmosisClientRenderTest {

    private DocmosisClient client;
    private static final String COVER_PAGE_TEMPLATE_FILE = "test-files/FL-FRM-GOR-ENG-12345.pdf";

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(DocmosisClientRenderTest::intercept)
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
                .message("")
                .code(200)
                .protocol(Protocol.HTTP_2)
                .build();
    }

    @Test
    public void renderTemplate() throws IOException, DocumentTaskProcessingException {
        File input = new File(ClassLoader.getSystemResource(COVER_PAGE_TEMPLATE_FILE).getPath());
        File output = client.renderDocmosisTemplate(
                COVER_PAGE_TEMPLATE_FILE,
                JsonNodeFactory.instance.objectNode().put("caseNo", "12345"));

        assertNotEquals(input.getName(), output.getName());
        assertEquals(input.length(), output.length());
    }
}
