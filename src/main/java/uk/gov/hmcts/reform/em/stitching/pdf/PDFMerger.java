package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.*;

@Service
public class PDFMerger {

    public File merge(Bundle bundle, Map<BundleDocument, File> documents) throws IOException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger(documents, bundle);

        return statefulPDFMerger.merge();
    }

    private class StatefulPDFMerger {
        private final PDFMergerUtility merger = new PDFMergerUtility();
        private final PDDocument document = new PDDocument();
        private final Deque<TableOfContents> tableOfContents = new ArrayDeque<>();
        private final Map<BundleDocument, File> documents;
        private final Bundle bundle;

        public StatefulPDFMerger(Map<BundleDocument, File> documents, Bundle bundle) {
            this.documents = documents;
            this.bundle = bundle;
        }

        public File merge() throws IOException {
            addContainer(bundle, bundle.hasTableOfContents(), 0);
            final File file = File.createTempFile("stitched", ".pdf");

            document.save(file);
            document.close();

            return file;
        }

        private int addContainer(SortableBundleItem container,
                                 boolean addTableOfContents,
                                 int currentPageNumber) throws IOException {

            if (addTableOfContents) {
                tableOfContents.push(new TableOfContents(document, container.getTitle(), container.getDescription()));
                currentPageNumber++;
            }

            for (SortableBundleItem item : container.getSortedItems().collect(Collectors.toList())) {
                if (item.getSortedItems().count() > 0) {
                    currentPageNumber = addContainer(item, bundle.hasFolderCoversheets(), currentPageNumber);
                } else if (documents.containsKey(item)) {
                    currentPageNumber = addDocument(item, currentPageNumber);
                }
            }

            if (addTableOfContents) {
                tableOfContents.pop();
            }

            return currentPageNumber;
        }

        private int addDocument(SortableBundleItem item, int currentPageNumber) throws IOException {
            PDDocument newDoc = PDDocument.load(documents.get(item));
            merger.appendDocument(document, newDoc);
            newDoc.close();

            if (!tableOfContents.isEmpty()) {
                tableOfContents.peek().addItem(item.getTitle(), currentPageNumber);

                if (bundle.hasCoversheets()) {
                    addBackToTopLink(currentPageNumber, tableOfContents.peek().getPage());
                }
            }

            return currentPageNumber + newDoc.getNumberOfPages();
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
