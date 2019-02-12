package uk.gov.hmcts.reform.em.stitching.conversion;

import com.google.common.collect.Lists;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Uses pdfbox to create a PDF with a single page showing the image.
 */
public class ImageConverter implements FileToPDFConverter {

    @Override
    public List<String> accepts() {
        return Lists.newArrayList(
            "image/bmp",
            "image/gif",
            "image/jpeg",
            "image/png",
            "image/svg+xml",
            "image/tiff",
            "image/jpeg"
        );
    }

    @Override
    public File convert(File file) throws IOException {
        final PDDocument document = new PDDocument();
        final PDPage page = new PDPage();

        document.addPage(page);

        final PDImageXObject pdImage = PDImageXObject.createFromFileByContent(file, document);
        final PDPageContentStream contents = new PDPageContentStream(document, page);
        final PDRectangle mediaBox = page.getMediaBox();
        final float startX = (mediaBox.getWidth() - pdImage.getWidth()) / 2;
        final float startY = (mediaBox.getHeight() - pdImage.getHeight()) / 2;

        try {
            contents.drawImage(pdImage, startX, startY);
        } finally {
            contents.close();
        }

        File outputFile = File.createTempFile(file.getName(), ".pdf");

        document.save(outputFile);
        document.close();

        return outputFile;
    }

}
