package uk.gov.hmcts.reform.em.stitching.conversion;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PDFConverterTest {

    private final PDFConverter converter = new PDFConverter();

    @Test
    void accepts() {
        List<String> result = converter.accepts();

        assertEquals("application/pdf", result.get(0));
    }

    @Test
    void convert() {
        File file = new File("/tmp");

        assertEquals(file, converter.convert(file));
    }
}
