package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

public class PDFUtility {

    private PDFUtility() {
        
    }

    public static void addCenterText(PDDocument document, PDPage page, String text) throws IOException {
        addCenterText(document, page, text, 20);
    }

    public static void addCenterText(PDDocument document, PDPage page, String text, int yOffset) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true);

        int fontSize = 20;
        PDFont font = PDType1Font.HELVETICA_BOLD;
        contentStream.setFont(font, fontSize);

        final float stringWidth = getStringWidth(text, font, fontSize);
        final float titleHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
        final float pageHeight = page.getMediaBox().getHeight();
        final float pageWidth = page.getMediaBox().getWidth();

        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - stringWidth) / 2, pageHeight - yOffset - titleHeight);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.close();
    }

    public static void addText(PDDocument document, PDPage page, String text, float yOffset) throws IOException {
        final PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true);
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA, 10);
        stream.newLineAtOffset(50, page.getMediaBox().getHeight() - yOffset);
        stream.showText(text);
        stream.endText();
        stream.close();
    }

    private static float getStringWidth(String string, PDFont font, int fontSize) throws IOException{
        return font.getStringWidth(string) / 1000 * fontSize;
    }

}
