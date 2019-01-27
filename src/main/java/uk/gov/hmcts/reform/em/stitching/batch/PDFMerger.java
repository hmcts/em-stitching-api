package uk.gov.hmcts.reform.em.stitching.batch;

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
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class PDFMerger {

    public File merge(List<PDDocument> documents) throws IOException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger();

        return statefulPDFMerger.merge(documents);
    }

    private class StatefulPDFMerger {
        private final PDFMergerUtility merger = new PDFMergerUtility();
        private final PDDocument document = new PDDocument();
        private final PDPage page = new PDPage();
        private int currentPageNumber = 1;

        public StatefulPDFMerger() {
            document.addPage(page);
        }

        public File merge(List<PDDocument> documents) throws IOException {
            for (PDDocument document : documents) {
                add(document);
            }

            File file = File.createTempFile("stitched", ".pdf");

            document.save(file);

            return file;
        }

        private void add(PDDocument newDoc) throws IOException {
            merger.appendDocument(document, newDoc);

            addTableOfContentsItem();

            currentPageNumber += newDoc.getNumberOfPages();
        }

        private void addTableOfContentsItem() throws IOException {
            PDPageXYZDestination destination = new PDPageXYZDestination();
            destination.setPage(document.getPage(currentPageNumber));

            PDActionGoTo action = new PDActionGoTo();
            action.setDestination(destination);

            PDRectangle rectangle = new PDRectangle(50, 600, 200, 20);

            PDAnnotationLink link = new PDAnnotationLink();
            link.setAction(action);
            link.setDestination(destination);
            link.setRectangle(rectangle);

            page.getAnnotations().add(link);

            PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true);
            stream.beginText();
            stream.setNonStrokingColor(0, 0, 0);
            stream.setFont(PDType1Font.HELVETICA, 8);
            stream.newLineAtOffset(50, 600);
            stream.showText("Bundle item, p" + currentPageNumber);
            stream.endText();
            stream.close();
        }
    }
}
