package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithAdditionalDoc;

class PDFOutlineTest {

    private SortableBundleItem rootSortableItem;
    private TreeNode<SortableBundleItem> outlineTree;
    private PDDocument document;
    private PDFOutline pdfOutline;

    private static final File FILE_1 = new File(
        ClassLoader.getSystemResource("test-files/TEST_INPUT_FILE.pdf").getPath()
    );

    private static final File FILE_2 = new File(
        ClassLoader.getSystemResource("test-files/outlined.pdf").getPath()
    );

    private static final File FILE_3 = new File(
        ClassLoader.getSystemResource("test-files/outline_with_actions.pdf").getPath()
    );

    private static final File FILE_4 = new File(
        ClassLoader.getSystemResource("test-files/outline_with_named.pdf").getPath()
    );

    @BeforeEach
    void setUp() {
        document = new PDDocument();
        document.addPage(new PDPage());

        rootSortableItem = mock(SortableBundleItem.class);
        when(rootSortableItem.getId()).thenReturn(100L);
        when(rootSortableItem.getTitle()).thenReturn("Root Sortable Item");

        outlineTree = new TreeNode<>(rootSortableItem);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (document != null) {
            document.close();
        }
    }

    private SortableBundleItem createMockSortableItem(Long id, String title) {
        SortableBundleItem item = mock(SortableBundleItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getTitle()).thenReturn(title);
        return item;
    }

    @Test
    void createOutlineForDocument() throws IOException {
        pdfOutline = new PDFOutline(document, outlineTree);
        SortableBundleItem bundleItem = createMockSortableItem(1L, "Bundle");
        pdfOutline.addBundleItem(bundleItem);
        pdfOutline.setRootOutlineItemDest();

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();

        assertEquals("Bundle", documentOutline.getFirstChild().getTitle());
        assertNotNull(documentOutline.getFirstChild().getDestination());
        assertNotNull(documentOutline.getFirstChild());
    }

    @Test
    void createSubOutlinesForDocument() throws IOException {
        SortableBundleItem bundleItem = createMockSortableItem(2L, "Bundle");
        outlineTree = new TreeNode<>(bundleItem);
        document.addPage(new PDPage());
        pdfOutline = new PDFOutline(document, outlineTree);

        pdfOutline.addBundleItem(bundleItem);
        pdfOutline.addItem(0, "Folder Item 1");
        pdfOutline.addItem(1, "Folder Item 2");
        pdfOutline.setRootOutlineItemDest();

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem bundleOutline = documentOutline.getFirstChild();

        assertNotNull(bundleOutline);
        assertNotNull(bundleOutline.getDestination());
        assertEquals("Bundle", bundleOutline.getTitle());
        assertEquals("Folder Item 1", bundleOutline.getFirstChild().getTitle());
        assertEquals("Folder Item 2", bundleOutline.getLastChild().getTitle());
    }

    @Test
    void addItemWithInvalidPage() {
        pdfOutline = new PDFOutline(document, outlineTree);
        assertThrows(IndexOutOfBoundsException.class,
            () -> pdfOutline.addItem(-2,"test Invalid Page"));
    }

