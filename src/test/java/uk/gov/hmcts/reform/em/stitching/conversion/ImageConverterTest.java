package uk.gov.hmcts.reform.em.stitching.conversion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageConverterTest {

    private final ImageConverter converter = new ImageConverter();
    private final List<File> tempFilesCreated = new ArrayList<>();

    @AfterEach
    void tearDown() throws IOException {
        for (File file : tempFilesCreated) {
            Files.deleteIfExists(file.toPath());
        }
        tempFilesCreated.clear();
    }

    private File createTestImageFile(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Test", width / 10, height / 2);
        g2d.dispose();

        File tempFile = File.createTempFile("test-image-", "." + format);
        ImageIO.write(image, format, tempFile);
        tempFilesCreated.add(tempFile);
        return tempFile;
    }

    @Test
    void acceptsReturnsCorrectMimeTypes() {
        List<String> acceptedTypes = converter.accepts();
        assertEquals(7, acceptedTypes.size());
        assertTrue(acceptedTypes.contains("image/bmp"));
        assertTrue(acceptedTypes.contains("image/gif"));
        assertTrue(acceptedTypes.contains("image/jpeg"));
        assertTrue(acceptedTypes.contains("image/png"));
        assertTrue(acceptedTypes.contains("image/svg+xml"));
        assertTrue(acceptedTypes.contains("image/tiff"));
        assertTrue(acceptedTypes.contains("image/jpg"));
    }

    @Test
    void convertJpgToPdfCreatesPdfFile() throws IOException {
        File input = new File(ClassLoader.getSystemResource("flying-pig.jpg").getPath());
        File output = converter.convert(input);
        tempFilesCreated.add(output);

        assertNotNull(output);
        assertTrue(output.exists());
        assertTrue(output.length() > 0, "Output PDF should not be empty");
        assertTrue(output.getName().endsWith(".pdf"));
    }

    @Test
    void convertPngNarrowerTallerThanPage() throws IOException {
        File narrowTallImage = createTestImageFile(300, 1000, "png");
        File outputPdf = converter.convert(narrowTallImage);
        tempFilesCreated.add(outputPdf);

        assertNotNull(outputPdf);
        assertTrue(outputPdf.exists());
        assertTrue(outputPdf.length() > 0, "Output PDF should not be empty");
        assertTrue(outputPdf.getName().endsWith(".pdf"));
    }
}