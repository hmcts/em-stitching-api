package uk.gov.hmcts.reform.em.stitching.template;

import okhttp3.*;
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

public class TemplateRenditionClientTest {

    private TemplateRenditionClient client;

    private static final String COVER_PAGE_TEMPLATE = "FL-FRM-GOR-ENG-12345";

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(TemplateRenditionClientTest::intercept)
                .build();

        client = new TemplateRenditionClient(okHttpClient);
        ReflectionTestUtils.setField(client, "docmosisRenderEndpoint", "http://example.org");
        ReflectionTestUtils.setField(client, "docmosisAccessKey", "key");
    }

    private static Response intercept(Interceptor.Chain chain) throws IOException {
        InputStream file = ClassLoader.getSystemResourceAsStream(COVER_PAGE_TEMPLATE + ".pdf");

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
        File input = new File(ClassLoader.getSystemResource(COVER_PAGE_TEMPLATE + ".pdf").getPath());
        File output = client.renderTemplate(COVER_PAGE_TEMPLATE, "json_blob");

        assertNotEquals(input.getName(), output.getName());
        assertEquals(input.length(), output.length());
    }
}
