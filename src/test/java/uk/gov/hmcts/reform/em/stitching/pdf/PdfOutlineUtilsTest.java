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
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.isPrivate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
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
        when(item1Mock.getTitle()).thenReturn("First Title");
        when(item1Mock.getNextSibling()).thenReturn(item2Mock);
        when(item2Mock.getTitle()).thenReturn("Second Title");
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
    void returnsEmptyListOnSizeMismatch() {
        when(containerMock.getSortedDocuments()).thenAnswer(inv -> Stream.of(documentMock));
        Map<BundleDocument, File> fileMap = Map.of();

        List<String> titles = PdfOutlineUtils.getSubtitles(containerMock, fileMap);

        assertTrue(titles.isEmpty());
        loaderMockedStatic.verifyNoInteractions();
    }

    @Test
    void returnsZeroOnIoException() {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenThrow(new IOException("File not found"));

        Integer count = PdfOutlineUtils.getNumberOfSubtitles(containerMock, fileMap);

        assertEquals(0, count);
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
    void handlesIoExceptionWhenClosingFile() throws IOException {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenReturn(pdDocumentMock);
        when(pdDocumentMock.getDocumentCatalog()).thenReturn(pdDocumentCatalogMock);
        when(pdDocumentCatalogMock.getDocumentOutline()).thenReturn(outlineMock);
        when(outlineMock.getFirstChild()).thenReturn(item1Mock);
        when(item1Mock.getTitle()).thenReturn("Valid Title");

        doThrow(new IOException("Close Mock Exception")).when(pdDocumentMock).close();

        Integer count = PdfOutlineUtils.getNumberOfSubtitles(containerMock, fileMap);
        assertEquals(1, count);

        List<String> titles = PdfOutlineUtils.getSubtitles(containerMock, fileMap);
        assertEquals(1, titles.size());

        verify(pdDocumentMock, times(2)).close();
    }

    @Test
    void returnsZeroOnNullOutline() throws IOException {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenReturn(pdDocumentMock);
        when(pdDocumentMock.getDocumentCatalog()).thenReturn(pdDocumentCatalogMock);
        when(pdDocumentCatalogMock.getDocumentOutline()).thenReturn(null);

        Integer count = PdfOutlineUtils.getNumberOfSubtitles(containerMock, fileMap);

        assertEquals(0, count);
        verify(pdDocumentMock).close();
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

    @Test
    void ignoresOutlineWithNullFirstChild() throws IOException {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenReturn(pdDocumentMock);
        when(pdDocumentMock.getDocumentCatalog()).thenReturn(pdDocumentCatalogMock);
        when(pdDocumentCatalogMock.getDocumentOutline()).thenReturn(outlineMock);

        when(outlineMock.getFirstChild()).thenReturn(null);

        Integer count = PdfOutlineUtils.getNumberOfSubtitles(containerMock, fileMap);
        assertEquals(0, count);

        List<String> titles = PdfOutlineUtils.getSubtitles(containerMock, fileMap);
        assertTrue(titles.isEmpty());

        verify(pdDocumentMock, times(2)).close();
    }

    @Test
    void testCircularReferenceSafelyExits() throws IOException {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenReturn(pdDocumentMock);
        when(pdDocumentMock.getDocumentCatalog()).thenReturn(pdDocumentCatalogMock);
        when(pdDocumentCatalogMock.getDocumentOutline()).thenReturn(outlineMock);

        when(outlineMock.getFirstChild()).thenReturn(item1Mock);
        when(item1Mock.getTitle()).thenReturn("Item 1");
        when(item1Mock.getNextSibling()).thenReturn(item2Mock);

        when(item2Mock.getTitle()).thenReturn("Item 2");
        when(item2Mock.getNextSibling()).thenReturn(item1Mock); // circular reference back to item1

        List<String> titles = PdfOutlineUtils.getSubtitles(containerMock, fileMap);
        assertEquals(2, titles.size(), "Should extract exact items before circular termination");
        assertTrue(titles.contains("Item 1"));
        assertTrue(titles.contains("Item 2"));

        int count = PdfOutlineUtils.getNumberOfSubtitles(containerMock, fileMap);
        assertEquals(2, count);

        verify(pdDocumentMock, times(2)).close();
    }

    @Test
    void testExtremeDepthLimit() throws IOException {
        setupMockStreams();

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenReturn(pdDocumentMock);
        when(pdDocumentMock.getDocumentCatalog()).thenReturn(pdDocumentCatalogMock);
        when(pdDocumentCatalogMock.getDocumentOutline()).thenReturn(outlineMock);

        PDOutlineItem firstItem = mock(PDOutlineItem.class);
        when(outlineMock.getFirstChild()).thenReturn(firstItem);
        when(firstItem.getTitle()).thenReturn("Level 0");

        PDOutlineItem current = firstItem;
        for (int i = 1; i <= 20; i++) {
            PDOutlineItem child = mock(PDOutlineItem.class);
            lenient().when(child.getTitle()).thenReturn("Level " + i);
            lenient().when(current.getFirstChild()).thenReturn(child);
            current = child;
        }

        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);
        List<String> titles = PdfOutlineUtils.getSubtitles(containerMock, fileMap);
        assertEquals(11, titles.size());
        assertEquals("Level 0", titles.getFirst());
        assertEquals("Level 10", titles.get(10));

        int count = PdfOutlineUtils.getNumberOfSubtitles(containerMock, fileMap);
        assertEquals(11, count);

        verify(pdDocumentMock, times(2)).close();
    }

    @Test
    void testIgnoresNullTitles() throws IOException {
        setupMockStreams();
        Map<BundleDocument, File> fileMap = Map.of(documentMock, fileMock);

        loaderMockedStatic.when(() -> Loader.loadPDF(fileMock)).thenReturn(pdDocumentMock);
        when(pdDocumentMock.getDocumentCatalog()).thenReturn(pdDocumentCatalogMock);
        when(pdDocumentCatalogMock.getDocumentOutline()).thenReturn(outlineMock);

        PDOutlineItem item3Mock = mock(PDOutlineItem.class);

        when(outlineMock.getFirstChild()).thenReturn(item1Mock);
        when(item1Mock.getTitle()).thenReturn("Valid Title 1");
        when(item1Mock.getNextSibling()).thenReturn(item2Mock);

        when(item2Mock.getTitle()).thenReturn(null); // Structural metadata node
        when(item2Mock.getNextSibling()).thenReturn(item3Mock);

        when(item3Mock.getTitle()).thenReturn("Valid Title 2");
        when(item3Mock.getNextSibling()).thenReturn(null);

        List<String> titles = PdfOutlineUtils.getSubtitles(containerMock, fileMap);
        assertEquals(2, titles.size());
        assertEquals("Valid Title 1", titles.get(0));
        assertEquals("Valid Title 2", titles.get(1));

        int count = PdfOutlineUtils.getNumberOfSubtitles(containerMock, fileMap);
        assertEquals(2, count);

        verify(pdDocumentMock, times(2)).close();
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<PdfOutlineUtils> constructor = PdfOutlineUtils.class.getDeclaredConstructor();

        assertTrue(isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);
        constructor.newInstance();
    }

    private void setupMockStreams() {
        when(containerMock.getSortedDocuments()).thenAnswer(inv -> Stream.of(documentMock));
        when(containerMock.getSortedItems()).thenAnswer(inv -> Stream.of(containerMock));
    }
}