    @Test
    void mergeWithTableOfContentsAndOutlines() throws IOException {
        Bundle newBundle = createFlatTestBundleWithAdditionalDoc();
        HashMap<BundleDocument, File> documentsMap = new HashMap<>();
        documentsMap.put(newBundle.getDocuments().get(0), FILE_1);
        documentsMap.put(newBundle.getDocuments().get(1), FILE_2);

        PDFMerger merger = new PDFMerger();

        newBundle.setHasTableOfContents(true);
        File mergedFile = merger.merge(newBundle, documentsMap, null);

        try (PDDocument mergedDocument = Loader.loadPDF(mergedFile);
             PDDocument doc1 = Loader.loadPDF(FILE_1);
             PDDocument doc2 = Loader.loadPDF(FILE_2)) {

            final int numberOfPagesInTableOfContents = 1;
            final int expectedPages = doc1.getNumberOfPages()
                + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;
            assertEquals(expectedPages, mergedDocument.getNumberOfPages());

            PDDocumentOutline documentOutline = mergedDocument.getDocumentCatalog().getDocumentOutline();
            PDOutlineItem bundleOutline = documentOutline.getFirstChild();
            assertNotNull(bundleOutline);
            assertNotNull(bundleOutline.getDestination());
            assertEquals("Title of the bundle", bundleOutline.getTitle());

            PDOutlineItem firstLevel = bundleOutline.getFirstChild();
            assertEquals("Index Page", firstLevel.getTitle());

            PDOutlineItem bundleDoc1Item = firstLevel.getNextSibling();
            assertEquals("Bundle Doc 1", bundleDoc1Item.getTitle());
            PDOutlineItem bundleDoc1Outline = bundleDoc1Item.getFirstChild();
            assertEquals("Slide 1", bundleDoc1Outline.getTitle());

            PDOutlineItem bundleDoc2Item = bundleDoc1Item.getNextSibling();
            assertEquals("Bundle Doc 2", bundleDoc2Item.getTitle());
            PDOutlineItem bundleDoc2Outline = bundleDoc2Item.getFirstChild();
            assertEquals("Title of the bundle", bundleDoc2Outline.getTitle());
            assertEquals("Cover Page", bundleDoc2Outline.getFirstChild().getTitle());
            assertEquals("Index Page", bundleDoc2Outline.getFirstChild().getNextSibling().getTitle());

            PDOutlineItem subFolder1Outline = bundleDoc2Outline.getFirstChild().getNextSibling().getNextSibling();
            assertEquals("Folder 1", subFolder1Outline.getTitle());
            assertEquals("Bundle Doc 1", subFolder1Outline.getFirstChild().getTitle());
            assertEquals("Slide 1", subFolder1Outline.getFirstChild().getFirstChild().getTitle());
            assertEquals("Bundle Doc 2", subFolder1Outline.getNextSibling().getTitle());
        }
    }

    @Test
    void mergeWithSpecialOutlines() throws IOException {
        Bundle newBundle = createFlatTestBundleWithAdditionalDoc();
        HashMap<BundleDocument, File> documentsMap = new HashMap<>();
        documentsMap.put(newBundle.getDocuments().get(0), FILE_3);
        documentsMap.put(newBundle.getDocuments().get(1), FILE_4);

        PDFMerger merger = new PDFMerger();
        newBundle.setHasTableOfContents(true);
        File mergedFile = merger.merge(newBundle, documentsMap, null);

        try (PDDocument mergedDocument = Loader.loadPDF(mergedFile);
             PDDocument doc1 = Loader.loadPDF(FILE_3);
             PDDocument doc2 = Loader.loadPDF(FILE_4)) {

            final int numberOfPagesInTableOfContents = 1;
            final int expectedPages = doc1.getNumberOfPages()
                + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;
            assertEquals(expectedPages, mergedDocument.getNumberOfPages());

            PDDocumentOutline documentOutline = mergedDocument.getDocumentCatalog().getDocumentOutline();
            PDOutlineItem bundleOutline = documentOutline.getFirstChild();
            assertNotNull(bundleOutline);
            assertNotNull(bundleOutline.getDestination());
            assertEquals("Title of the bundle", bundleOutline.getTitle());

            PDOutlineItem firstLevel = bundleOutline.getFirstChild();
            assertEquals("Index Page", firstLevel.getTitle());

            PDOutlineItem bundleDoc1Item = firstLevel.getNextSibling();
            assertEquals("Bundle Doc 1", bundleDoc1Item.getTitle());
            PDOutlineItem bundleDoc1Outline = bundleDoc1Item.getFirstChild();
            assertEquals("2001: A Space Odyssey", bundleDoc1Outline.getTitle());
            assertEquals("link to IMDB", bundleDoc1Outline.getFirstChild().getTitle());
            assertEquals("instant info", bundleDoc1Outline.getFirstChild().getNextSibling().getTitle());

            PDOutlineItem bundleDoc1Outline2 = bundleDoc1Outline.getNextSibling();
            assertEquals("3-Iron", bundleDoc1Outline2.getTitle());
            assertEquals("link to IMDB", bundleDoc1Outline2.getFirstChild().getTitle());
            assertEquals("instant info", bundleDoc1Outline2.getFirstChild().getNextSibling().getTitle());

            PDOutlineItem bundleDoc2Item = bundleDoc1Item.getNextSibling();
            assertEquals("Bundle Doc 2", bundleDoc2Item.getTitle());
            PDOutlineItem bundleDoc2Outline = bundleDoc2Item.getFirstChild();
            assertEquals("link to test", bundleDoc2Outline.getTitle());
        }
    }

