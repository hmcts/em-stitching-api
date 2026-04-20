package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfOutlineUtilsTest {

    @Mock
    private SortableBundleItem containerMock;
    @Mock
    private BundleDocument documentMock;
    @Mock
    private File fileMock;
    @Mock
    private PDDocument pdDocumentMock;
    @Mock
    private PDDocumentCatalog pdDocumentCatalogMock;
    @Mock
    private PDDocumentOutline outlineMock;
    @Mock
    private PDOutlineItem item1Mock;
    @Mock
    private PDOutlineItem item2Mock;

    private MockedStatic<Loader> loaderMockedStatic;

    @BeforeEach
    void setUp() {
        loaderMockedStatic = mockStatic(Loader.class);
    }

    @AfterEach
    void tearDown() {
        loaderMockedStatic.close();
    }

    @Test
    void returnsCountForValidOutline() throws IOException {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenReturn(pdDocumentMock);
        when(pdDocumentMock.getDocumentCatalog()).thenReturn(pdDocumentCatalogMock);
        when(pdDocumentCatalogMock.getDocumentOutline()).thenReturn(outlineMock);

        when(outlineMock.getFirstChild()).thenReturn(item1Mock);
        when(item1Mock.getNextSibling()).thenReturn(item2Mock);
        when(item2Mock.getNextSibling()).thenReturn(null);

        Integer count = PdfOutlineUtils.getNumberOfSubtitles(containerMock, fileMap);

        assertEquals(2, count);
        verify(pdDocumentMock).close(); 
    }

    @Test
    void returnsListForValidOutline() throws IOException {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenReturn(pdDocumentMock);
        when(pdDocumentMock.getDocumentCatalog()).thenReturn(pdDocumentCatalogMock);
        when(pdDocumentCatalogMock.getDocumentOutline()).thenReturn(outlineMock);

        when(outlineMock.getFirstChild()).thenReturn(item1Mock);
        when(item1Mock.getTitle()).thenReturn("First Title");
        when(item1Mock.getNextSibling()).thenReturn(item2Mock);
        
        when(item2Mock.getTitle()).thenReturn("Second Title");
        when(item2Mock.getNextSibling()).thenReturn(null);

        List<String> titles = PdfOutlineUtils.getSubtitles(containerMock, fileMap);

        assertEquals(2, titles.size());
        assertEquals("First Title", titles.get(0));
        assertEquals("Second Title", titles.get(1));
        verify(pdDocumentMock).close();
    }

    @Test
    void returnsZeroOnSizeMismatch() {
        when(containerMock.getSortedDocuments()).thenAnswer(inv -> Stream.of(documentMock));
        Map<BundleDocument, File> fileMap = Map.of(); // Empty map

        Integer count = PdfOutlineUtils.getNumberOfSubtitles(containerMock, fileMap);

        assertEquals(0, count);
        loaderMockedStatic.verifyNoInteractions();
    }

    @Test
    void returnsEmptyOnIoException() {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenThrow(new IOException("File not found"));

        List<String> titles = PdfOutlineUtils.getSubtitles(containerMock, fileMap);

        assertTrue(titles.isEmpty());
    }

    @Test
    void returnsEmptyOnNullOutline() throws IOException {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenReturn(pdDocumentMock);
        when(pdDocumentMock.getDocumentCatalog()).thenReturn(pdDocumentCatalogMock);
        when(pdDocumentCatalogMock.getDocumentOutline()).thenReturn(null);

        List<String> titles = PdfOutlineUtils.getSubtitles(containerMock, fileMap);

        assertTrue(titles.isEmpty());
        verify(pdDocumentMock).close();
    }
    
    private void setupMockStreams() {
        when(containerMock.getSortedDocuments()).thenAnswer(inv -> Stream.of(documentMock));
        when(containerMock.getSortedItems()).thenAnswer(inv -> Stream.of(containerMock));
    }
}