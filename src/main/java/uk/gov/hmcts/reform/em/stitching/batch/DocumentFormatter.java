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

    public static void addCoversheetTextToFirstPage(PDDocument document, String documentName) throws IOException {

        PDPage coversheet = document.getPage(0);
        PDPageContentStream contentStream = new PDPageContentStream(document, coversheet);

        //        CENTER TITLE
        int marginTop = 100;
        int fontSize = 20;
        PDFont font = PDType1Font.HELVETICA_BOLD;

        float titleWidth = font.getStringWidth(documentName) / 1000 * fontSize;
        float titleHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;

        contentStream.setFont(font, fontSize);
        contentStream.beginText();
        contentStream.newLineAtOffset((coversheet.getMediaBox().getWidth() - titleWidth) / 2, coversheet.getMediaBox().getHeight() - marginTop - titleHeight);
        contentStream.showText(documentName);
        contentStream.endText();
        contentStream.close();

    }

    //  THIS METHOD WILL WRAP TEXT IF WIDTH > WIDER THAN PAGE
    private static void addWrappedTextToFirstPage(PDDocument document, String textToBeWrapped) throws IOException {

        PDPage firstPage = document.getPage(0);
        PDPageContentStream contentStream = new PDPageContentStream(document, firstPage);

        int fontSize = 20;
        String[] linesOfText = null;
        String lineOfText = null;
        linesOfText = WordUtils.wrap(textToBeWrapped, 500, null, true).split("\\r?\\n");

        for (int counter=0; counter< linesOfText.length; counter++) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, fontSize);
            contentStream.newLineAtOffset(50,600-counter*(fontSize+5));
            lineOfText = linesOfText[counter];
            contentStream.showText(lineOfText);
            contentStream.endText();
        }
    }
}