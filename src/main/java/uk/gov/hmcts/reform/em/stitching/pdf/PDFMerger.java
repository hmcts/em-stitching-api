package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.isEmpty;
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
        private TableOfContents tableOfContents;
        private final Map<BundleDocument, File> documents;
        private final Bundle bundle;
        private static final String BACK_TO_TOP = "Back to top";
        private int currentPageNumber = 0;

        public StatefulPDFMerger(Map<BundleDocument, File> documents, Bundle bundle) {
            this.documents = documents;
            this.bundle = bundle;
        }

        public File merge() throws IOException {
            if (bundle.hasTableOfContents()) {
                this.tableOfContents = new TableOfContents(document, bundle);
                currentPageNumber += tableOfContents.getNumberPages();
            }

            addContainer(bundle);
            final File file = File.createTempFile("stitched", ".pdf");

            document.save(file);
            document.close();

            return file;
        }

        private int addContainer(SortableBundleItem container) throws IOException {
            for (SortableBundleItem item : container.getSortedItems().collect(Collectors.toList())) {
                if (item.getSortedItems().count() > 0) {
                    if (bundle.hasFolderCoversheets()) {
                        addFolderCoversheet(item);
                    }
                    addContainer(item);
                } else if (documents.containsKey(item)) {
                    addDocument(item);
                }
            }

            return currentPageNumber;
        }

        private void addFolderCoversheet(SortableBundleItem item) throws IOException {
            PDPage page = new PDPage();
            document.addPage(page);

            if (tableOfContents != null) {
                tableOfContents.addFolder(item.getTitle(), currentPageNumber);
                addUpwardLink();
            }

            addCenterText(document, page, item.getTitle());

            if (item.getDescription() != null) {
                addText(document, page, item.getDescription(), 80, PDType1Font.HELVETICA,12);
            }

            currentPageNumber++;
        }

        private void addDocument(SortableBundleItem item) throws IOException {
            PDDocument newDoc = PDDocument.load(documents.get(item));
            merger.appendDocument(document, newDoc);
            newDoc.close();

            if (tableOfContents != null) {
                tableOfContents.addDocument(item.getTitle(), currentPageNumber);
                addUpwardLink();
            }

            currentPageNumber += newDoc.getNumberOfPages();
        }

        private void addUpwardLink() throws IOException {
            final float yOffset = 130f;
            final PDPage from = document.getPage(currentPageNumber);

            addLink(document, from, tableOfContents.getPage(), StatefulPDFMerger.BACK_TO_TOP, yOffset, PDType1Font.HELVETICA,12);
        }
    }


    private class TableOfContents {
        private static final int NUM_ITEMS_PER_PAGE = 40;
        private final List<PDPage> pages = new ArrayList<>();
        private final PDDocument document;
        private final Bundle bundle;
        private int numDocumentsAdded = 0;

        private TableOfContents(PDDocument document, Bundle bundle) throws IOException {
            this.document = document;
            this.bundle = bundle;

            for (int i = 0; i < getNumberPages(); i++) {
                final PDPage page = new PDPage();
                pages.add(page);
                document.addPage(page);
            }

            addCenterText(document, getPage(), bundle.getTitle());

            if (!isEmpty(bundle.getDescription())) {
                addText(document, getPage(), bundle.getDescription(), 80, PDType1Font.HELVETICA,12);
            }

            addCenterText(document, getPage(), "Contents", 130);
        }

        public void addDocument(String documentTitle, int pageNumber) throws IOException {
            final float yOffset = getVerticalOffset();
            final PDPage destination = document.getPage(pageNumber);
            final String text = documentTitle + ", p" + (pageNumber + 1);

            addLink(document, getPage(), destination, text, yOffset,PDType1Font.HELVETICA,12);
            numDocumentsAdded++;
        }

        public void addFolder(String title, int pageNumber) throws IOException {
            final PDPage destination = document.getPage(pageNumber);
            float yyOffset = getVerticalOffset();

            addText(document, getPage(), " ", yyOffset, PDType1Font.HELVETICA_BOLD,13);
            yyOffset += LINE_HEIGHT;
            addLink(document, getPage(), destination, title, yyOffset, PDType1Font.HELVETICA_BOLD,13);
            yyOffset += LINE_HEIGHT;
            addText(document, getPage(), " ", yyOffset, PDType1Font.HELVETICA_BOLD,13);

            numDocumentsAdded += 3;
        }

        private float getVerticalOffset() {
            return 170f + ((numDocumentsAdded % NUM_ITEMS_PER_PAGE) * LINE_HEIGHT);
        }

        public PDPage getPage() {
            int pageIndex = (int) Math.floor((double) numDocumentsAdded / NUM_ITEMS_PER_PAGE);

            return pages.get(Math.min(pageIndex, pages.size() - 1));
        }

        public int getNumberPages() {
            int numDocuments = (int) bundle.getSortedDocuments().count();
            int numFolders =  (int) bundle.getNestedFolders().count();
            int numberTocItems = bundle.hasFolderCoversheets() ? numDocuments + (numFolders * 3) : numDocuments;
            int numPages = (int) Math.ceil((double) numberTocItems / TableOfContents.NUM_ITEMS_PER_PAGE);

            return Math.max(1, numPages);
        }

    }
}
