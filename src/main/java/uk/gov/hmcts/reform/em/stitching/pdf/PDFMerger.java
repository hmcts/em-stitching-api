package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.*;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.*;


@Service
public class PDFMerger {

    public File merge(Bundle bundle, Map<BundleDocument, File> documents, File coverPage, File documentImage)
            throws IOException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger(documents, bundle, coverPage, documentImage);
  
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
        private File documentImage;

        public StatefulPDFMerger(Map<BundleDocument, File> documents, Bundle bundle, File coverPage, File documentImage) {
            this.documents = documents;
            this.bundle = bundle;
            this.coverPage = coverPage;
            this.documentImage = documentImage;
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
                pdfOutline.addParentItem(currentPageNumber, item.getTitle());
            }

            currentPageNumber++;
        }

        private void addDocument(SortableBundleItem item) throws IOException {
            PDDocument newDoc = PDDocument.load(documents.get(item));
            final PDDocumentOutline newDocOutline = newDoc.getDocumentCatalog().getDocumentOutline();
            newDoc.getDocumentCatalog().setDocumentOutline(null);

            try (Overlay overlay = new Overlay()) {
                if (documentImage != null) {
                    addDocumentImage(newDoc, overlay);
                }

                merger.appendDocument(document, newDoc);

                newDoc.close();
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

            pdfOutline.addParentItem(currentPageNumber - (bundle.hasCoversheets() ? 1 : 0), item.getTitle());
            if (newDocOutline != null) {
                pdfOutline.mergeDocumentOutline(currentPageNumber, newDocOutline);
            }
            pdfOutline.closeParentItem();
            currentPageNumber += newDoc.getNumberOfPages();
        }

        private void addDocumentImage(PDDocument document, Overlay overlay) throws IOException {
            PDDocument overlayDocument = new PDDocument();
            PDPage overlayPage = new PDPage();
            overlayDocument.addPage(overlayPage);

            PDImageXObject pdImage = PDImageXObject.createFromFileByExtension(documentImage, overlayDocument);
            PDRectangle mediaBox = overlayPage.getMediaBox();

            bundle.getDocumentImage().verifyCoordinates();
            double startX = (mediaBox.getWidth() * (Double.valueOf(bundle.getDocumentImage().getCoordinateX()) / 100.0)) - (pdImage.getWidth() / 2);
            double startY = (mediaBox.getHeight() * (Double.valueOf(bundle.getDocumentImage().getCoordinateY()) / 100.0)) - (pdImage.getHeight() / 2);

            try (PDPageContentStream contentStream = new PDPageContentStream(overlayDocument, overlayPage)) {
                contentStream.drawImage(pdImage, (float) startX, (float) startY);
            }

            overlay.setInputPDF(document);
            overlay.setOverlayPosition(bundle.getDocumentImage().getImageRendering().getPosition());

            if (bundle.getDocumentImage().getImageRenderingLocation() == ImageRenderingLocation.allPages) {
                overlay.setAllPagesOverlayPDF(overlayDocument);
            } else {
                overlay.setFirstPageOverlayPDF(overlayDocument);
            }

            HashMap<Integer, String> overlayMap = new HashMap<>();
            overlay.overlay(overlayMap);
        }

        private void addUpwardLink() throws IOException {
            final float yOffset = 730f;
            final PDPage from = document.getPage(currentPageNumber);

            addRightLink(document, from, tableOfContents.getPage(), StatefulPDFMerger.BACK_TO_TOP, yOffset, PDType1Font.HELVETICA,12);
        }
    }


    private class TableOfContents {
        private static final int NUM_ITEMS_PER_PAGE = 40;
        private static final String INDEX_PAGE = "Index Page";
        private final List<PDPage> pages = new ArrayList<>();
        private final PDDocument document;
        private final Bundle bundle;
        private final Map<BundleDocument, File> documents;
        private int numDocumentsAdded = 0;
        private boolean endOfFolder = false;

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

            if (sibling.getDestination() instanceof PDPageDestination) {
                PDPageDestination pd = (PDPageDestination) sibling.getDestination();
                destination = document.getPage(pd.retrievePageNumber() + pageNumber);
            }

            if (!sibling.getTitle().equalsIgnoreCase(documentTitle)) {
                addSubtitleLink(document, getPage(), destination, sibling.getTitle(), yyOffset, PDType1Font.HELVETICA);
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
