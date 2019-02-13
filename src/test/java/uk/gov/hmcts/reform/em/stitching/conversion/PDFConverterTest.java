package uk.gov.hmcts.reform.em.stitching.conversion;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class PDFConverterTest {

    private final PDFConverter converter = new PDFConverter();

    @Test
    public void accepts() {
        List<String> result = converter.accepts();

        assertEquals("application/pdf", result.get(0));
    }

    @Test
    public void convert() {
        File file = new File("/tmp");

        assertEquals(file, converter.convert(file));
    }
}