    @Test
    void getOutlinePageWithNullItemReturnsMinusOne() {
        pdfOutline = new PDFOutline(document, outlineTree);
        int result = pdfOutline.getOutlinePage(null, null);
        assertEquals(-1, result);
    }

    @Test
    void testRemoveNullObject() {
        // Mock the necessary objects and behavior
        PDOutlineItem outline = mock(PDOutlineItem.class);
        COSDictionary cosDictionary = mock(COSDictionary.class);
        COSName cosName = COSName.getPDFName("TestName");
        COSObject cosObject = mock(COSObject.class);

        // Setting up the mock behavior
        when(outline.getCOSObject()).thenReturn(cosDictionary);
        Map.Entry<COSName, COSBase> mapEntry = new AbstractMap.SimpleEntry<>(cosName, cosObject);
        Set<Map.Entry<COSName, COSBase>> entries = new HashSet<>();
        entries.add(mapEntry);
        when(cosDictionary.entrySet()).thenReturn(entries);
        when(cosObject.getObject()).thenReturn(null);

        pdfOutline = new PDFOutline(document, outlineTree);

        // Call the method to test
        PDOutlineItem result = pdfOutline.removeNullObject(outline);

        // Verify the expected behavior
        verify(cosDictionary).removeItem(cosName);

        // Check the result
        assertEquals(outline, result);
    }

    @Test
    void setRootOutlineDestinationWhenOutlineIsEmpty() {
        PDDocumentOutline pdOutline = new PDDocumentOutline();
        document.getDocumentCatalog().setDocumentOutline(pdOutline);
        assertNull(pdOutline.getFirstChild());

        pdfOutline = new PDFOutline(document, null);
        pdfOutline.setRootOutlineItemDest();
        assertNotNull(document.getDocumentCatalog().getDocumentOutline());
        assertNull(document.getDocumentCatalog().getDocumentOutline().getFirstChild());
    }

    @Test
    void removeNullObjectWithNullItem() {
        pdfOutline = new PDFOutline(document, null);
        assertNull(pdfOutline.removeNullObject(null));
    }

    @Test
    void removeNullObjectWhenItemCosObjectIsNull() {
        pdfOutline = new PDFOutline(document, null);
        PDOutlineItem mockItem = mock(PDOutlineItem.class);
        when(mockItem.getCOSObject()).thenReturn(null);
        assertEquals(mockItem, pdfOutline.removeNullObject(mockItem));
        verify(mockItem, times(1)).getCOSObject();
    }

    @Test
    void getOutlinePageOnDestinationError() throws IOException {
        PDOutlineItem mockItem = mock(PDOutlineItem.class);
        PDDocumentCatalog mockCatalog = mock(PDDocumentCatalog.class);
        when(mockItem.getDestination()).thenThrow(new RuntimeException("Simulated error"));

        pdfOutline = new PDFOutline(document, null);
        int pageNum = pdfOutline.getOutlinePage(mockItem, mockCatalog);
        assertEquals(-1, pageNum);
    }

