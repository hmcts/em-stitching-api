package uk.gov.hmcts.reform.em.stitching.batch;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class BundleFormatter {

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

    // TODO Make this private
    public static void addEmptyLastPage(PDDocument document) {
        PDPage emptyPage = new PDPage();
        document.addPage(emptyPage);
    }

    // TODO Make this private
    public static void moveLastPageToFirst(PDDocument document) {
        PDPageTree allPages = document.getDocumentCatalog().getPages();
        if (allPages.getCount() > 1) {
            PDPage lastPage = allPages.get(allPages.getCount() - 1);
            allPages.remove(allPages.getCount() - 1);
            PDPage firstPage = allPages.get(0);
            allPages.insertBefore(lastPage, firstPage);
        }
    }

    // TODO Make this private
    public static void addCoversheetTextToFirstPage(PDDocument document, String documentName) throws IOException {

        PDPage coversheet = document.getPage(0); // Ensure this is a 0 based array
        PDPageContentStream contentStream = new PDPageContentStream(document, coversheet);

        // Formatting cover sheet text
        contentStream.beginText();
        contentStream.setFont(PDType1Font.TIMES_BOLD, 20);
        contentStream.newLineAtOffset(25,500);

        // Add string to cover sheet
        contentStream.showText(documentName);
        contentStream.endText();
        contentStream.close();
    }


}