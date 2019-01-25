package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.io.File;
import java.io.IOException;

public class PDFMerger {

    private final PDFMergerUtility merger;
    private final PDDocument document = new PDDocument();
    private final PDPage page = new PDPage();
    private int currentPageNumber = 1;

    public PDFMerger(PDFMergerUtility merger) {
        this.merger = merger;

        document.addPage(page);
    }

    public File mergeDocuments() throws IOException {
        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());

        return new File(merger.getDestinationFileName());
    }

    public void add(PDDocument newDoc) throws IOException {
        merger.appendDocument(document, newDoc);

        addTableOfContentsItem();

        currentPageNumber += newDoc.getNumberOfPages();
    }

    private void addTableOfContentsItem() throws IOException {
        PDPageXYZDestination dest = new PDPageXYZDestination();
        dest.setPageNumber(currentPageNumber);
        dest.setLeft(0);
        dest.setTop(0);

        PDActionGoTo action = new PDActionGoTo();
        action.setDestination(dest);

        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(72);
        rect.setLowerLeftY(600);
        rect.setUpperRightX(144);
        rect.setUpperRightY(620);

        PDAnnotationLink link = new PDAnnotationLink();
        link.setAction(action);
        link.setDestination(dest);
        link.setRectangle(rect);

        PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true);
        stream.beginText();
        stream.setNonStrokingColor(0,0,0);
        stream.setFont(PDType1Font.HELVETICA, 8);
        stream.newLineAtOffset(50,220);
        stream.showText("Website: google.com");
        stream.endText();
        stream.close();
    }

}