    @Test
    void findPdOutlineItemForNonExistentKey() {
        PDDocumentOutline outline = new PDDocumentOutline();
        PDOutlineItem rootItem = new PDOutlineItem();
        rootItem.setTitle("Root");
        rootItem.getCOSObject().setItem(COSName.getPDFName("key1LRoot"), COSNull.NULL);
        outline.addLast(rootItem);
        document.getDocumentCatalog().setDocumentOutline(outline);

        pdfOutline = new PDFOutline(document, null);
        assertNull(pdfOutline.findPdOutlineItem(rootItem, "nonExistentKey"));
    }

    @Test
    void findPdOutlineNestedItem() {
        PDDocumentOutline outline = new PDDocumentOutline();
        PDOutlineItem rootItem = new PDOutlineItem();
        rootItem.setTitle("Root");
        rootItem.getCOSObject().setItem(COSName.getPDFName("key1LRoot"), COSNull.NULL);
        outline.addLast(rootItem);

        PDOutlineItem childItem = new PDOutlineItem();
        childItem.setTitle("Child");
        childItem.getCOSObject().setItem(COSName.getPDFName("key2LChild"), COSNull.NULL);
        rootItem.addLast(childItem);
        document.getDocumentCatalog().setDocumentOutline(outline);

        pdfOutline = new PDFOutline(document, null);
        assertEquals(childItem, pdfOutline.findPdOutlineItem(rootItem, "key2LChild"));
    }

    @Test
    void addOutlineToDocumentWhenParentNotFound() {
        SortableBundleItem rootItem = createMockSortableItem(1L, "TreeRoot");
        SortableBundleItem childItem = createMockSortableItem(2L, "TreeChild");
        outlineTree = new TreeNode<>(rootItem);
        outlineTree.addChild(childItem);

        PDDocumentOutline pdfDocOutline = new PDDocumentOutline();
        PDOutlineItem unrelatedPdfRoot = new PDOutlineItem();
        unrelatedPdfRoot.setTitle("UnrelatedPDFRoot");
        pdfDocOutline.addLast(unrelatedPdfRoot);
        document.getDocumentCatalog().setDocumentOutline(pdfDocOutline);

        pdfOutline = new PDFOutline(document, outlineTree);
        pdfOutline.addItem(childItem, 0);

        boolean isTopLevel = false;
        for (PDOutlineItem topLevelItem : document.getDocumentCatalog().getDocumentOutline().children()) {
            if (topLevelItem.getTitle().equals("TreeChild")) {
                isTopLevel = true;
                break;
            }
        }
        assertTrue(isTopLevel, "Item should be added as a top-level outline item.");
    }

