package uk.gov.hmcts.reform.em.stitching.batch;

import java.io.File;
import java.io.IOException;

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

    private static void addEmptyLastPage(PDDocument document) {
        PDPage emptyPage = new PDPage();
        document.addPage(emptyPage);
    }

    private static void moveLastPageToFirst(PDDocument document) {
        PDPageTree allPages = document.getDocumentCatalog().getPages();
        if (allPages.getCount() > 1) {
            PDPage lastPage = allPages.get(allPages.getCount() - 1);
            allPages.remove(allPages.getCount() - 1);
            PDPage firstPage = allPages.get(0);
            allPages.insertBefore(lastPage, firstPage);
        }
    }

    private static void addCoversheetTextToFirstPage(PDDocument document, String documentName) throws IOException {

        PDPage coversheet = document.getPage(0); // Ensure this is a 0 based array
        PDPageContentStream contentStream = new PDPageContentStream(document, coversheet);


        int marginTop = 100;
        PDFont font = PDType1Font.HELVETICA_BOLD; // Or whatever font you want.
        int fontSize = 20; // Or whatever font size you want.
        float titleWidth = font.getStringWidth(documentName) / 1000 * fontSize;
        float titleHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;


        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset((coversheet.getMediaBox().getWidth() - titleWidth) / 2, coversheet.getMediaBox().getHeight() - marginTop - titleHeight);

        contentStream.showText(documentName);
        contentStream.endText();
        contentStream.close();

    }


}