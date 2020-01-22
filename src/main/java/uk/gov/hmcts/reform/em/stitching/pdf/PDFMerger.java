package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.*;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import static org.springframework.util.StringUtils.*;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.*;


@Service
public class PDFMerger {

    public File merge(Bundle bundle, Map<BundleDocument, File> documents, File coverPage) throws IOException, DocumentTaskProcessingException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger(documents, bundle, coverPage);

        return statefulPDFMerger.merge();
    }

    private class StatefulPDFMerger {
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
                this.tableOfContents = new TableOfContents(document, bundle);
                pdfOutline.addItem(currentPageNumber, "Index Page");
                currentPageNumber += tableOfContents.getNumberPages();
            }

            addContainer(bundle);
            pdfOutline.setRootOutlineItemDest(0);
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
                    pdfOutline.closeParentItem();
                } else if (documents.containsKey(item)) {// check if the item (Bundle Doc 1) is present in the keyset for the documents
                    if (bundle.hasCoversheets()) {
                        addCoversheet(item);
                    }
                    addDocument(item); // if present then the item is passed to the addDocument Method
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
                pdfOutline.addParentItem(currentPageNumber, item.getTitle());
                // toc
            }

            currentPageNumber++;
        }

        private void addDocument(SortableBundleItem item) throws IOException {
            PDDocument newDoc = PDDocument.load(documents.get(item)); // item from the bundle is loaded as a new PDDocument
            ArrayList<String> siblings = new ArrayList<>();
            final PDDocumentOutline newDocOutline = newDoc.getDocumentCatalog().getDocumentOutline();
            newDoc.getDocumentCatalog().setDocumentOutline(null);

            merger.appendDocument(document, newDoc);

            if (bundle.getPaginationStyle() != PaginationStyle.off) {
                addPageNumbers(
                        document,
                        bundle.getPaginationStyle(),
                        currentPageNumber,
                        currentPageNumber + newDoc.getNumberOfPages());
            }

            if (tableOfContents != null && newDocOutline!= null) {
               // tableOfContents.addDocument(item.getTitle(), currentPageNumber, newDoc.getNumberOfPages());
                //check if item has any siblings if so add them with the method call
                PDOutlineItem current =newDocOutline.getFirstChild();
               if(current !=null){
                  siblings.add(current.getTitle());
                  current= current.getNextSibling();
               }
                tableOfContents.addDocument(item.getTitle(), currentPageNumber, newDoc.getNumberOfPages(),siblings);
            }
            if (tableOfContents != null && newDocOutline==null) {
               tableOfContents.addDocument(item.getTitle(), currentPageNumber, newDoc.getNumberOfPages());

            }

            pdfOutline.addParentItem(currentPageNumber - (bundle.hasCoversheets() ? 1 : 0), item.getTitle());
            //toc

            if (newDocOutline != null) {
               pdfOutline.mergeDocumentOutline(currentPageNumber, newDocOutline);
                //toc
               }
            pdfOutline.closeParentItem();
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
        private static final String INDEX_PAGE = "Index Page 2448";
        private final List<PDPage> pages = new ArrayList<>();
        private final PDDocument document;
        private final Bundle bundle;
        private int numDocumentsAdded = 0;
        private boolean endOfFolder = false;

        private TableOfContents(PDDocument document, Bundle bundle) throws IOException {
            this.document = document;
            this.bundle = bundle;

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

        public void addDocument(String documentTitle, int pageNumber, int noOfPages, List<String> siblings) throws IOException {
            float yyOffset = getVerticalOffset();

            // add an extra space after a folder so the document doesn't look like it's in the folder
            if (endOfFolder) {
                addText(document, getPage(), " ", 50, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
                yyOffset += LINE_HEIGHT;
                numDocumentsAdded++;
            }

            final PDPage destination = document.getPage(pageNumber);
            if(siblings.size()>0 && siblings!= null){
                addLink(document, getPage(), destination, documentTitle, yyOffset, PDType1Font.HELVETICA, 12, siblings); // titles of the documents are arranged and links are added at the same time
            }
            else {
                addLink(document, getPage(), destination, documentTitle, yyOffset, PDType1Font.HELVETICA, 12); // titles of the documents are arranged and links are added at the same time
            }


            String pageNo = bundle.getPageNumberFormat().getPageNumber(pageNumber, noOfPages);

            addText(document, getPage(), pageNo, 480, yyOffset - 3, PDType1Font.HELVETICA, 12); // to add text for the Total Pages column
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
            int numberTocItems = bundle.hasFolderCoversheets() ? numDocuments + (numFolders * 3) : numDocuments;
            int numPages = (int) Math.ceil((double) numberTocItems / TableOfContents.NUM_ITEMS_PER_PAGE);

            return Math.max(1, numPages);
        }

        public void setEndOfFolder(boolean value) {
            endOfFolder = value;
        }
    }
}
