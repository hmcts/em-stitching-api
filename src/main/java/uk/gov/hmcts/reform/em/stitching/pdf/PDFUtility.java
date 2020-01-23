package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.*;
import org.apache.pdfbox.pdmodel.common.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.action.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.*;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.*;
import java.util.ArrayList;
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
        stream.newLine();
        stream.endText();
        stream.close();
    }
    public static void addText(PDDocument document, PDPage page, String text, float xxOffset,
                               float yyOffset, PDType1Font pdType1Font, int fontSize, List<PDOutlineItem> subtitles) throws IOException {
        if (text == null) {
            return;
        }

        PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true);
        stream.beginText();
        stream.setFont(pdType1Font, fontSize);
        stream.newLineAtOffset(xxOffset-15, page.getMediaBox().getHeight() - yyOffset+10);
        stream.showText(text);
        stream.newLineAtOffset(xxOffset,-(1.5f *fontSize));
        for(PDOutlineItem subtitle:subtitles){
            createLinkForSubtitle(subtitle,yyOffset,xxOffset,document);
            stream.showText(subtitle.getTitle());
            stream.newLineAtOffset(0,-(1.5f *fontSize));

        }
        stream.endText();
        stream.close();
    }

    private static void createLinkForSubtitle(PDOutlineItem subtitle,float yyOffset,float xxOffset,PDDocument document) throws IOException {
        PDFont font = PDType1Font.TIMES_BOLD_ITALIC;
        final PDPageXYZDestination destination = new PDPageXYZDestination();
        destination.setPage(subtitle.findDestinationPage(document));

        PDActionGoTo action = new PDActionGoTo();
        action.setDestination(destination);

        PDAnnotationLink annotationLink = new PDAnnotationLink();
        annotationLink.setAction(action);

        float offset = (font.getStringWidth(subtitle.getTitle()) / 10) * 18;
        PDRectangle position = new PDRectangle();
        position.setLowerLeftX(offset);
        position.setLowerLeftY(50);
        position.setUpperRightX(offset);
       position.setUpperRightY(50);


        annotationLink.setRectangle(position);
       PDBorderStyleDictionary borderULine = new PDBorderStyleDictionary();
        borderULine.setStyle(PDBorderStyleDictionary.STYLE_UNDERLINE);
        borderULine.setWidth(0);
        annotationLink.setBorderStyle(borderULine);


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

        addText(document, from, text, xxOffset + 5, yyOffset - 3, font, fontSize);


    }
    public static void addLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                               PDType1Font font, int fontSize, List<PDOutlineItem> subtitles) throws IOException {
        addLink(document, from, to, text, yyOffset, 45, font, fontSize, subtitles);

    }

    public static void addLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset, float xxOffset,
                               PDType1Font font, int fontSize, List<PDOutlineItem> subtitles) throws IOException {
        final PDPageXYZDestination destination = new PDPageXYZDestination();
        destination.setPage(to);

        final PDActionGoTo action = new PDActionGoTo();
        action.setDestination(destination);

        final float pageWidth = from.getMediaBox().getWidth();


        final PDRectangle rectangle = new PDRectangle(
            xxOffset,
            from.getMediaBox().getHeight() - yyOffset ,
            pageWidth - xxOffset - 40,
            LINE_HEIGHT);
        final PDAnnotationLink link = new PDAnnotationLink();
        link.setAction(action);
        link.setDestination(destination);
        link.setRectangle(rectangle);
        from.getAnnotations().add(link);

        addText(document, from, text, xxOffset + 20, yyOffset +8, font, fontSize,subtitles);
    }



    public static void addRightLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                                    PDType1Font font, int fontSize) throws IOException {
        final float pageWidth = from.getMediaBox().getWidth();
        final float stringWidth = getStringWidth(text, font, fontSize);

        addLink(document, from, to, text, yyOffset, pageWidth - stringWidth - 53, font, fontSize, null);
    }
}
