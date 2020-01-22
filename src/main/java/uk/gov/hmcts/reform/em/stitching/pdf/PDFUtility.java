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
import java.util.List;

public final class PDFUtility {
    public static int LINE_HEIGHT = 15;
    public static Boolean titleBoxCreated;
    public static Boolean subtitleBoxCreated;

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

        PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true);
        stream.beginText();
        stream.setFont(pdType1Font, fontSize);
        stream.newLineAtOffset(xxOffset, page.getMediaBox().getHeight() - yyOffset);
        stream.showText(text);
        stream.endText();
        stream.close();
    }
   /* public static void addTextWithSiblings(PDDocument document, PDPage page, String text, float xxOffset,
                               float yyOffset, PDType1Font pdType1Font, int fontSize, List<String> subtitles) throws IOException {
        if (text == null) {
            return;
        }

        //PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true);
        PDPageContentStream stream = new PDPageContentStream(document, page);
        stream.addRect(xxOffset + 5, yyOffset - 3,20,10);
        stream.beginText();
        stream.setFont(pdType1Font, fontSize);
        stream.newLineAtOffset(xxOffset, page.getMediaBox().getHeight() - yyOffset);
        stream.showText(text);
        for(String subtitle:subtitles){
            stream.showText(subtitle);
            stream.newLine();
        }

        stream.endText();
        stream.close();
    }
*/
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
                               PDType1Font font, int fontSize) throws IOException {
        addLink(document, from, to, text, yyOffset, 45, font, fontSize);

    }

    public static void addLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset, float xxOffset,
                               PDType1Font font, int fontSize) throws IOException {
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

        addText(document, from, text, xxOffset + 5, yyOffset - 3, font, fontSize);// text for the link creation in Index Page


    }
    public static void addLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                               PDType1Font font, int fontSize, List<String> subtitles) throws IOException {
        addLink(document, from, to, text, yyOffset, 45, font, fontSize, subtitles);

    }

    public static void addLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset, float xxOffset,
                               PDType1Font font, int fontSize, List<String> subtitles) throws IOException {
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

       addText(document, from, text, xxOffset + 5, yyOffset - 3, font, fontSize);// text for the link creation in Index Page
        titleBoxCreated = true;
        if(subtitles.size()>0 && subtitles!=null){
                for(String subtitle:subtitles){
                    if(titleBoxCreated){
                        addText(document, from, subtitle, xxOffset + 20, yyOffset +8, font, fontSize);// text for the link creation in Index Page
               }
            }
            subtitleBoxCreated =true;
        }
    }

    public static void addRightLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                                    PDType1Font font, int fontSize) throws IOException {
        final float pageWidth = from.getMediaBox().getWidth();
        final float stringWidth = getStringWidth(text, font, fontSize);

        addLink(document, from, to, text, yyOffset, pageWidth - stringWidth - 53, font, fontSize, null);
    }
}
