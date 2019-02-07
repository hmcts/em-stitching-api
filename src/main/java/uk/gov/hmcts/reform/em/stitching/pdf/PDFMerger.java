package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.*;

@Service
public class PDFMerger {

    public File merge(Bundle bundle, List<PDDocument> documents) throws IOException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger();

        return statefulPDFMerger.merge(bundle, documents);
    }

    private class StatefulPDFMerger {
        private static final int LINE_HEIGHT = 15;
        private final PDFMergerUtility merger = new PDFMergerUtility();
        private final PDDocument document = new PDDocument();
        private final PDPage page = new PDPage();
        private int currentPageNumber = 1;

        public File merge(Bundle bundle, List<PDDocument> documents) throws IOException {
            setupFirstPage(bundle);

            int documentIndex = 1;

            for (PDDocument d : documents) {
                add(d, documentIndex++);
                d.close();
            }

            final File file = File.createTempFile("stitched", ".pdf");

            document.save(file);
            document.close();

            return file;
        }

        private void setupFirstPage(Bundle bundle) throws IOException {
            document.addPage(page);
            addCenterText(document, page, bundle.getBundleTitle());
            addText(document, page, bundle.getDescription(), 80);
            addCenterText(document, page, "Contents", 100);
        }

        private void add(PDDocument newDoc, int documentIndex) throws IOException {
            merger.appendDocument(document, newDoc);

            addTableOfContentsItem(getDocumentTitle(newDoc), documentIndex);

            currentPageNumber += newDoc.getNumberOfPages();
        }

        private void addTableOfContentsItem(String documentTitle, int documentIndex) throws IOException {
            final PDPageXYZDestination destination = new PDPageXYZDestination();
            destination.setPage(document.getPage(currentPageNumber));

            final PDActionGoTo action = new PDActionGoTo();
            action.setDestination(destination);

            final float yOffset = 150f + documentIndex * LINE_HEIGHT;
            final PDRectangle rectangle = new PDRectangle(45, page.getMediaBox().getHeight() - yOffset, 500, LINE_HEIGHT);

            final PDBorderStyleDictionary underline = new PDBorderStyleDictionary();
            underline.setStyle(PDBorderStyleDictionary.STYLE_UNDERLINE);

            final PDAnnotationLink link = new PDAnnotationLink();
            link.setAction(action);
            link.setDestination(destination);
            link.setRectangle(rectangle);
            link.setBorderStyle(underline);

            page.getAnnotations().add(link);

            addText(document, page, documentTitle + ", p" + (currentPageNumber + 1), yOffset - 3);
        }
    }
}
