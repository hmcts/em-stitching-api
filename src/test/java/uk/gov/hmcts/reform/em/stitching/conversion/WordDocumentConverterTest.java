package uk.gov.hmcts.reform.em.stitching.conversion;

import okhttp3.*;
import org.apache.pdfbox.io.*;
import org.junit.*;

import java.io.*;

import static org.junit.Assert.*;

public class WordDocumentConverterTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    private WordDocumentConverter converter;

    private OkHttpClient okHttpClient;

    @Before
    public void setup() {
        okHttpClient = new OkHttpClient
                .Builder()
            .addInterceptor(WordDocumentConverterTest::intercept)
            .build();

        converter = new WordDocumentConverter("key", "http://example.org", okHttpClient);
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

    private static Response interceptException(Interceptor.Chain chain) throws IOException {
        InputStream file = ClassLoader.getSystemResourceAsStream(PDF_FILENAME);

        return new Response.Builder()
                .body(ResponseBody.create(MediaType.get("application/pdf"), IOUtils.toByteArray(file)))
                .request(chain.request())
                .message("")
                .code(400)
                .protocol(Protocol.HTTP_2)
                .build();
    }

    @Test
    public void accepts() {
        assertEquals("application/msword", converter.accepts().get(0));
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", converter.accepts().get(1));
    }

    @Test
    public void convert() throws IOException {
        File input = new File(ClassLoader.getSystemResource("wordDocument.doc").getPath());
        File output = converter.convert(input);

        assertNotEquals(input.getName(), output.getName());
    }

    @Test(expected = IOException.class)
    public void convertException() throws IOException {
        File input = new File(ClassLoader.getSystemResource("wordDocument.doc").getPath());
        okHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(WordDocumentConverterTest::interceptException)
                .build();
        converter = new WordDocumentConverter("key", "http://example.org", okHttpClient);
        File output = converter.convert(input);

    }
}
