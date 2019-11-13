package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.*;
import org.apache.pdfbox.pdmodel.common.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.action.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.*;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.*;

public final class PDFUtility {
    public static final int LINE_HEIGHT = 15;

    private PDFUtility() {

    }

    public static void addCenterText(PDDocument document, PDPage page, String text) throws IOException {
        addCenterText(document, page, text, 20);
    }

    public static void addCenterText(PDDocument document, PDPage page, String text, int yyOffset) throws IOException {
        if (text == null) {
            return;
        }

        PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true);

        int fontSize = 14;
        PDFont font = PDType1Font.HELVETICA_BOLD;
        contentStream.setFont(font, fontSize);

        final float stringWidth = getStringWidth(text, font, fontSize);
        final float titleHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
        final float pageHeight = page.getMediaBox().getHeight();
        final float pageWidth = page.getMediaBox().getWidth();

        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - stringWidth) / 2, pageHeight - yyOffset - titleHeight);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.close();
    }

    public static void addText(PDDocument document, PDPage page, String text, float xxOffset,
                               float yyOffset, PDType1Font pdType1Font, int fontSize) throws IOException {
        if (text == null) {
            return;
        }

        final PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true);
        stream.beginText();
        stream.setFont(pdType1Font, fontSize);
        stream.newLineAtOffset(xxOffset, page.getMediaBox().getHeight() - yyOffset);
        stream.showText(text);
        stream.endText();
        stream.close();
    }

    public static void addPageNumbers(PDDocument document, PaginationStyle paginationStyle,
                                      int startNumber, int endNumber) throws IOException {
        for (int i = startNumber; i < endNumber; i++) {
            PDPage page = document.getPage(i);
            Pair<Float, Float> pageNumberLocation = paginationStyle.getPageLocation(page);
            addText(document, page, String.valueOf(i + 1), pageNumberLocation.getFirst(), pageNumberLocation.getSecond(), PDType1Font.HELVETICA_BOLD, 13);
        }
    }

    private static float getStringWidth(String string, PDFont font, int fontSize) throws IOException {
        return font.getStringWidth(string) / 1000 * fontSize;
    }

    public static void addLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                               PDType1Font pdType1Font, int fontSize) throws IOException {
        final PDPageXYZDestination destination = new PDPageXYZDestination();
        destination.setPage(to);

        final PDActionGoTo action = new PDActionGoTo();
        action.setDestination(destination);

        final PDRectangle rectangle = new PDRectangle(45, from.getMediaBox().getHeight() - yyOffset, 500, LINE_HEIGHT);

        final PDAnnotationLink link = new PDAnnotationLink();
        link.setAction(action);
        link.setDestination(destination);
        link.setRectangle(rectangle);
        from.getAnnotations().add(link);

        addText(document, from, text, 50,yyOffset - 3, pdType1Font,fontSize);
    }

}
