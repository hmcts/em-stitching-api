package uk.gov.hmcts.reform.em.stitching.conversion;

import okhttp3.*;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DocmosisConverterTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    private DocmosisConverter converter;

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient
            .Builder()
            .addInterceptor(DocmosisConverterTest::intercept)
            .build();

        converter = new DocmosisConverter("key", "http://example.org", okHttpClient);
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

    private static Response errorIntercept(Interceptor.Chain chain) throws IOException {
        InputStream file = ClassLoader.getSystemResourceAsStream(PDF_FILENAME);

        return new Response.Builder()
                .body(ResponseBody.create(MediaType.get("text/plain"), "Wrong Data in Body"))
                .request(chain.request())
                .message("Incorrect request")
                .code(400)
                .protocol(Protocol.HTTP_2)
                .build();
    }

    @Test
    public void accepts() {
        assertEquals("application/msword", converter.accepts().get(0));
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", converter.accepts().get(1));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",converter.accepts().get(4));
        assertEquals("application/vnd.ms-excel",converter.accepts().get(5));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.template",converter.accepts().get(6));
        assertEquals("application/vnd.openxmlformats-officedocument.presentationml.presentation",converter.accepts().get(7));
        assertEquals("application/vnd.ms-powerpoint", converter.accepts().get(8));
        assertEquals("application/vnd.openxmlformats-officedocument.presentationml.template", converter.accepts().get(9));
        assertEquals("application/vnd.openxmlformats-officedocument.presentationml.slideshow", converter.accepts().get(10));
        assertEquals("text/plain", converter.accepts().get(12));
        assertEquals("application/rtf", converter.accepts().get(13));
    }

    @Test
    public void convert() throws IOException {
        File input = new File(ClassLoader.getSystemResource("wordDocument.doc").getPath());
        File output = converter.convert(input);

        assertNotEquals(input.getName(), output.getName());
    }

    @Test
    public void convertExcelTest() throws IOException {
        File input = new File(ClassLoader.getSystemResource("TestExcelConversion.xlsx").getPath());
        File output = converter.convert(input);

        assertNotEquals(input.getName(), output.getName());
    }


    @Test
    public void convertPptTest() throws IOException {
        File input = new File(ClassLoader.getSystemResource("potential_and_kinetic.ppt").getPath());
        File output = converter.convert(input);

        assertNotEquals(input.getName(), output.getName());
    }


    @Test
    public void convertPptxTest() throws IOException {
        File input = new File(ClassLoader.getSystemResource("Performance_Out.pptx").getPath());
        File output = converter.convert(input);

        assertNotEquals(input.getName(), output.getName());
    }

    @Test
    public void convertTextTest() throws IOException {
        File input = new File(ClassLoader.getSystemResource("sample_text_file.txt").getPath());
        File output = converter.convert(input);

        assertNotEquals(input.getName(), output.getName());
    }

    @Test
    public void convertRichTextFileTest() throws IOException {
        File input = new File(ClassLoader.getSystemResource("rtf.rtf").getPath());
        File output = converter.convert(input);

        assertNotEquals(input.getName(), output.getName());
    }

    @Test(expected = IOException.class)
    public void convertError() throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(DocmosisConverterTest::errorIntercept)
                .build();

        converter = new DocmosisConverter("key", "http://example.org", okHttpClient);

        File input = new File(ClassLoader.getSystemResource("rtf.rtf").getPath());
        converter.convert(input);

    }
}
