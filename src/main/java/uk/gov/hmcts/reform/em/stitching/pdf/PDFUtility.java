package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.*;
import org.apache.pdfbox.pdmodel.common.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.pdmodel.interactive.action.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.*;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;
import java.io.*;

public final class PDFUtility {
    public static final int LINE_HEIGHT = 15;
    public static final int LINE_HEIGHT_SUBTITLES = 12;

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

        final PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true);
        stream.beginText();
        stream.setFont(pdType1Font, fontSize);
        stream.newLineAtOffset(xxOffset, page.getCropBox().getHeight() - yyOffset);
        stream.showText(sanitizeText(text));
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

    public static void addSubtitleLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                                       PDType1Font pdType1Font) throws IOException {
        addSubtitleLink(document, from, to, text, yyOffset, 45, pdType1Font);
    }

    public static void addSubtitleLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset, float xxOffset,
                                       PDType1Font pdType1Font) throws IOException {

        PDAnnotationLink link = generateLink(to, from, xxOffset, yyOffset);
        removeLinkBorder(link);
        addText(document, from, text, xxOffset + 45, yyOffset - 3, pdType1Font, LINE_HEIGHT_SUBTITLES);
    }

    public static PDAnnotationLink generateLink(PDPage to, PDPage from, float xxOffset, float yyOffset) throws IOException {
        final PDPageXYZDestination destination = new PDPageXYZDestination();
        destination.setPage(to);

        final PDActionGoTo action = new PDActionGoTo();
        action.setDestination(destination);

        final float pageWidth = from.getMediaBox().getWidth();

        final PDRectangle rectangle = new PDRectangle(
                xxOffset,
                from.getMediaBox().getHeight() - yyOffset,
                pageWidth - xxOffset - 40,
                LINE_HEIGHT
        );

        final PDAnnotationLink link = new PDAnnotationLink();
        link.setAction(action);
        link.setDestination(destination);
        link.setRectangle(rectangle);
        from.getAnnotations().add(link);
        return link;
    }

    public static void removeLinkBorder(PDAnnotationLink link) {
        PDBorderStyleDictionary borderLine = new PDBorderStyleDictionary();
        borderLine.setStyle(PDBorderStyleDictionary.STYLE_UNDERLINE);
        borderLine.setWidth(0);
        link.setBorderStyle(borderLine);
    }

    public static void addLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                               PDType1Font font, int fontSize) throws IOException {
        addLink(document, from, to, text, Pair.of(Float.valueOf(45), yyOffset), font, fontSize);
    }

    public static void addLink(PDDocument document, PDPage from, PDPage to, String text, Pair<Float, Float> offset,
                               PDType1Font font, int fontSize) throws IOException {

        PDAnnotationLink link = generateLink(to, from, offset.getFirst(), offset.getSecond());
        removeLinkBorder(link);

        addText(document, from, text, offset.getFirst(), offset.getSecond(), font, fontSize);
    }

    public static void addRightLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                                    PDType1Font font, int fontSize) throws IOException {
        final float pageWidth = from.getMediaBox().getWidth();
        final float stringWidth = getStringWidth(text, font, fontSize);

        addLink(document, from, to, text, Pair.of(pageWidth - stringWidth - 53, yyOffset), font, fontSize);
    }

    private static String sanitizeText(String rawString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawString.length(); i++) {
            if (WinAnsiEncoding.INSTANCE.contains(rawString.charAt(i))) {
                sb.append(rawString.charAt(i));
            }
        }
        return sb.toString();
    }
}
