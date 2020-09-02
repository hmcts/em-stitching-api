package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.*;

@Service
public class PDFMerger {
    private static final String INDEX_PAGE = "Index Page";

    public File merge(Bundle bundle, Map<BundleDocument, File> documents, File coverPage) throws IOException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger(documents, bundle, coverPage);

        return statefulPDFMerger.merge();
    }

    private class StatefulPDFMerger {
        private final Logger log = LoggerFactory.getLogger(StatefulPDFMerger.class);
        private final PDFMergerUtility merger = new PDFMergerUtility();
        private final PDDocument document = new PDDocument();
        private final PDFOutline pdfOutline = new PDFOutline(document);
        private TableOfContents tableOfContents;
        private final Map<BundleDocument, File> documents;
        private final Bundle bundle;
        private static final String BACK_TO_TOP = "Back to index";
        private int currentPageNumber = 0;
        private File coverPage;

        public StatefulPDFMerger(Map<BundleDocument, File> documents, Bundle bundle, File coverPage) {
            this.documents = documents;
            this.bundle = bundle;
            this.coverPage = coverPage;
        }

        public File merge() throws IOException {
            pdfOutline.addBundleItem(bundle.getTitle());

            if (coverPage != null) {
                PDDocument coverPageDocument = PDDocument.load(coverPage);
                merger.appendDocument(document, coverPageDocument);
                currentPageNumber += coverPageDocument.getNumberOfPages();
                pdfOutline.addItem(0, "Cover Page");
            }

            if (bundle.hasTableOfContents()) {
                this.tableOfContents = new TableOfContents(document, bundle, documents);
                pdfOutline.addItem(currentPageNumber, INDEX_PAGE);
                currentPageNumber += tableOfContents.getNumberPages();
            }

            addContainer(bundle);
            pdfOutline.setRootOutlineItemDest();

            final File file = File.createTempFile("stitched", ".pdf");
            document.save(file);
            document.close();

            return file;
        }

        private int addContainer(SortableBundleItem container) throws IOException {
            for (SortableBundleItem item : container.getSortedItems().collect(Collectors.toList())) {
                if (item.getSortedItems().count() > 0) {
                    if (bundle.hasFolderCoversheets()) {
                        addCoversheet(item);
                    }
                    addContainer(item);
                } else if (documents.containsKey(item)) {
                    if (bundle.hasCoversheets()) {
                        addCoversheet(item);
                    }

                    try {
                        addDocument(item);
                    } catch (Exception e) {
                        String filename = documents.get(item).getName();
                        String name = item.getTitle();
                        String error = String.format("Error processing %s, %s", name, filename);
                        log.error(error, e);

                        throw new IOException(error, e);
                    }
                }
            }

            if (tableOfContents != null) {
                tableOfContents.setEndOfFolder(true);
            }

            return currentPageNumber;
        }

        private void addCoversheet(SortableBundleItem item) throws IOException {
            PDPage page = new PDPage();
            document.addPage(page);

            if (tableOfContents != null) {
                if (item.getSortedItems().count() > 0) {
                    tableOfContents.addFolder(item.getTitle(), currentPageNumber);
                }
                addUpwardLink();
            }

            addCenterText(document, page, item.getTitle(), 330);

            if (item.getSortedItems().count() > 0) {
                pdfOutline.addItem(currentPageNumber, item.getTitle());
            }
            currentPageNumber++;
        }

        private void addDocument(SortableBundleItem item) throws IOException {
            PDDocument newDoc = PDDocument.load(documents.get(item));
            final PDDocumentOutline newDocOutline = newDoc.getDocumentCatalog().getDocumentOutline();

            if (bundle.hasCoversheets()) {
                pdfOutline.addItem(document.getNumberOfPages() - 1, item.getTitle());
            } else if (newDocOutline != null) {
                PDOutlineItem outlineItem = pdfOutline.createHeadingItem(newDoc.getPage(0), item.getTitle());
                newDocOutline.addFirst(outlineItem);
            }

            try {
                merger.appendDocument(document, newDoc);
            } catch (IndexOutOfBoundsException e) {
                newDoc.getDocumentCatalog().setStructureTreeRoot(new PDStructureTreeRoot());
                log.info("Setting new PDF structure tree of " + item.getTitle());
                merger.appendDocument(document, newDoc);
            }

            if (bundle.getPaginationStyle() != PaginationStyle.off) {
                addPageNumbers(
                        document,
                        bundle.getPaginationStyle(),
                        currentPageNumber,
                        currentPageNumber + newDoc.getNumberOfPages());
            }
            if (tableOfContents != null && newDocOutline != null) {
                ArrayList<PDOutlineItem> siblings = new ArrayList<>();
                PDOutlineItem anySubtitlesForItem = newDocOutline.getFirstChild();
                while (anySubtitlesForItem != null) {
                    siblings.add(anySubtitlesForItem);
                    anySubtitlesForItem = anySubtitlesForItem.getNextSibling();
                }
                tableOfContents.addDocument(item.getTitle(), currentPageNumber, newDoc.getNumberOfPages());
                for (PDOutlineItem subtitle : siblings) {
                    tableOfContents.addDocumentWithOutline(item.getTitle(), currentPageNumber, subtitle);
                }
            }
            if (tableOfContents != null && newDocOutline == null) {
                tableOfContents.addDocument(item.getTitle(), currentPageNumber, newDoc.getNumberOfPages());
            }
            currentPageNumber += newDoc.getNumberOfPages();
            newDoc.close();
        }

        private void addUpwardLink() throws IOException {
            final float yOffset = 730f;
            final PDPage from = document.getPage(currentPageNumber);

            addRightLink(document, from, tableOfContents.getPage(), StatefulPDFMerger.BACK_TO_TOP, yOffset, PDType1Font.HELVETICA,12);
        }
    }


    private class TableOfContents {
        private static final int NUM_ITEMS_PER_PAGE = 40;
        private final List<PDPage> pages = new ArrayList<>();
        private final PDDocument document;
        private final Bundle bundle;
        private final Map<BundleDocument, File> documents;
        private int numDocumentsAdded = 0;
        private boolean endOfFolder = false;
        private final Logger logToc = LoggerFactory.getLogger(TableOfContents.class);

        private TableOfContents(PDDocument document, Bundle bundle, Map<BundleDocument, File> documents) throws IOException {
            this.document = document;
            this.bundle = bundle;
            this.documents = documents;

            for (int i = 0; i < getNumberPages(); i++) {
                final PDPage page = new PDPage();
                pages.add(page);
                document.addPage(page);
            }

            if (!isEmpty(bundle.getDescription())) {
                addText(document, getPage(), bundle.getDescription(), 50,80, PDType1Font.HELVETICA,12);
            }

            addCenterText(document, getPage(), INDEX_PAGE, 130);
            String pageNumberTitle = bundle.getPageNumberFormat().getPageNumberTitle();
            addText(document, getPage(), pageNumberTitle, 480,165, PDType1Font.HELVETICA,12);
        }

        public void addDocument(String documentTitle, int pageNumber, int noOfPages) throws IOException {
            float yyOffset = getVerticalOffset();

            // add an extra space after a folder so the document doesn't look like it's in the folder
            if (endOfFolder) {
                addText(document, getPage(), " ", 50, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
                yyOffset += LINE_HEIGHT;
                numDocumentsAdded++;
            }

            final PDPage destination = document.getPage(pageNumber);

            addLink(document, getPage(), destination, documentTitle, yyOffset, PDType1Font.HELVETICA, 12);

            String pageNo = bundle.getPageNumberFormat().getPageNumber(pageNumber, noOfPages);

            addText(document, getPage(), pageNo, 480, yyOffset - 3, PDType1Font.HELVETICA, 12);
            numDocumentsAdded++;
            endOfFolder = false;
        }

        public void addDocumentWithOutline(String documentTitle, int pageNumber, PDOutlineItem sibling) throws IOException {
            float yyOffset = getVerticalOffset();
            PDPage destination = new PDPage();
            // add an extra space after a folder so the document doesn't look like it's in the folder
            if (endOfFolder) {
                addText(document, getPage(), " ", 50, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
                yyOffset += LINE_HEIGHT;
                numDocumentsAdded++;
            }

            try {
                if (Objects.nonNull(sibling)) {
                    if (sibling.getDestination() instanceof PDPageDestination) {
                        PDPageDestination pd = (PDPageDestination) sibling.getDestination();
                        destination = document.getPage(pd.retrievePageNumber() + pageNumber);
                    }

                    if (!sibling.getTitle().equalsIgnoreCase(documentTitle)) {
                        addSubtitleLink(document, getPage(), destination, sibling.getTitle(), yyOffset,PDType1Font.HELVETICA);
                    }
                }
            } catch (Exception e) {
                logToc.error("error processing subtitles:",e);
            }

            numDocumentsAdded++;
            endOfFolder = false;
        }

        public void addFolder(String title, int pageNumber) throws IOException {
            final PDPage destination = document.getPage(pageNumber);
            float yyOffset = getVerticalOffset();

            addText(document, getPage(), " ", 50, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
            yyOffset += LINE_HEIGHT;
            addLink(document, getPage(), destination, title, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
            yyOffset += LINE_HEIGHT;
            addText(document, getPage(), " ", 50, yyOffset, PDType1Font.HELVETICA_BOLD, 13);

            numDocumentsAdded += 3;
            endOfFolder = false;
        }

        private float getVerticalOffset() {
            return 190f + ((numDocumentsAdded % NUM_ITEMS_PER_PAGE) * LINE_HEIGHT);
        }

        public PDPage getPage() {
            int pageIndex = (int) Math.floor((double) numDocumentsAdded / NUM_ITEMS_PER_PAGE);

            return pages.get(Math.min(pageIndex, pages.size() - 1));
        }

        public int getNumberPages() {
            int numDocuments = (int) bundle.getSortedDocuments().count();
            int numFolders = (int) bundle.getNestedFolders().count();
            int numSubtitle = bundle.getSubtitles(bundle, documents);
            int numberTocItems = bundle.hasFolderCoversheets() ? numDocuments + (numFolders * 3) + numSubtitle : numDocuments + numSubtitle;
            int numPages = (int) Math.ceil((double) numberTocItems / TableOfContents.NUM_ITEMS_PER_PAGE);

            return Math.max(1, numPages);
        }

        public void setEndOfFolder(boolean value) {
            endOfFolder = value;
        }
    }
}
