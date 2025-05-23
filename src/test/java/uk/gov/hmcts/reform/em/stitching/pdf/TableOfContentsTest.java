package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PageNumberFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class TableOfContentsTest {

    @Mock
    private Bundle mockBundle;

    private PDDocument document;
    private Map<BundleDocument, File> documentsMap;

    private MockedStatic<PDFUtility> mockedPdfUtility;

    private static final float TITLE_XX_OFFSET_VALUE = 50f;
    private static final int NUM_LINES_PER_PAGE_CONST = 38;

    @BeforeEach
    void setUp() {
        document = new PDDocument();
        documentsMap = new HashMap<>();

        mockedPdfUtility = mockStatic(PDFUtility.class);

        mockedPdfUtility.when(() -> PDFUtility.splitString(anyString(), anyInt(), any(PDType1Font.class), anyFloat()))
            .thenReturn(new String[]{"default single line"});

        mockedPdfUtility.when(() -> PDFUtility.splitString(eq(null), anyInt(), any(PDType1Font.class), anyFloat()))
            .thenReturn(new String[]{});

        mockedPdfUtility.when(() -> PDFUtility.addText(any(PDDocument.class), any(PDPage.class), any(PDFText.class),
            anyInt())).thenAnswer(invocation -> null);
        mockedPdfUtility.when(() -> PDFUtility.addText(any(PDDocument.class), any(PDPage.class), any(PDFText.class)))
            .thenAnswer(invocation -> null);
        mockedPdfUtility.when(() -> PDFUtility.addLink(any(PDDocument.class), any(PDPage.class), any(PDFLink.class),
            anyInt())).thenAnswer(invocation -> null);
        mockedPdfUtility.when(() -> PDFUtility.addCenterText(any(PDDocument.class), any(PDPage.class), anyString(),
            anyInt())).thenAnswer(invocation -> null);
        mockedPdfUtility.when(() -> PDFUtility.addSubtitleLink(any(), any(), any(), anyString(), anyFloat(), any()))
            .thenAnswer(invocation -> null);

        when(mockBundle.getDescription()).thenReturn("Default Bundle Description");
        when(mockBundle.getPageNumberFormat()).thenReturn(PageNumberFormat.PAGE_RANGE);
        when(mockBundle.getSortedDocuments()).thenAnswer(invocation -> Stream.empty());
        when(mockBundle.getNestedFolders()).thenAnswer(invocation -> Stream.empty());
        when(mockBundle.hasFolderCoversheets()).thenReturn(true);
        lenient().when(mockBundle.getSubtitles(any(Bundle.class), any(Map.class))).thenReturn(Collections.emptyList());

        mockSpecificSplitString("Default Bundle Description",
            TableOfContents.SPACE_PER_LINE, 12f, new String[]{"Default Bundle Description"});
    }

    @AfterEach
    void tearDown() throws IOException {
        mockedPdfUtility.close();
        if (document != null) {
            document.close();
        }
    }

    private void mockSpecificSplitString(String textToMatch, int spacePerLine, float fontSize, String[] resultLines) {
        if (textToMatch == null) {
            mockedPdfUtility.when(() -> PDFUtility.splitString(
                    eq(null), eq(spacePerLine), any(PDType1Font.class), eq(fontSize)))
                .thenReturn(resultLines);
            return;
        }
        mockedPdfUtility.when(() -> PDFUtility.splitString(
                eq(textToMatch), eq(spacePerLine), any(PDType1Font.class), eq(fontSize)))
            .thenReturn(resultLines);
    }


    private void setupBundleForLineCounting(String description, List<BundleDocument> docs, List<String> subtitles) {
        when(mockBundle.getDescription()).thenReturn(description);
        mockSpecificSplitString(description, TableOfContents.SPACE_PER_LINE, 12f,
            description == null ? new String[]{} : description.split("\n"));

        when(mockBundle.getSortedDocuments()).thenAnswer(invocation -> docs.stream());
        for (BundleDocument doc : docs) {
            String docTitle = doc.getDocTitle();
            mockSpecificSplitString(docTitle, TableOfContents.SPACE_PER_TITLE_LINE, 12f,
                docTitle == null ? new String[]{} : docTitle.split("\n"));
        }

        when(mockBundle.getSubtitles(mockBundle, documentsMap)).thenReturn(subtitles);
        for (String subtitle : subtitles) {
            mockSpecificSplitString(subtitle, TableOfContents.SPACE_PER_SUBTITLE_LINE, 12f,
                subtitle == null ? new String[]{} : subtitle.split("\n"));
        }

        Stream<Bundle> folderStream = Stream.empty();
        when(mockBundle.getNestedFolders()).thenAnswer(invocation -> folderStream);
    }


    @Test
    void constructorAddsInitialTextAndPages() throws IOException {
        String description = "Test Bundle Description";
        setupBundleForLineCounting(description, Collections.emptyList(), Collections.emptyList());

        final TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);

        assertEquals(1, document.getNumberOfPages(),
            "PDDocument should have 1 page added by TOC constructor based on its getNumberPages call.");

        PDPage expectedTocPage = document.getPage(0);

        mockedPdfUtility.verify(() -> PDFUtility.addText(eq(document), eq(expectedTocPage),
            argThat(pdfText -> description.equals(pdfText.getText())), eq(TableOfContents.SPACE_PER_LINE)));
        mockedPdfUtility.verify(() -> PDFUtility.addCenterText(eq(document), eq(expectedTocPage),
            eq(PDFMerger.INDEX_PAGE), anyInt()));
        mockedPdfUtility.verify(() -> PDFUtility.addText(eq(document), eq(expectedTocPage),
            argThat(pdfText -> PageNumberFormat.PAGE_RANGE.getPageNumberTitle().equals(pdfText.getText()))));

        assertNotNull(toc.getPage(), "toc.getPage() should return a valid page after construction.");
        assertEquals(expectedTocPage, toc.getPage(), "toc.getPage() should return the first TOC page.");
    }

    @Test
    void addDocumentWritesTitleAndPageNumber() throws IOException {
        setupBundleForLineCounting("Desc", Collections.emptyList(), Collections.emptyList());
        final TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);

        PDPage destinationPage = new PDPage();
        document.addPage(destinationPage);
        int destinationPageNumInMainDoc = document.getNumberOfPages() - 1;

        String docTitle = "Test Document";
        mockSpecificSplitString(docTitle, TableOfContents.SPACE_PER_TITLE_LINE, 12f, new String[]{docTitle});
        String docTitle2 = "Test Document 2";
        mockSpecificSplitString(docTitle2, TableOfContents.SPACE_PER_TITLE_LINE, 12f, new String[]{docTitle2});

        toc.addDocument(docTitle, destinationPageNumInMainDoc, 5);

        mockedPdfUtility.verify(() -> PDFUtility.addLink(eq(document), eq(toc.getPage()),
            argThat(link -> docTitle.equals(link.getText()) && link.getXxOffset() == TITLE_XX_OFFSET_VALUE),
            eq(1)
        ));
        mockedPdfUtility.verify(() -> PDFUtility.addText(eq(document), eq(toc.getPage()),
            argThat(pdfText -> mockBundle.getPageNumberFormat().getPageNumber(destinationPageNumInMainDoc, 5)
                .equals(pdfText.getText()) && pdfText.getXxOffset() == 480f)
        ));
        mockedPdfUtility.verify(() -> PDFUtility.addText(any(PDDocument.class),
            any(PDPage.class), argThat(isEndOfFolderSpaceLine())), never());

    }

    @Test
    void addDocumentWhenEndOfFolderAddsExtraSpace() throws IOException {
        setupBundleForLineCounting("Desc", Collections.emptyList(), Collections.emptyList());
        TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);
        PDPage destinationPage = new PDPage();
        document.addPage(destinationPage);
        int destinationPageNumInMainDoc = document.getNumberOfPages() - 1;

        String docTitle = "Another Document";
        mockSpecificSplitString(docTitle, TableOfContents.SPACE_PER_TITLE_LINE, 12f, new String[]{docTitle});

        toc.setEndOfFolder(true);
        toc.addDocument(docTitle, destinationPageNumInMainDoc, 3);

        mockedPdfUtility.verify(() -> PDFUtility.addText(eq(document), eq(toc.getPage()),
            argThat(isEndOfFolderSpaceLine())
        ), times(1));
        mockedPdfUtility.verify(() -> PDFUtility.addLink(eq(document), eq(toc.getPage()),
            argThat(link -> docTitle.equals(link.getText())),
            eq(1)
        ));

        String docTitle2 = "DocAfterReset";
        mockSpecificSplitString(docTitle2, TableOfContents.SPACE_PER_TITLE_LINE, 12f, new String[]{docTitle2});
        toc.addDocument(docTitle2, destinationPageNumInMainDoc, 2);

        mockedPdfUtility.verify(() -> PDFUtility.addText(any(PDDocument.class), any(PDPage.class),
            argThat(isEndOfFolderSpaceLine())), times(1));
    }

    @Test
    void addFolderWritesFolderTitle() throws IOException {
        setupBundleForLineCounting("Desc", Collections.emptyList(), Collections.emptyList());
        TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);
        PDPage destinationPage = new PDPage();
        document.addPage(destinationPage);
        int destinationPageNumInMainDoc = document.getNumberOfPages() - 1;

        String folderTitle = "My Folder";
        mockSpecificSplitString(folderTitle,
            TableOfContents.SPACE_PER_TITLE_LINE, 13f, new String[]{folderTitle});

        toc.setEndOfFolder(true);
        toc.addFolder(folderTitle, destinationPageNumInMainDoc);

        mockedPdfUtility.verify(() -> PDFUtility.addLink(eq(document), eq(toc.getPage()),
            argThat(link -> folderTitle.equals(link.getText()) && link.getFontSize() == 13),
            eq(1)
        ));

        mockedPdfUtility.verify(() -> PDFUtility.addText(any(PDDocument.class), any(PDPage.class),
            argThat(isEndOfFolderSpaceLine())), times(2));

        String docAfterFolder = "DocAfterFolder";
        mockSpecificSplitString(docAfterFolder, TableOfContents.SPACE_PER_TITLE_LINE,
            12f, new String[]{docAfterFolder});
        toc.addDocument(docAfterFolder, destinationPageNumInMainDoc, 1);

        mockedPdfUtility.verify(() -> PDFUtility.addText(any(PDDocument.class), any(PDPage.class),
            argThat(isEndOfFolderSpaceLine())), times(2));
    }

    @Test
    void addDocumentWithOutlineWhenPageRetrievalFails() throws IOException {
        setupBundleForLineCounting("Desc", Collections.emptyList(), Collections.emptyList());
        final TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);

        PDOutlineItem mockSibling = mock(PDOutlineItem.class);
        PDPageDestination mockPageDest = mock(PDPageDestination.class);

        when(mockSibling.getTitle()).thenReturn("Sibling Title");
        when(mockSibling.getDestination()).thenReturn(mockPageDest);
        when(mockPageDest.retrievePageNumber()).thenThrow(new RuntimeException("Simulated page retrieval failure"));

        toc.addDocumentWithOutline("Main Doc Title", 10, mockSibling);

        mockedPdfUtility.verify(() -> PDFUtility
            .addSubtitleLink(any(), any(), any(), anyString(), anyFloat(), any()), never());
    }

    @Test
    void addDocumentWithOutlineWithValidSibling() throws IOException {
        setupBundleForLineCounting("Desc", Collections.emptyList(), Collections.emptyList());
        final TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);

        PDOutlineItem mockSibling = mock(PDOutlineItem.class);
        PDPageDestination mockPageDest = mock(PDPageDestination.class);
        final int mainDocPageForSiblingLink = 5;

        String siblingTitle = "Sibling Title";
        when(mockSibling.getTitle()).thenReturn(siblingTitle);
        when(mockSibling.getDestination()).thenReturn(mockPageDest);
        int siblingOriginalPageNum = 2;
        when(mockPageDest.retrievePageNumber()).thenReturn(siblingOriginalPageNum);

        mockSpecificSplitString(siblingTitle,
            TableOfContents.SPACE_PER_SUBTITLE_LINE, 12f, new String[]{siblingTitle});

        int requiredPageIndex = siblingOriginalPageNum + mainDocPageForSiblingLink;
        for (int i = document.getNumberOfPages(); i <= requiredPageIndex; i++) {
            document.addPage(new PDPage());
        }

        toc.addDocumentWithOutline("Main Document With Outline", mainDocPageForSiblingLink, mockSibling);

        mockedPdfUtility.verify(() -> PDFUtility.addSubtitleLink(
            eq(document),
            eq(toc.getPage()),
            eq(document.getPage(requiredPageIndex)),
            eq(siblingTitle),
            anyFloat(),
            any(PDType1Font.class)
        ));
    }

    @Test
    void addDocumentWhenDocumentTitleEqualsSiblingTitle() throws IOException {
        setupBundleForLineCounting("Desc", Collections.emptyList(), Collections.emptyList());
        final TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);

        PDOutlineItem mockSibling = mock(PDOutlineItem.class);
        PDPageDestination mockPageDest = mock(PDPageDestination.class);
        final int siblingDestPageIdx = 5;

        when(mockSibling.getTitle()).thenReturn("Same Title");
        when(mockSibling.getDestination()).thenReturn(mockPageDest);
        when(mockPageDest.retrievePageNumber()).thenReturn(2);

        toc.addDocumentWithOutline("Same Title", siblingDestPageIdx, mockSibling);

        mockedPdfUtility.verify(() ->
            PDFUtility.addSubtitleLink(any(), any(), any(), anyString(), anyFloat(), any()), never());
    }

    @Test
    void addDocumentWithOutlineWithNullSibling() throws IOException {
        setupBundleForLineCounting("Desc", Collections.emptyList(), Collections.emptyList());
        TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);
        toc.addDocumentWithOutline("Doc Title", 1, null);
        mockedPdfUtility.verify(() ->
            PDFUtility.addSubtitleLink(any(), any(), any(), anyString(), anyFloat(), any()), never());
    }


    @Test
    void getPageCyclesThroughPages() throws IOException {
        final int initialLinesInToc = 10;

        List<BundleDocument> docs = new ArrayList<>();
        for (int i = 0; i < NUM_LINES_PER_PAGE_CONST * 2; i++) {
            BundleDocument doc = mock(BundleDocument.class);
            String title = "Doc " + i;
            when(doc.getDocTitle()).thenReturn(title);
            mockSpecificSplitString(title, TableOfContents.SPACE_PER_TITLE_LINE, 12f, new String[]{title});
            docs.add(doc);
        }
        setupBundleForLineCounting("L1\nL2\nL3\nL4\nL5", docs, Collections.emptyList());

        TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);
        assertEquals(3, document.getNumberOfPages(), "PDDocument should have 3 pages for TOC.");

        final PDPage firstTocPage = document.getPage(0);
        final PDPage secondTocPage = document.getPage(1);
        final PDPage thirdTocPage = document.getPage(2);
        
        assertEquals(firstTocPage, toc.getPage(), "Initially, should be on the first TOC page.");

        for (int i = 0; i < (NUM_LINES_PER_PAGE_CONST - initialLinesInToc - 1); i++) {
            toc.addDocument("Filler Doc " + i, 1, 1);
        }
        assertEquals(firstTocPage, toc.getPage());

        toc.addDocument("Overflow Doc 1", 1, 1);
        assertEquals(secondTocPage, toc.getPage());

        for (int i = 0; i < (NUM_LINES_PER_PAGE_CONST - 1); i++) {
            toc.addDocument("Filler Doc Page 2 " + i, 1, 1);
        }
        assertEquals(secondTocPage, toc.getPage());

        toc.addDocument("Overflow Doc 2", 1, 1);
        assertEquals(thirdTocPage, toc.getPage());
    }

    @Test
    void addDocumentWithOutlineWhenEndOfFolder() throws IOException {
        setupBundleForLineCounting("Desc", Collections.emptyList(), Collections.emptyList());
        TableOfContents toc = new TableOfContents(document, mockBundle, documentsMap);

        toc.setEndOfFolder(true);

        String documentTitle = "Doc After Folder";
        int pageNumber = 5;
        PDOutlineItem mockSibling = null;

        mockSpecificSplitString(documentTitle,
            TableOfContents.SPACE_PER_TITLE_LINE, 12f, new String[]{documentTitle});

        toc.addDocumentWithOutline(documentTitle, pageNumber, mockSibling);

        mockedPdfUtility.verify(() -> PDFUtility.addText(
            eq(document),
            eq(toc.getPage()),
            argThat(isEndOfFolderSpaceLine())
        ), times(1));

        String docAfterReset = "DocAfterReset";
        mockSpecificSplitString(docAfterReset,
            TableOfContents.SPACE_PER_TITLE_LINE, 12f, new String[]{docAfterReset});
        int nextPageForLink = pageNumber + 1;
        while (document.getNumberOfPages() <= nextPageForLink) {
            document.addPage(new PDPage());
        }
        toc.addDocument(docAfterReset, nextPageForLink, 1);

        mockedPdfUtility.verify(() -> PDFUtility.addText(any(PDDocument.class), any(PDPage.class),
            argThat(isEndOfFolderSpaceLine())), times(1));
    }


    private ArgumentMatcher<PDFText> isEndOfFolderSpaceLine() {
        return pdfText -> " ".equals(pdfText.getText())
            && pdfText.getXxOffset() == TITLE_XX_OFFSET_VALUE
            && pdfText.getPdType1Font().getName().equals(Standard14Fonts.FontName.HELVETICA_BOLD.toString())
            && pdfText.getFontSize() == 13;
    }
}