package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.IOException;

import static org.springframework.util.StringUtils.hasLength;

public final class PDFUtility {
    public static final int LINE_HEIGHT = 18;
    public static final int LINE_HEIGHT_SUBTITLES = 12;

    private PDFUtility() {

    }

    static void addCenterText(PDDocument document, PDPage page, String text) throws IOException {
        addCenterText(document, page, text, 20);
    }

    static void addCenterText(PDDocument document, PDPage page, String text, int yyOffset) throws IOException {
        if (text == null) {
            return;
        }

        PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true);

        int fontSize = 14;
        PDType1Font font = PDType1Font.HELVETICA_BOLD;
        contentStream.setFont(font, fontSize);

        //Need to sanitize the text, as the getStringWidth() does not except special characters
        final float stringWidth = getStringWidth(sanitizeText(text), font, fontSize);
        final float titleHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
        final float pageHeight = page.getMediaBox().getHeight();
        final float pageWidth = page.getMediaBox().getWidth();

        if (stringWidth <= 550) {
            contentStream.beginText();
            contentStream.newLineAtOffset((pageWidth - stringWidth) / 2, pageHeight - yyOffset - titleHeight);
            contentStream.showText(text);
            contentStream.endText();
            contentStream.close();
        } else {
            writeText(contentStream, text, calculatePositionX(pageWidth, stringWidth), pageHeight - yyOffset - titleHeight,
                font, fontSize, 45);
        }
    }

    static void addText(PDDocument document, PDPage page, String text, float xxOffset,
                               float yyOffset, PDType1Font pdType1Font, int fontSize) throws IOException {
        addText(document, page, text, xxOffset, yyOffset, pdType1Font, fontSize, 55);

    }

    static void addText(PDDocument document, PDPage page, String text, float xxOffset,
                               float yyOffset, PDType1Font pdType1Font, int fontSize, int noOfWords) throws IOException {
        if (text == null) {
            return;
        }
        final PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true);
        //Need to sanitize the text, as the getStringWidth() does not except special characters
        final float stringWidth = getStringWidth(sanitizeText(text), pdType1Font, fontSize);
        final float titleHeight = page.getMediaBox().getHeight() - yyOffset;
        if (stringWidth <= 550) {
            stream.beginText();
            stream.setFont(pdType1Font, fontSize);
            stream.newLineAtOffset(xxOffset, page.getMediaBox().getHeight() - yyOffset);
            stream.showText(sanitizeText(text));
            stream.endText();
            stream.close();
        } else {
            writeText(stream, sanitizeText(text), xxOffset, titleHeight, pdType1Font, fontSize, noOfWords);
        }

    }

    static void addPageNumbers(PDDocument document, PaginationStyle paginationStyle,
                                      int startNumber, int endNumber) throws IOException {
        for (int i = startNumber; i < endNumber; i++) {
            PDPage page = document.getPage(i);
            Pair<Float, Float> pageNumberLocation = paginationStyle.getPageLocation(page);
            addText(document, page, String.valueOf(i + 1), pageNumberLocation.getFirst(), pageNumberLocation.getSecond(), PDType1Font.HELVETICA_BOLD, 13);
        }
    }

    static float getStringWidth(String string, PDFont font, int fontSize) throws IOException {
        return font.getStringWidth(string) / 1000 * fontSize;
    }

    static void addSubtitleLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                                       PDType1Font pdType1Font) throws IOException {

        float xxOffset = 45;
        PDAnnotationLink link = generateLink(to, from, xxOffset, yyOffset);
        removeLinkBorder(link);
        addText(document, from, text, xxOffset + 45, yyOffset - 3, pdType1Font, LINE_HEIGHT_SUBTITLES);
    }

    private static PDAnnotationLink generateLink(PDPage to, PDPage from, float xxOffset, float yyOffset) throws IOException {
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

    private static void removeLinkBorder(PDAnnotationLink link) {
        PDBorderStyleDictionary borderLine = new PDBorderStyleDictionary();
        borderLine.setStyle(PDBorderStyleDictionary.STYLE_UNDERLINE);
        borderLine.setWidth(0);
        link.setBorderStyle(borderLine);
    }

    static void addLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                               PDType1Font font, int fontSize) throws IOException {
        addLink(document, from, to, text, yyOffset, 45, font, fontSize);
    }

    private static void addLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset, float xxOffset,
                               PDType1Font font, int fontSize) throws IOException {

        PDAnnotationLink link = generateLink(to, from, xxOffset, yyOffset);
        removeLinkBorder(link);

        addText(document, from, text, xxOffset, yyOffset, font, fontSize);
    }

    static void addRightLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                                    PDType1Font font, int fontSize) throws IOException {
        final float pageWidth = from.getMediaBox().getWidth();
        final float stringWidth = getStringWidth(text, font, fontSize);

        addLink(document, from, to, text, yyOffset, pageWidth - stringWidth - 53, font, fontSize);
    }

    static String sanitizeText(String rawString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawString.length(); i++) {
            if (WinAnsiEncoding.INSTANCE.contains(rawString.charAt(i))) {
                sb.append(rawString.charAt(i));
            }
        }
        return sb.toString();
    }

    private static void writeText(PDPageContentStream contentStream, String text, float positionX, float positionY,
                                 PDType1Font pdType1Font, float fontSize, int noOfWords) throws IOException {

        String [] tmpText = splitString(text, noOfWords);
        for (int k = 0;k < tmpText.length;k++) {
            contentStream.beginText();
            contentStream.setFont(pdType1Font, fontSize);
            contentStream.newLineAtOffset(positionX, positionY);
            contentStream.showText(sanitizeText(tmpText[k]));
            contentStream.endText();
            positionY = positionY - 20;
        }
        contentStream.setLineWidth((float) 0.25);
        contentStream.close();

    }

    static String [] splitString(String text, int noOfWords) {
        /* pdfBox doesnt support linebreaks. Therefore, following steps are requierd to automatically put linebreaks in the pdf
         * 1) split each word in string that has to be linefeded and put them into an array of string, e.g. String [] parts
         * 2) create an array of stringbuffer with (textlength/(number of characters in a line)), e.g. 280/70=5 >> we need 5 linebreaks!
         * 3) put the parts into the stringbuffer[i], until the limit of maximum number of characters in a line is allowed,
         * 4) loop until stringbuffer.length < linebreaks
         *
         */
        if (!hasLength(text)){
            return ArrayUtils.toArray();
        }
        var linebreaks = text.length() / noOfWords; //how many linebreaks do I need?
        String [] newText = new String[linebreaks + 1];
        String tmpText = text;
        String [] parts = tmpText.split(" "); //save each word into an array-element

        //split each word in String into a an array of String text.
        StringBuffer [] stringBuffer = new StringBuffer[linebreaks + 1]; //StringBuffer is necessary because of
        // manipulating text
        var i = 0; //initialize counter
        var totalTextLength = 0;
        for (var k = 0;k < linebreaks + 1;k++) {
            stringBuffer[k] = new StringBuffer();
            while (true) {
                if (i >= parts.length) {
                    break; //avoid NullPointerException
                }
                totalTextLength = totalTextLength + parts[i].length(); //count each word in String
                if (totalTextLength > noOfWords) {
                    break; //put each word in a stringbuffer until string length is >80
                }
                stringBuffer[k].append(parts[i]);
                stringBuffer[k].append(" ");
                i++;
            }
            //reset counter, save linebreaked text into the array, finally convert it to a string
            totalTextLength = 0;
            newText[k] = stringBuffer[k].toString();
        }
        return newText;
    }

    private static float calculatePositionX(float pageWidth, float stringWidth) {

        float temp = stringWidth - pageWidth;
        if (temp > pageWidth) {
            return calculatePositionX(pageWidth, temp);
        }
        return (pageWidth - temp) / 3;

    }
}