    @Test
    void copyOutlineWhenNodeNotInTree() throws IOException {
        SortableBundleItem rootBundleItem = createMockSortableItem(1L, "RootBundleItem");
        outlineTree = new TreeNode<>(createMockSortableItem(999L, "DummyTreeRoot"));
        pdfOutline = new PDFOutline(document, outlineTree);
        pdfOutline.addBundleItem(rootBundleItem);

        PDOutlineItem dummyLastChild = new PDOutlineItem();
        dummyLastChild.setTitle("Dummy Last Child of Root");
        String keyForNonExistentItemInTree = "nonExistentKeyInTree";
        dummyLastChild.getCOSObject().setItem(COSName.getPDFName(keyForNonExistentItemInTree), COSNull.NULL);
        pdfOutline.rootOutline.addLast(dummyLastChild);

        try (PDDocument srcDoc = new PDDocument()) {
            srcDoc.addPage(new PDPage());
            PDDocumentOutline srcOutline = new PDDocumentOutline();
            PDOutlineItem srcItem1 = new PDOutlineItem();
            srcItem1.setTitle("SrcItem1");
            srcItem1.setDestination(srcDoc.getPage(0));
            srcOutline.addLast(srcItem1);
            srcDoc.getDocumentCatalog().setDocumentOutline(srcOutline);

            pdfOutline.copyOutline(srcOutline, srcDoc.getDocumentCatalog(),
                keyForNonExistentItemInTree, 0);
        }

        boolean found = false;
        for (PDOutlineItem item : dummyLastChild.children()) {
            if ("SrcItem1".equals(item.getTitle())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Copied item should be added as child of the item found by key.");
    }

    @Test
    void copyOutlineWhenParentNotInPdfOutline() throws IOException {
        SortableBundleItem treeRootItem = createMockSortableItem(1L, "TreeRoot");
        SortableBundleItem treeChildItem = createMockSortableItem(2L, "TreeChild");
        outlineTree = new TreeNode<>(treeRootItem);
        outlineTree.addChild(treeChildItem);

        pdfOutline = new PDFOutline(document, outlineTree);

        PDDocumentOutline docOutline = new PDDocumentOutline();
        PDOutlineItem pdfMainRoot = new PDOutlineItem();
        pdfMainRoot.setTitle("PDFMainRoot");
        pdfMainRoot.getCOSObject().setItem(COSName.getPDFName(
            createItemKeyForTest(createMockSortableItem(99L, "SomeOtherRoot"))), COSNull.NULL);
        docOutline.addLast(pdfMainRoot);
        document.getDocumentCatalog().setDocumentOutline(docOutline);

        PDOutlineItem pdfTreeChildItemPlaceholder = new PDOutlineItem();
        pdfTreeChildItemPlaceholder.setTitle(treeChildItem.getTitle());
        String keyForChildInTree = createItemKeyForTest(treeChildItem);
        pdfTreeChildItemPlaceholder.getCOSObject().setItem(COSName.getPDFName(keyForChildInTree), COSNull.NULL);
        pdfMainRoot.addLast(pdfTreeChildItemPlaceholder);

        try (PDDocument srcDoc = new PDDocument()) {
            srcDoc.addPage(new PDPage());
            PDDocumentOutline srcOutline = new PDDocumentOutline();
            PDOutlineItem srcItem1 = new PDOutlineItem();
            srcItem1.setTitle("SrcItem1Copied");
            srcItem1.setDestination(srcDoc.getPage(0));
            srcOutline.addLast(srcItem1);
            srcDoc.getDocumentCatalog().setDocumentOutline(srcOutline);

            pdfOutline.copyOutline(srcOutline, srcDoc.getDocumentCatalog(), keyForChildInTree, 0);
        }

        boolean found = false;
        for (PDOutlineItem item : pdfTreeChildItemPlaceholder.children()) {
            if ("SrcItem1Copied".equals(item.getTitle())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Copied item should be added as child of the pre-existing item for the key.");
    }

    @Test
    void setUpDestinationsIfCosObjectKeyAlreadyTracked() throws IOException {
        SortableBundleItem childSortableItem = createMockSortableItem(101L, "Child Sortable Item");
        outlineTree.addChild(childSortableItem);
        pdfOutline = new PDFOutline(document, outlineTree);
        pdfOutline.addBundleItem(rootSortableItem);

        String parentKeyInPdf = createItemKeyForTest(rootSortableItem);
        PDOutlineItem pdfParentOutlineItem = pdfOutline.findPdOutlineItem(
            document.getDocumentCatalog().getDocumentOutline().getFirstChild(), parentKeyInPdf);
        assertNotNull(pdfParentOutlineItem);

        PDOutlineItem pdfChildPlaceholder = new PDOutlineItem();
        pdfChildPlaceholder.setTitle(childSortableItem.getTitle());
        String keyForCopyOutline = createItemKeyForTest(childSortableItem);
        pdfChildPlaceholder.getCOSObject().setItem(COSName.getPDFName(keyForCopyOutline), COSNull.NULL);
        pdfParentOutlineItem.addLast(pdfChildPlaceholder);

        try (PDDocument srcDoc = new PDDocument()) {
            srcDoc.addPage(new PDPage());
            PDDocumentCatalog srcDocCatalog = srcDoc.getDocumentCatalog();
            PDDocumentOutline srcOutline = new PDDocumentOutline();
            srcDocCatalog.setDocumentOutline(srcOutline);

            COSObjectKey predefinedKeyForSkip = new COSObjectKey(123L, 0);
            pdfOutline.cosObjectKeys.clear();
            pdfOutline.cosObjectKeys.add(predefinedKeyForSkip);

            PDOutlineItem realSourceItemToSpy = new PDOutlineItem();
            realSourceItemToSpy.setTitle("ItemToSkipProcessing");
            PDActionGoTo action = new PDActionGoTo();
            PDPageXYZDestination dest = new PDPageXYZDestination();
            dest.setPage(srcDoc.getPage(0));
            action.setDestination(dest);
            realSourceItemToSpy.setAction(action);

            PDOutlineItem spiedSourceItem = spy(realSourceItemToSpy);
            COSDictionary actualCosDict = spiedSourceItem.getCOSObject();
            COSDictionary spiedActualCosDict = spy(actualCosDict);
            when(spiedSourceItem.getCOSObject()).thenReturn(spiedActualCosDict);
            when(spiedActualCosDict.getKey()).thenReturn(predefinedKeyForSkip);
            srcOutline.addLast(spiedSourceItem);

            pdfOutline.copyOutline(srcOutline, srcDocCatalog, keyForCopyOutline, 0);
        }

        PDOutlineItem copiedItem = null;
        for (PDOutlineItem child : pdfChildPlaceholder.children()) {
            if ("ItemToSkipProcessing".equals(child.getTitle())) {
                copiedItem = child;
                break;
            }
        }
        assertNotNull(copiedItem);
        assertNotNull(copiedItem.getAction());
        assertNull(copiedItem.getDestination());
    }

    @Test
    void getOutlinePageSetsDefaultTitleIfNull() throws IOException {
        pdfOutline = new PDFOutline(document, null);
        final PDOutlineItem mockItem = spy(new PDOutlineItem());
        final PDPageDestination mockDestination = mock(PDPageDestination.class);
        final PDDocumentCatalog mockCatalog = mock(PDDocumentCatalog.class);

        when(mockItem.getTitle()).thenReturn(null);
        when(mockItem.getDestination()).thenReturn(mockDestination);
        when(mockDestination.retrievePageNumber()).thenReturn(0);

        pdfOutline.getOutlinePage(mockItem, mockCatalog);
        verify(mockItem).setTitle("   ");
    }

    @Test
    void getOutlinePageRetrievesPage() throws IOException {
        pdfOutline = new PDFOutline(document, null);
        final PDOutlineItem mockItem = spy(new PDOutlineItem());
        final PDActionGoTo mockAction = mock(PDActionGoTo.class);
        final PDPageDestination mockDestination = mock(PDPageDestination.class);
        final PDDocumentCatalog mockCatalog = mock(PDDocumentCatalog.class);

        when(mockItem.getTitle()).thenReturn("ActionTitle");
        when(mockItem.getDestination()).thenReturn(null);
        when(mockItem.getAction()).thenReturn(mockAction);
        when(mockAction.getDestination()).thenReturn(mockDestination);
        when(mockDestination.retrievePageNumber()).thenReturn(1);

        document.addPage(new PDPage());
        int pageNum = pdfOutline.getOutlinePage(mockItem, mockCatalog);

        assertEquals(1, pageNum);
        verify(mockItem, atLeastOnce()).getTitle();
    }

    private String createItemKeyForTest(SortableBundleItem item) {
        return item.getId() + item.getTitle();
    }
}