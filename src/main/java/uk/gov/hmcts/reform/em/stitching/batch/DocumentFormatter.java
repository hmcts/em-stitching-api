package uk.gov.hmcts.reform.em.stitching.batch;

import java.io.File;
import java.io.IOException;

import net.logstash.logback.encoder.org.apache.commons.lang.WordUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import static java.lang.Math.round;

public class DocumentFormatter {

    public static File addCoverSheetToDocument(File file) throws IOException {

        String fileName = file.getName();

        PDDocument document = PDDocument.load(file);
        addEmptyLastPage(document);
        moveLastPageToFirst(document);
        addCoversheetTextToFirstPage(document, fileName);

        final File outputFile = File.createTempFile("coversheet", ".pdf");
        document.save(outputFile);
        document.close();

        return outputFile;
    }

    public static void addEmptyLastPage(PDDocument document) {
        PDPage emptyPage = new PDPage();
        document.addPage(emptyPage);
    }

    public static void moveLastPageToFirst(PDDocument document) {
        PDPageTree allPages = document.getDocumentCatalog().getPages();
        if (allPages.getCount() > 1) {
            PDPage lastPage = allPages.get(allPages.getCount() - 1);
            allPages.remove(allPages.getCount() - 1);
            PDPage firstPage = allPages.get(0);
            allPages.insertBefore(lastPage, firstPage);
        }
    }

    public static void addCoversheetTextToFirstPage(PDDocument document, String documentTitle) throws IOException {

        // FONT
        int marginTop = 100;
        int marginLeft = 50;
        int fontSize = 20;
        PDFont font = PDType1Font.HELVETICA_BOLD;

        PDPage coversheet = document.getPage(0);
        PDPageContentStream contentStream = new PDPageContentStream(document, coversheet);
        contentStream.setFont(font, fontSize);

        float stringWidth = font.getStringWidth(documentTitle) / 1000 * fontSize;
        float pageWidth = coversheet.getMediaBox().getWidth();

        String mockLongTitle = "1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 long text";

        if (stringWidth < pageWidth) {
            centerTextInContentStream(contentStream, documentTitle, coversheet, font, fontSize, marginTop);
        } else {
            addWrappedTextToContentStream(contentStream, mockLongTitle, coversheet, fontSize, marginTop, marginLeft);
        }
        contentStream.close();
    }

    private static void centerTextInContentStream(PDPageContentStream contentStream, String inputText, PDPage page, PDFont font, int fontSize, int marginTop) throws IOException {
        float titleHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
        float titleWidth = font.getStringWidth(inputText) / 1000 * fontSize;
        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();

        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - marginTop - titleHeight);
        contentStream.showText(inputText);
        contentStream.endText();
    }


    // This method will not be used for document names, since document names will never have spaces,
    // and will usually not be wider than the page.
    // This method can be used for table of contents, and for the front page (bundle page, description and purpose)
    // I've left a demo function in the unit tests, to demonstrate its functionality.
    private static void addWrappedTextToContentStream(PDPageContentStream contentStream, String inputText, PDPage page, int fontSize, int marginTop, int marginLeft) throws IOException {
        float pageHeight = page.getMediaBox().getHeight();

        String[] linesOfText;
        linesOfText = WordUtils.wrap(inputText, 60).split("\\r?\\n");

        for (int counter=0; counter< linesOfText.length; counter++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(marginLeft, pageHeight - marginTop - (counter*(fontSize + 5))); //
            contentStream.showText(linesOfText[counter]);
            contentStream.endText();
        }
    }
}