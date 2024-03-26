package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.StringUtils.hasLength;

public final class PDFUtility {
    public static final int LINE_HEIGHT = 18;
    public static final int FONT_SIZE_SUBTITLES = 12;

    private static final Logger log = LoggerFactory.getLogger(PDFUtility.class);


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

        float fontSize = 14;
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        contentStream.setFont(font, fontSize);

        final float stringWidth = getStringWidth(text, font, fontSize);
        final float titleHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
        final float pageHeight = page.getMediaBox().getHeight();
        final float pageWidth = page.getMediaBox().getWidth();
        float positionX = calculateCentrePositionX(pageWidth, stringWidth);
        writeText(contentStream, text, positionX,
            pageHeight - yyOffset - titleHeight, font, fontSize, (int) (pageWidth - positionX * 2));
    }

    static void addText(PDDocument document, PDPage page, String text, float xxOffset,
                               float yyOffset, PDType1Font pdType1Font, int fontSize) throws IOException {
        addText(document, page, text, xxOffset, yyOffset, pdType1Font, fontSize, TableOfContents.SPACE_PER_TITLE_LINE);

    }

    static void addText(
            PDDocument document, PDPage page, String text, float xxOffset,
            float yyOffset, PDType1Font pdType1Font,
            int fontSize, int noOfWords) throws IOException {
        if (text == null) {
            return;
        }
        final PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true);
        //Need to sanitize the text, as the getStringWidth() does not except special characters
        final float titleHeight = page.getMediaBox().getHeight() - yyOffset;
        writeText(stream, sanitizeText(text), xxOffset, titleHeight, pdType1Font, fontSize, noOfWords);

    }

    static void addPageNumbers(PDDocument document, PaginationStyle paginationStyle,
                                      int startNumber, int endNumber) throws IOException {
        for (int i = startNumber; i < endNumber; i++) {
            PDPage page = document.getPage(i);
            Pair<Float, Float> pageNumberLocation = paginationStyle.getPageLocation(page);
            addText(document, page, String.valueOf(i + 1), pageNumberLocation.getFirst(),
                pageNumberLocation.getSecond(), new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
        }
    }

    static float getStringWidth(String string, PDFont font, float fontSize) {
        try {
            //Need to sanitize the text, as the getStringWidth() does not except special characters
            return font.getStringWidth(sanitizeText(string)) / 1000 * fontSize;
        } catch (IOException e) {
            log.info("Error getting string width information");
            return 0;
        }
    }

    static void addSubtitleLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                                       PDType1Font pdType1Font) throws IOException {

        float xxOffset = 45;
        int noOfLines = splitString(text, TableOfContents.SPACE_PER_SUBTITLE_LINE,
            pdType1Font, FONT_SIZE_SUBTITLES).length;
        PDAnnotationLink link = generateLink(to, from, xxOffset, yyOffset, noOfLines);
        removeLinkBorder(link);
        addText(document, from, text, xxOffset + 45, yyOffset - 3, pdType1Font, FONT_SIZE_SUBTITLES,
            TableOfContents.SPACE_PER_SUBTITLE_LINE);
    }

    private static PDAnnotationLink generateLink(
        PDPage to, PDPage from,
        float xxOffset, float yyOffset, int noOfLines) throws IOException {
        final PDPageXYZDestination destination = new PDPageXYZDestination();
        destination.setPage(to);

        final PDActionGoTo action = new PDActionGoTo();
        action.setDestination(destination);

        final float pageWidth = from.getMediaBox().getWidth();

        int height = LINE_HEIGHT * noOfLines;

        final PDRectangle rectangle = new PDRectangle(
                xxOffset,
                from.getMediaBox().getHeight() - yyOffset - height + LINE_HEIGHT,
                pageWidth - xxOffset - 40,
                height
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

    static void addLink(
            PDDocument document, PDPage from, PDPage to,
            String text, float yyOffset,
            PDType1Font font, int fontSize, int noOfLines) throws IOException {
        addLink(document, from, to, text, yyOffset, 45, font, fontSize, noOfLines);
    }

    private static void addLink(
            PDDocument document, PDPage from, PDPage to,
            String text, float yyOffset, float xxOffset,
            PDType1Font font, int fontSize, int noOflines) throws IOException {

        PDAnnotationLink link = generateLink(to, from, xxOffset, yyOffset, noOflines);
        removeLinkBorder(link);

        addText(document, from, text, xxOffset, yyOffset, font, fontSize);
    }

    static void addRightLink(PDDocument document, PDPage from, PDPage to, String text, float yyOffset,
                                    PDType1Font font, int fontSize) throws IOException {
        final float pageWidth = from.getMediaBox().getWidth();
        final float stringWidth = getStringWidth(text, font, fontSize);

        addLink(document, from, to, text, yyOffset, pageWidth - stringWidth - 53, font, fontSize, 1);
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

        String [] tmpText = splitString(text, noOfWords, pdType1Font, fontSize);
        for (int k = 0;k < tmpText.length;k++) {
            contentStream.beginText();
            contentStream.setFont(pdType1Font, fontSize);
            contentStream.newLineAtOffset(positionX, positionY);
            contentStream.showText(sanitizeText(tmpText[k]));
            contentStream.endText();
            positionY = positionY - LINE_HEIGHT;
        }
        contentStream.setLineWidth((float) 0.25);
        contentStream.close();

    }

    static String [] splitString(String text, int lineWidth, PDType1Font pdType1Font, float fontSize) {
        /* pdfBox doesn't support linebreaks. Therefore,
         * following steps are required to automatically put linebreaks in the pdf
         * 1) split each word in string and put them into an array of string, e.g. String [] parts
         * 2) create a list to store the lines
         * 3) put the words into a string builder, until the limit of width for a line is reached,
         * 4) add the contents of the string builder to the list
         * 5) clear the builder to start a new line
         * 6) loop until all words are added to lines
         *
         */
        if (!hasLength(text)) {
            return ArrayUtils.toArray();
        }
        String [] words = text.split(" "); //save each word into an array-element
        List<String> lines = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        float currentLineWidth = 0;
        for (String word : words) {
            float wordWidth = getStringWidth(word, pdType1Font, fontSize);
            if (currentLineWidth + wordWidth <= lineWidth) {
                currentLineWidth = addWordToCurrentLine(stringBuilder, currentLineWidth, word, wordWidth);
                continue;
            }
            // Start a new line
            processLine(lines, stringBuilder);
            stringBuilder.setLength(0);
            currentLineWidth = 0;
            currentLineWidth = addWordToCurrentLine(stringBuilder, currentLineWidth, word, wordWidth);
        }
        processLine(lines, stringBuilder);
        return lines.toArray(new String[0]);
    }

    private static void processLine(List<String> lines, StringBuilder stringBuilder) {
        if (!stringBuilder.isEmpty()) {
            stringBuilder.setLength(stringBuilder.length() - 1);
            lines.add(stringBuilder.toString());
        }
    }

    private static float addWordToCurrentLine(StringBuilder stringBuilder,
                                              float currentLineWidth, String word, float wordWidth) {
        stringBuilder.append(word);
        currentLineWidth += wordWidth;
        stringBuilder.append(" ");
        currentLineWidth++;
        return currentLineWidth;
    }

    private static float calculateCentrePositionX(float pageWidth, float stringWidth) {

        float temp = stringWidth - pageWidth;
        if (temp > pageWidth) {
            return calculateCentrePositionX(pageWidth, temp);
        }
        return Math.abs(temp) / 2;

    }
}
