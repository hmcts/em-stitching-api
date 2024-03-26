package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithAdditionalDoc;

public class PDFOutlineTest {

    private final BundleDocument item = new BundleDocument();
    private TreeNode<SortableBundleItem> outlineTree = null;

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

    @Test
    public void createOutlineForDocument() throws IOException {
        PDDocument document = new PDDocument();
        document.addPage(new PDPage());
        PDFOutline pdfOutline = new PDFOutline(document, outlineTree);
        item.setId(1L);
        item.setDocTitle("Bundle");
        pdfOutline.addBundleItem(item);
        pdfOutline.setRootOutlineItemDest();

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();

        assertEquals("Bundle", documentOutline.getFirstChild().getTitle());
        assertNotNull(documentOutline.getFirstChild().getDestination());
        assertNotNull(documentOutline.getFirstChild());
    }

    @Test
    public void createSubOutlinesForDocument() throws IOException {
        PDDocument document = new PDDocument();
        outlineTree = new TreeNode(item);
        document.addPage(new PDPage());
        document.addPage(new PDPage());
        PDFOutline pdfOutline = new PDFOutline(document, outlineTree);
        item.setId(2L);
        item.setDocTitle("Bundle");
        pdfOutline.addBundleItem(item);
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

    @Test(expected = IndexOutOfBoundsException.class)
    public void addItemWithInvalidPage() {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document, outlineTree);
        pdfOutline.addItem(-2,"test Invalid Page");
    }

    /*
    Title of the bundle
        Index Page
        Bundle Doc 1
            Slide 1
        Bundle Doc 2
            Title of the bundle
                Cover Page
                Index Page
                Folder 1
                    Bundle Doc 1
                        Slide 1
                Bundle Doc 2
*/


    @Test
    public void mergeWithTableOfContentsAndOutlines() throws IOException {
        Bundle newBundle = createFlatTestBundleWithAdditionalDoc();
        HashMap<BundleDocument, File> documents2;

        documents2 = new HashMap<>();
        documents2.put(newBundle.getDocuments().get(0), FILE_1);
        documents2.put(newBundle.getDocuments().get(1), FILE_2);

        PDFMerger merger = new PDFMerger();

        newBundle.setHasTableOfContents(true);
        File merged = merger.merge(newBundle, documents2, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_2);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());

        PDDocumentOutline documentOutline = mergedDocument.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem bundleOutline = documentOutline.getFirstChild();
        assertNotNull(bundleOutline);
        assertNotNull(bundleOutline.getDestination());
        assertEquals("Title of the bundle", bundleOutline.getTitle());
        var firstLevel = bundleOutline.getFirstChild();
        assertEquals("Index Page", bundleOutline.getFirstChild().getTitle());
        assertEquals("Bundle Doc 1", firstLevel.getNextSibling().getTitle());

        var bundleDoc1Outline = firstLevel.getNextSibling().getFirstChild();
        assertEquals("Slide 1", bundleDoc1Outline.getTitle());

        assertEquals("Bundle Doc 2", firstLevel.getNextSibling().getNextSibling().getTitle());
        var bundleDoc2Outline = firstLevel.getNextSibling().getNextSibling().getFirstChild();
        assertEquals("Title of the bundle", bundleDoc2Outline.getTitle());

        assertEquals("Cover Page", bundleDoc2Outline.getFirstChild().getTitle());
        assertEquals("Index Page", bundleDoc2Outline.getFirstChild().getNextSibling().getTitle());

        var subFolder1Outline = bundleDoc2Outline.getFirstChild().getNextSibling().getNextSibling();
        assertEquals("Folder 1", subFolder1Outline.getTitle());
        assertEquals("Bundle Doc 1", subFolder1Outline.getFirstChild().getTitle());
        assertEquals("Slide 1", subFolder1Outline.getFirstChild().getFirstChild().getTitle());

        assertEquals("Folder 1", bundleDoc2Outline.getFirstChild().getNextSibling().getNextSibling().getTitle());
        assertEquals(
            "Bundle Doc 2",
            bundleDoc2Outline.getFirstChild().getNextSibling().getNextSibling().getNextSibling().getTitle()
        );


        mergedDocument.close();

    }

    @Test
    public void mergeWithSpecialOutlines() throws IOException {
        Bundle newBundle = createFlatTestBundleWithAdditionalDoc();
        HashMap<BundleDocument, File> documents2;

        documents2 = new HashMap<>();
        documents2.put(newBundle.getDocuments().get(0), FILE_3);
        documents2.put(newBundle.getDocuments().get(1), FILE_4);

        PDFMerger merger = new PDFMerger();

        newBundle.setHasTableOfContents(true);
        File merged = merger.merge(newBundle, documents2, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_3);
        PDDocument doc2 = Loader.loadPDF(FILE_4);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());

        PDDocumentOutline documentOutline = mergedDocument.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem bundleOutline = documentOutline.getFirstChild();
        assertNotNull(bundleOutline);
        assertNotNull(bundleOutline.getDestination());
        assertEquals("Title of the bundle", bundleOutline.getTitle());
        var firstLevel = bundleOutline.getFirstChild();
        assertEquals("Index Page", bundleOutline.getFirstChild().getTitle());
        assertEquals("Bundle Doc 1", firstLevel.getNextSibling().getTitle());

        var bundleDoc1Outline = firstLevel.getNextSibling().getFirstChild();
        assertEquals("2001: A Space Odyssey", bundleDoc1Outline.getTitle());

        var bundleDoc1Child1 = firstLevel.getNextSibling().getFirstChild().getFirstChild();
        assertEquals("link to IMDB", bundleDoc1Child1.getTitle());
        var bundleDoc1Child2 = firstLevel.getNextSibling().getFirstChild().getFirstChild().getNextSibling();
        assertEquals("instant info", bundleDoc1Child2.getTitle());

        var bundleDoc1Outline2 = firstLevel.getNextSibling().getFirstChild().getNextSibling();
        assertEquals("3-Iron", bundleDoc1Outline2.getTitle());

        var bundleDoc1Outline2child1 = firstLevel.getNextSibling().getFirstChild().getNextSibling().getFirstChild();
        assertEquals("link to IMDB", bundleDoc1Outline2child1.getTitle());
        var bundleDoc1Outline2child2 =
                firstLevel.getNextSibling().getFirstChild().getNextSibling().getFirstChild().getNextSibling();
        assertEquals("instant info", bundleDoc1Outline2child2.getTitle());

        assertEquals("Bundle Doc 2", firstLevel.getNextSibling().getNextSibling().getTitle());
        var bundleDoc2Outline = firstLevel.getNextSibling().getNextSibling().getFirstChild();
        assertEquals("link to test", bundleDoc2Outline.getTitle());

        mergedDocument.close();

    }

    @Test
    public void getOutlinePage_should_return_exception() {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document, outlineTree);
        int result = pdfOutline.getOutlinePage(null, null);
        assertEquals(-1, result);
    }
}
