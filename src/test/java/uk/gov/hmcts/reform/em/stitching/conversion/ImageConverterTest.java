package uk.gov.hmcts.reform.em.stitching.conversion;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageConverterTest {

    ImageConverter converter = new ImageConverter();

    @Test
    void accepts() {
        assertEquals("image/bmp", converter.accepts().get(0));
        assertEquals("image/gif", converter.accepts().get(1));
        assertEquals("image/jpeg", converter.accepts().get(2));
        assertEquals("image/png", converter.accepts().get(3));
    }

    @Test
    void convert() throws IOException {
        File input = new File(ClassLoader.getSystemResource("flying-pig.jpg").getPath());
        File output = converter.convert(input);

        assertEquals(".pdf", output.getName().substring(output.getName().lastIndexOf(".")));
    }
}
