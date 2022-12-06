package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.commons.collections4.CollectionUtils;
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

import static java.lang.Math.max;
import static org.springframework.util.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.LINE_HEIGHT;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addCenterText;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addLink;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addPageNumbers;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addRightLink;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addSubtitleLink;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addText;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.splitString;

@Service
public class PDFMerger {
    private static final String INDEX_PAGE = "Index Page";

    public File merge(Bundle bundle, Map<BundleDocument, File> documents, File coverPage) throws IOException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger(documents, bundle, coverPage);

        return statefulPDFMerger.merge();
    }

    private static class StatefulPDFMerger {
        private final Logger log = LoggerFactory.getLogger(StatefulPDFMerger.class);
        private final PDFMergerUtility merger = new PDFMergerUtility();
        private final PDDocument document = new PDDocument();
        private final PDFOutline pdfOutline = new PDFOutline(document);
        private TableOfContents tableOfContents;
        private final Map<BundleDocument, File> documents;
        private final Bundle bundle;
        private static final String BACK_TO_TOP = "Back to index";
        private int currentPageNumber = 0;
        private final File coverPage;

        private StatefulPDFMerger(Map<BundleDocument, File> documents, Bundle bundle, File coverPage) {
            this.documents = documents;
            this.bundle = bundle;
            this.coverPage = coverPage;
        }

        private File merge() throws IOException {
            try {
                pdfOutline.addBundleItem(bundle.getTitle());

                if (coverPage != null) {
                    try (PDDocument coverPageDocument = PDDocument.load(coverPage)) {
                        merger.appendDocument(document, coverPageDocument);
                        currentPageNumber += coverPageDocument.getNumberOfPages();
                        pdfOutline.addItem(0, "Cover Page");
                    }
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
                return file;
            }
            finally {
                document.close();
            }
        }

        private void addContainer(SortableBundleItem container) throws IOException {
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
            try (PDDocument newDoc = PDDocument.load(documents.get(item))) {
                addDocument(item, newDoc);
            }
        }

        private void addDocument(SortableBundleItem item, PDDocument newDoc) throws IOException {
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
                log.debug("Setting new PDF structure tree of " + item.getTitle());
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
        }

        private void addUpwardLink() throws IOException {
            final float yOffset = 730f;
            final PDPage from = document.getPage(currentPageNumber);

            addRightLink(document, from, tableOfContents.getPage(), StatefulPDFMerger.BACK_TO_TOP, yOffset, PDType1Font.HELVETICA,12);
        }
    }


    private static class TableOfContents {
        private static final int NUM_LINES_PER_PAGE = 38;
        private final List<PDPage> pages = new ArrayList<>();
        private final PDDocument document;
        private final Bundle bundle;
        private final Map<BundleDocument, File> documents;
        private static final float TOP_MARGIN_OFFSET = 40f;
        private static final int CHARS_PER_LINE = 100;
        private static final int CHARS_PER_TITLE_LINE = 45;
        private int numLinesAdded = 0;
        private boolean endOfFolder = false;
        private final Logger logToc = LoggerFactory.getLogger(TableOfContents.class);

        private TableOfContents(PDDocument document, Bundle bundle, Map<BundleDocument, File> documents) throws IOException {
            this.document = document;
            this.bundle = bundle;
            this.documents = documents;

            int noOfPages = getNumberPages();
            for (int i = 0; i < noOfPages; i++) {
                final PDPage page = new PDPage();
                pages.add(page);
                document.addPage(page);
            }

            if (!isEmpty(bundle.getDescription())) {
                addText(document, getPage(), bundle.getDescription(), 50,80, PDType1Font.HELVETICA,12, 80);
            }

            int descriptionLines = splitString(bundle.getDescription(), CHARS_PER_LINE).length;
            int indexVerticalOffset = max(descriptionLines * 20 + 70, 90);
            addCenterText(document, getPage(), INDEX_PAGE, indexVerticalOffset);

            String pageNumberTitle = bundle.getPageNumberFormat().getPageNumberTitle();
            int pageNumberVerticalOffset = indexVerticalOffset + 30;
            addText(document, getPage(), pageNumberTitle, 480, pageNumberVerticalOffset, PDType1Font.HELVETICA,12);

            numLinesAdded += (pageNumberVerticalOffset - TOP_MARGIN_OFFSET) / 20;
            numLinesAdded += 2;
        }

        private void addDocument(String documentTitle, int pageNumber, int noOfPages) throws IOException {

            float yyOffset = getVerticalOffset();

            // add an extra space after a folder so the document doesn't look like it's in the folder
            if (endOfFolder) {
                addText(document, getPage(), " ", 50, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
                yyOffset += LINE_HEIGHT;
                numLinesAdded += 1;
            }

            final PDPage destination = document.getPage(pageNumber);

            addLink(document, getPage(), destination, documentTitle, yyOffset, PDType1Font.HELVETICA, 12);
            //Need to check the documentTitle width for the noOfLines calculations.
            final float stringWidth = PDFUtility.getStringWidth(PDFUtility.sanitizeText(documentTitle),
                PDType1Font.HELVETICA, 12);

            String pageNo = bundle.getPageNumberFormat().getPageNumber(pageNumber, noOfPages);

            addText(document, getPage(), pageNo, 480, yyOffset - 3, PDType1Font.HELVETICA, 12);
            int noOfLines = 1;
            if (stringWidth > 550) {
                noOfLines = splitString(documentTitle, CHARS_PER_TITLE_LINE).length;
            }
            numLinesAdded += noOfLines;
            endOfFolder = false;
        }

        private void addDocumentWithOutline(String documentTitle, int pageNumber, PDOutlineItem sibling) throws IOException {
            int noOfLines = splitString(sibling.getTitle(), CHARS_PER_TITLE_LINE).length;
            float yyOffset = getVerticalOffset();
            PDPage destination = new PDPage();
            // add an extra space after a folder so the document doesn't look like it's in the folder
            if (endOfFolder) {
                addText(document, getPage(), " ", 50, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
                yyOffset += LINE_HEIGHT;
                numLinesAdded += 1;
            }

            try {
                if (Objects.nonNull(sibling)) {
                    if (sibling.getDestination() instanceof PDPageDestination) {
                        PDPageDestination pd = (PDPageDestination) sibling.getDestination();
                        destination = document.getPage(pd.retrievePageNumber() + pageNumber);
                    }

                    if (!sibling.getTitle().equalsIgnoreCase(documentTitle)) {
                        addSubtitleLink(document, getPage(), destination, sibling.getTitle(), yyOffset, PDType1Font.HELVETICA);
                        numLinesAdded += noOfLines;
                    }
                }
            } catch (Exception e) {
                logToc.error("error processing subtitles:",e);
            }
            endOfFolder = false;
        }

        private void addFolder(String title, int pageNumber) throws IOException {
            final PDPage destination = document.getPage(pageNumber);
            float yyOffset = getVerticalOffset();

            addText(document, getPage(), " ", 50, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
            yyOffset += LINE_HEIGHT;
            addLink(document, getPage(), destination, title, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
            int noOfLines = splitString(title, CHARS_PER_TITLE_LINE).length;
            yyOffset += (LINE_HEIGHT * noOfLines);
            addText(document, getPage(), " ", 50, yyOffset, PDType1Font.HELVETICA_BOLD, 13);
            //Multiple by 3. As in the above lines. For each folder added. we add an empty line before and after the
            // folder text in the TOC.
            numLinesAdded += (noOfLines + 2);
            endOfFolder = false;
        }

        private float getVerticalOffset() {
            return TOP_MARGIN_OFFSET + ((numLinesAdded % NUM_LINES_PER_PAGE) * LINE_HEIGHT);
        }

        private PDPage getPage() {
            int pageIndex = (int) Math.floor((double) numLinesAdded / NUM_LINES_PER_PAGE);

            return pages.get(Math.min(pageIndex, pages.size() - 1));
        }

        private int getNumberPages() {
            int numberOfLinesForAllTitles = getNumberOfLinesForAllTitles();
            int numFolders = (int) bundle.getNestedFolders().count();
            int numSubtitle = bundle.getSubtitles(bundle, documents);
            int foldersStartLine = max(splitString(bundle.getDescription(), CHARS_PER_LINE).length, 2) + 2;
            // Multiply by 3. For each folder added. we add an empty line before and after the
            // folder text in the TOC.
            int numberTocLines = foldersStartLine + (CollectionUtils.isNotEmpty(bundle.getFolders())
                    ? numberOfLinesForAllTitles + (numFolders * 4) + numSubtitle
                    : numberOfLinesForAllTitles + numSubtitle);
            int numPages = (int) Math.ceil((double) numberTocLines / TableOfContents.NUM_LINES_PER_PAGE);
            logToc.info("numberOfLinesForAllTitles:{}", numberOfLinesForAllTitles);
            logToc.info("numFolders={}", numFolders);
            logToc.info("numSubtitle{}" + numSubtitle);
            logToc.info("numPages={}", numPages);
            return max(1, numPages);
        }

        private int getNumberOfLinesForAllTitles() {
            return bundle.getSortedDocuments()
                    .map(d -> splitString(d.getDocTitle(), CHARS_PER_TITLE_LINE).length)
                    .mapToInt(Integer::intValue).sum();
        }

        private void setEndOfFolder(boolean value) {
            endOfFolder = value;
        }
    }
}
