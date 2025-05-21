package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import static org.springframework.util.ObjectUtils.isEmpty;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMerger.INDEX_PAGE;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.LINE_HEIGHT;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addCenterText;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addLink;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addSubtitleLink;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addText;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.splitString;

public class TableOfContents {

    protected static final int NUM_LINES_PER_PAGE = 38;
    private final List<PDPage> pages = new ArrayList<>();
    private final PDDocument document;
    private final Bundle bundle;
    private final Map<BundleDocument, File> documents;
    private static final float TOP_MARGIN_OFFSET = 40f;
    public static final int SPACE_PER_LINE = 500;
    public static final int SPACE_PER_TITLE_LINE = 400; //Also used for folders. May need third variable in the future.
    public static final int SPACE_PER_SUBTITLE_LINE = 350;
    protected int numLinesAdded = 0;
    protected boolean endOfFolder = false;
    private final Logger logger = LoggerFactory.getLogger(TableOfContents.class);
    protected static final int TITLE_XX_OFFSET = 50;

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
            PDFText pdfText = new PDFText(bundle.getDescription(),
                50, 80, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            addText(document, getPage(), pdfText, SPACE_PER_LINE);
        }

        int descriptionLines = splitString(bundle.getDescription(), SPACE_PER_LINE,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12).length;
        int indexVerticalOffset = max(descriptionLines * 20 + 70, 90);
        addCenterText(document, getPage(), INDEX_PAGE, indexVerticalOffset);

        String pageNumberTitle = bundle.getPageNumberFormat().getPageNumberTitle();
        int pageNumberVerticalOffset = indexVerticalOffset + 30;
        PDFText pdfText = new PDFText(pageNumberTitle,
            480, pageNumberVerticalOffset, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        addText(document, getPage(), pdfText);

        numLinesAdded += (pageNumberVerticalOffset - TOP_MARGIN_OFFSET) / 20;
        numLinesAdded += 2;
    }

    public void addDocument(String documentTitle, int pageNumber, int noOfPages) throws IOException {

        float yyOffset = getVerticalOffset();

        // add an extra space after a folder so the document doesn't look like it's in the folder
        if (endOfFolder) {
            PDFText pdfText = new PDFText(" ", TITLE_XX_OFFSET, yyOffset,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
            addText(document, getPage(), pdfText);
            yyOffset += LINE_HEIGHT;
            numLinesAdded += 1;
        }

        final PDPage destination = document.getPage(pageNumber);

        int noOfLines = splitString(documentTitle, SPACE_PER_TITLE_LINE,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12).length;
        PDFLink pdfLink = new PDFLink(documentTitle,
            TITLE_XX_OFFSET, yyOffset, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12, destination);
        addLink(document, getPage(), pdfLink, noOfLines);

        String pageNo = bundle.getPageNumberFormat().getPageNumber(pageNumber, noOfPages);

        addText(document, getPage(),
            new PDFText(pageNo, 480, yyOffset - 3,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12));
        numLinesAdded += noOfLines;
        endOfFolder = false;
    }

    public void addDocumentWithOutline(String documentTitle, int pageNumber, PDOutlineItem sibling) throws IOException {
        float yyOffset = getVerticalOffset();
        // add an extra space after a folder so the document doesn't look like it's in the folder
        if (endOfFolder) {
            PDFText pdfText = new PDFText(" ", TITLE_XX_OFFSET, yyOffset,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
            addText(document, getPage(), pdfText);
            yyOffset += LINE_HEIGHT;
            numLinesAdded += 1;
        }
        if (Objects.nonNull(sibling)) {
            int noOfLines = splitString(sibling.getTitle(), SPACE_PER_SUBTITLE_LINE,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12).length;
            PDPage destination = new PDPage();
            try {
                if (sibling.getDestination() instanceof PDPageDestination pdPageDestination) {
                    destination = document.getPage(pdPageDestination.retrievePageNumber() + pageNumber);
                }
                if (documentTitle != null && !documentTitle.equalsIgnoreCase(sibling.getTitle())) {
                    addSubtitleLink(
                        document,
                        getPage(),
                        destination,
                        sibling.getTitle(),
                        yyOffset,
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA));
                    numLinesAdded += noOfLines;
                }
            } catch (Exception e) {
                logger.error("Error processing subtitles: {}", documentTitle, e);
            }
        }
        endOfFolder = false;
    }

    public void addFolder(String title, int pageNumber) throws IOException {
        final PDPage destination = document.getPage(pageNumber);
        float yyOffset = getVerticalOffset();

        PDType1Font folderFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        int folderFontSize = 13;

        addText(document, getPage(),
            new PDFText(" ", TITLE_XX_OFFSET, yyOffset, folderFont, folderFontSize));
        yyOffset += LINE_HEIGHT;
        int noOfLines = splitString(title, SPACE_PER_TITLE_LINE, folderFont, folderFontSize).length;
        PDFLink pdfLink = new PDFLink(title,
            TITLE_XX_OFFSET, yyOffset, folderFont, folderFontSize, destination);
        addLink(document, getPage(), pdfLink, noOfLines);
        yyOffset += (LINE_HEIGHT * noOfLines);
        addText(document, getPage(),
            new PDFText(" ", TITLE_XX_OFFSET, yyOffset, folderFont, folderFontSize));
        // For each folder added. we add an empty line before and after the folder text in the TOC.
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
        int numFolders = !bundle.hasFolderCoversheets() ? 0 : (int) bundle.getNestedFolders().count();
        int numLinesSubtitles = getNumberOfLinesForAllSubtitles();
        int foldersStartLine =
            max(splitString(bundle.getDescription(), SPACE_PER_LINE,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12).length, 2) + 2;
        // Multiply by 3. For each folder added. we add an empty line before and after the
        // folder text in the TOC.
        int numberTocLines = foldersStartLine + (CollectionUtils.isNotEmpty(bundle.getFolders())
            ? numberOfLinesForAllTitles + (numFolders * 3) + (numLinesSubtitles)
            : numberOfLinesForAllTitles + numLinesSubtitles);
        int numPages = (int) Math.ceil((double) numberTocLines / TableOfContents.NUM_LINES_PER_PAGE);
        logger.info("numberOfLinesForAllTitles: {}, numFolders: {}, numSubtitle:{},numberTocLines: {}, numPages:{} ",
            numberOfLinesForAllTitles,
            numFolders,
            numLinesSubtitles,
            numberTocLines,
            numPages
        );
        return max(1, numPages);
    }

    private int getNumberOfLinesForAllSubtitles() {
        List<String> subtitles = bundle.getSubtitles(bundle, documents);
        return subtitles
            .stream()
            .mapToInt(subtitle -> splitString(subtitle, SPACE_PER_SUBTITLE_LINE,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12).length)
            .sum();
    }

    private int getNumberOfLinesForAllTitles() {
        return bundle.getSortedDocuments()
            .map(d -> splitString(d.getDocTitle(), SPACE_PER_TITLE_LINE,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12).length)
            .mapToInt(Integer::intValue).sum();
    }

    public void setEndOfFolder(boolean value) {
        endOfFolder = value;
    }
}
