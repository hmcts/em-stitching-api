package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.*;

@Service
public class PDFMerger {

    public File merge(Bundle bundle, List<Pair<BundleDocument, File>> documents) throws IOException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger();

        return statefulPDFMerger.merge(bundle, documents);
    }

    private class StatefulPDFMerger {
        private final PDFMergerUtility merger = new PDFMergerUtility();
        private final PDDocument document = new PDDocument();
        private final Deque<TableOfContents> tableOfContents = new ArrayDeque<>();

        public File merge(Bundle bundle, List<Pair<BundleDocument, File>> documents) throws IOException {
            if (bundle.hasTableOfContents()) {
                tableOfContents.push(new TableOfContents(document, bundle.getBundleTitle(), bundle.getDescription()));
            }

            int currentPageNumber = tableOfContents.size();

            for (Pair<BundleDocument, File> pair : documents) {
                final int numNewPages = add(pair.getSecond());

                if (!tableOfContents.isEmpty()) {
                    tableOfContents.peek().addItem(pair.getFirst().getDocTitle(), currentPageNumber);

                    if (bundle.hasCoversheets()) {
                        addBackToTopLink(currentPageNumber, tableOfContents.peek().getPage());
                    }
                }

                currentPageNumber += numNewPages;
            }

            final File file = File.createTempFile("stitched", ".pdf");

            document.save(file);
            document.close();

            return file;
        }

        private int add(File file) throws IOException {
            PDDocument newDoc = PDDocument.load(file);
            merger.appendDocument(document, newDoc);
            newDoc.close();

            return newDoc.getNumberOfPages();
        }

        private void addBackToTopLink(int currentPageNumber, PDPage tableOfContents) throws IOException {
            final float yOffset = 120f;
            final PDPage from = document.getPage(currentPageNumber);

            addLink(document, from, tableOfContents, "Back to top", yOffset);
        }
    }

    private class TableOfContents {
        private final PDPage page = new PDPage();
        private final PDDocument document;
        private int documentIndex = 1;

        private TableOfContents(PDDocument document, String title, String description) throws IOException {
            this.document = document;

            document.addPage(page);
            addCenterText(document, page, title);

            if (description != null) {
                addText(document, page, description, 80);
            }

            addCenterText(document, page, "Contents", 100);
        }

        public void addItem(String documentTitle, int pageNumber) throws IOException {
            final float yOffset = 150f + documentIndex * LINE_HEIGHT;
            final PDPage destination = document.getPage(pageNumber);
            final String text = documentTitle + ", p" + (pageNumber + 1);

            addLink(document, page, destination, text, yOffset);
            documentIndex++;
        }

        public PDPage getPage() {
            return page;
        }
    }
}
