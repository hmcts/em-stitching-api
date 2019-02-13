package uk.gov.hmcts.reform.em.stitching.conversion;

import com.google.common.collect.Lists;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.image.BufferedImage;
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

        final PDRectangle mediaBox = page.getMediaBox();
        final PDImageXObject pdImage = PDImageXObject.createFromFileByContent(file, document);
        final PDPageContentStream contents = new PDPageContentStream(document, page);
        final Dimension originalSize = new Dimension(pdImage.getWidth(), pdImage.getHeight());
        final Dimension maxSize = new Dimension((int)mediaBox.getWidth(), (int)mediaBox.getHeight());
        final Dimension scaledImageSize = getScaledDimension(originalSize, maxSize);
        final float startX = (mediaBox.getWidth() - scaledImageSize.width) / 2;
        final float startY = (mediaBox.getHeight() - scaledImageSize.height) / 2;

        try {
            contents.drawImage(pdImage, startX, startY, scaledImageSize.width, scaledImageSize.height);
        } finally {
            contents.close();
        }

        File outputFile = File.createTempFile(file.getName(), ".pdf");

        document.save(outputFile);
        document.close();

        return outputFile;
    }

    private static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
        int originalWidth = imgSize.width;
        int originalHeight = imgSize.height;
        int boundWidth = boundary.width;
        int boundHeight = boundary.height;
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // first check if we need to scale width
        if (originalWidth > boundWidth) {
            //scale width to fit
            newWidth = boundWidth;
            //scale height to maintain aspect ratio
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        // then check if we need to scale even with the new height
        if (newHeight > boundHeight) {
            //scale height to fit instead
            newHeight = boundHeight;
            //scale width to maintain aspect ratio
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        return new Dimension(newWidth, newHeight);
    }

}
