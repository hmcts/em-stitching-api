package uk.gov.hmcts.reform.em.stitching.batch;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class BundleFormatterFactory {

    // make documentName an instance variable?

    public static File addCoverSheetToDocument(File file) throws IOException {
        String documentName = file.getName();

        PDDocument document = PDDocument.load(file);
        addEmptyLastPage(document, documentName);
        moveLastPageToFirst(document, documentName);
        addCoversheetTextToFirstPage(document, documentName);

        // TODO Convert PDDocument to file
        File placeholderFile = file;
        return placeholderFile;
    }

    private static PDDocument addEmptyLastPage(PDDocument document, String documentName) throws IOException {
        PDPage emptyPage = new PDPage();
        document.addPage(emptyPage);
        saveDocument(document, documentName);
    }

    private static void moveLastPageToFirst(PDDocument document, String documentName) throws IOException {
        PDPageTree allPages = document.getDocumentCatalog().getPages();
        if (allPages.getCount() > 1) {
            PDPage lastPage = allPages.get(allPages.getCount() - 1);
            allPages.remove(allPages.getCount() - 1);
            PDPage firstPage = allPages.get(0);
            allPages.insertBefore(lastPage, firstPage);
        }
        saveDocument(document, documentName);
    }

    private static void addCoversheetTextToFirstPage(PDDocument document, String documentName) throws IOException {

        PDPage coversheet = document.getPage(1); // Ensure this is a 0 based array
        PDPageContentStream contentStream = new PDPageContentStream(document, coversheet);

        // Formatting cover sheet text
        contentStream.beginText();
        contentStream.setFont(PDType1Font.TIMES_BOLD, 20);
        contentStream.newLineAtOffset(25,500);

        // Add string to cover sheet
        contentStream.showText(documentName);
        contentStream.endText();
        contentStream.close();

        saveDocument(document, documentName);
    }

    private static void saveDocument(PDDocument document, String documentName) throws IOException {
        document.save(documentName);
        document.close();
        }

}