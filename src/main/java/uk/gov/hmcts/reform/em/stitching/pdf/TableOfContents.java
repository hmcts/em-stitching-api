package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.Math.max;
import static org.springframework.util.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMerger.INDEX_PAGE;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.LINE_HEIGHT;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addCenterText;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addLink;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addSubtitleLink;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addText;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.splitString;

public class TableOfContents {

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

    public TableOfContents(PDDocument document, Bundle bundle, Map<BundleDocument, File> documents) throws IOException {
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
            addText(document, getPage(), bundle.getDescription(), 50, 80, PDType1Font.HELVETICA, 12, 80);
        }

        int descriptionLines = splitString(bundle.getDescription(), CHARS_PER_LINE).length;
        int indexVerticalOffset = max(descriptionLines * 20 + 70, 90);
        addCenterText(document, getPage(), INDEX_PAGE, indexVerticalOffset);

        String pageNumberTitle = bundle.getPageNumberFormat().getPageNumberTitle();
        int pageNumberVerticalOffset = indexVerticalOffset + 30;
        addText(document, getPage(), pageNumberTitle, 480, pageNumberVerticalOffset, PDType1Font.HELVETICA, 12);

        numLinesAdded += (pageNumberVerticalOffset - TOP_MARGIN_OFFSET) / 20;
        numLinesAdded += 2;
    }

    public void addDocument(String documentTitle, int pageNumber, int noOfPages) throws IOException {

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

    public void addDocumentWithOutline(String documentTitle, int pageNumber, PDOutlineItem sibling) throws IOException {
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
            logToc.error("Error processing subtitles: {}", documentTitle, e);
        }
        endOfFolder = false;
    }

    public void addFolder(String title, int pageNumber) throws IOException {
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

    public PDPage getPage() {
        int pageIndex = (int) Math.floor((double) numLinesAdded / NUM_LINES_PER_PAGE);

        return pages.get(Math.min(pageIndex, pages.size() - 1));
    }

    public int getNumberPages() {
        int numberOfLinesForAllTitles = getNumberOfLinesForAllTitles();
        int numFolders = (int) bundle.getNestedFolders().count();
        int numSubtitle = bundle.getSubtitles(bundle, documents);
        int foldersStartLine = max(splitString(bundle.getDescription(), CHARS_PER_LINE).length, 2) + 2;
        // Multiply by 3. For each folder added. we add an empty line before and after the
        // folder text in the TOC.
        int numberTocLines = foldersStartLine + (CollectionUtils.isNotEmpty(bundle.getFolders())
                ? numberOfLinesForAllTitles + (numFolders * 3) + numSubtitle
                : numberOfLinesForAllTitles + numSubtitle);
        int numPages = (int) Math.ceil((double) numberTocLines / TableOfContents.NUM_LINES_PER_PAGE);
        logToc.info("numberOfLinesForAllTitles:{}", numberOfLinesForAllTitles);
        logToc.info("numFolders={}", numFolders);
        logToc.info("numSubtitle{}" + numSubtitle);
        logToc.info("numberTocLines{}" + numberTocLines);
        logToc.info("numPages={}", numPages);
        return max(1, numPages);
    }

    private int getNumberOfLinesForAllTitles() {
        return bundle.getSortedDocuments()
                .map(d -> splitString(d.getDocTitle(), CHARS_PER_TITLE_LINE).length)
                .mapToInt(Integer::intValue).sum();
    }

    public void setEndOfFolder(boolean value) {
        endOfFolder = value;
    }
}
