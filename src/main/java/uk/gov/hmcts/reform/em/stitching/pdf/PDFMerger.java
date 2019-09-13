package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.commons.collections.CollectionUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.stereotype.Service;
import org.springframework.util.*;
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
            //TODO - Need to check if FolderCoverSheet is required. If user says - Required. Then that also needs to go on the TOC as na item

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

        // todo: extract to class?
        private void addFolderCoversheet(SortableBundleItem item) throws IOException {
            PDPage page = new PDPage();
            document.addPage(page);

            if (tableOfContents != null) {
                // todo add separate method for addFolder as it takes up three lines on TOC
                tableOfContents.addItem(item.getTitle(), currentPageNumber);
            }

            addCenterText(document, page, item.getTitle());
            // todo add back to top

            if (item.getDescription() != null) {
                addText(document, page, item.getDescription(), 80);
            }

            currentPageNumber++;
        }

        private void addDocument(SortableBundleItem item) throws IOException {
            PDDocument newDoc = PDDocument.load(documents.get(item));
            merger.appendDocument(document, newDoc);
            newDoc.close();

            if (tableOfContents != null) {
                tableOfContents.addItem(item.getTitle(), currentPageNumber);
                addUpwardLink();
            }

            currentPageNumber += newDoc.getNumberOfPages();
        }

        private void addUpwardLink() throws IOException {
            final float yOffset = 130f;
            final PDPage from = document.getPage(currentPageNumber);

            addLink(document, from, tableOfContents.getPage(), StatefulPDFMerger.BACK_TO_TOP, yOffset);
        }
    }


    private class TableOfContents {
        private static final int NUM_ITEMS_PER_PAGE = 5;
        private static final int MIN_TOC_PAGE = 1;
        private final List<PDPage> pages = new ArrayList<>();
        private final PDDocument document;
        private final Bundle bundle;
        private int numDocumentsAdded = 1;

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
                addText(document, getPage(), bundle.getDescription(), 80);
            }

            addCenterText(document, getPage(), "Contents", 130);
        }

        public void addItem(String documentTitle, int pageNumber) throws IOException {
            final float yOffset = 170f + numDocumentsAdded * LINE_HEIGHT;
            final PDPage destination = document.getPage(pageNumber);
            final String text = documentTitle + ", p" + (pageNumber + 1);

            addLink(document, getPage(), destination, text, yOffset);
            numDocumentsAdded++;
        }

        public PDPage getPage() {
            int pageIndex = (int) Math.floor(numDocumentsAdded / NUM_ITEMS_PER_PAGE);

            return pages.get(pageIndex);
        }

        public int getNumberPages() {
            // todo get total number of items and total number of documents.
            // the difference between them should be the number of folders.
            int numFolders =  getNumOfFolders(bundle.getFolders());
            int numFolderRows = numFolders * 3;
            int numberTocItems = numFolderRows + (int) bundle.getSortedDocuments().count();
            if (numberTocItems <= TableOfContents.NUM_ITEMS_PER_PAGE) {
                return MIN_TOC_PAGE;
            } else {
                return (int) Math.ceil(numberTocItems / TableOfContents.NUM_ITEMS_PER_PAGE);
            }
        }

        private int getNumOfFolders(List<BundleFolder> folders) {
            int count = 0;
            if (CollectionUtils.isNotEmpty(folders)) {
                for (BundleFolder bundleFolder : folders) {
                    count++;
                    count += getNumOfFolders(bundleFolder.getFolders());
                }
            }
            return count;
        }
    }
}
