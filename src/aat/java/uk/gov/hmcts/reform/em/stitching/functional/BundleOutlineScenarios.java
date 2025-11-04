package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getOutlinePage;

class BundleOutlineScenarios extends BaseTest {
    private static final String STITCHED_DOCUMENT_URI = "bundle.stitchedDocumentURI";
    private static final String INDEX_TITLE = "Index Page";

    @Autowired
    protected BundleOutlineScenarios(TestUtil testUtil) {
        super(testUtil);
    }

    @Test
    void testStitchBundleWithNoOutlines() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);


        final PDDocument doc = Loader.loadPDF(stitchedFile);
        final PDDocumentOutline stitchedOutline = doc.getDocumentCatalog().getDocumentOutline();

        FileUtils.deleteQuietly(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        assertEquals(bundle.getBundleTitle(), bundleOutline.getTitle());
        assertEquals(INDEX_TITLE, bundleOutline.getFirstChild().getTitle());
        assertEquals(bundle.getDocuments().getFirst().getDocTitle(),
            bundleOutline.getFirstChild().getNextSibling().getTitle());
        assertEquals(bundle.getDocuments().get(1).getDocTitle(),
            bundleOutline.getFirstChild().getNextSibling().getNextSibling().getTitle());

        doc.close();
    }

    @Test
    void testStitchBundleWithDocumentOutlines() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundle();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocument doc = Loader.loadPDF(stitchedFile);
        final PDDocumentOutline stitchedOutline = doc.getDocumentCatalog().getDocumentOutline();

        FileUtils.deleteQuietly(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        assertEquals(bundle.getBundleTitle(), bundleOutline.getTitle());
        assertEquals(INDEX_TITLE, bundleOutline.getFirstChild().getTitle());
        assertEquals(bundle.getDocuments().getFirst().getDocTitle(), 
            bundleOutline.getFirstChild().getNextSibling().getTitle());

        doc.close();
    }

    @Test
    void testStitchBundleWithFolderStructure() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithFlatFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocument doc = Loader.loadPDF(stitchedFile);
        final PDDocumentOutline stitchedOutline = doc.getDocumentCatalog().getDocumentOutline();

        FileUtils.deleteQuietly(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        assertEquals("Bundle with folders", bundleOutline.getTitle());
        assertEquals(INDEX_TITLE, bundleOutline.getFirstChild().getTitle());
        var folder1 = bundleOutline.getFirstChild().getNextSibling();
        assertEquals("Folder 1", folder1.getTitle());
        assertEquals("Title (Document1.pdf)", folder1.getFirstChild().getTitle());

        var folder2 = folder1.getNextSibling();
        assertEquals("Folder 2", folder2.getTitle());
        assertEquals("Title (Document2.pdf)", folder2.getFirstChild().getTitle());

        doc.close();
    }

    @Test
    void testStitchBundleWithNestedFolders() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocument doc = Loader.loadPDF(stitchedFile);
        final PDDocumentOutline stitchedOutline = doc.getDocumentCatalog().getDocumentOutline();

        FileUtils.deleteQuietly(stitchedFile);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        assertEquals("Bundle with folders", bundleOutline.getTitle());
        var child = bundleOutline.getFirstChild();
        assertEquals(INDEX_TITLE, child.getTitle());
        var folder1 = child.getNextSibling();
        assertEquals("Folder 1", folder1.getTitle());

        var folder1Subs = folder1.getFirstChild();

        assertEquals("Title (Document1.pdf)", folder1Subs.getTitle());
        var folder1a = folder1Subs.getNextSibling();
        assertEquals("Folder 1a", folder1a.getTitle());
        assertEquals(folder1a.getFirstChild().getTitle(), "Title (Document1a.pdf)");

        var folder1b = folder1a.getNextSibling();

        assertEquals(folder1b.getTitle(), "Folder 1b");
        child = folder1b.getFirstChild();
        assertEquals(child.getTitle(), "Title (Document1b.pdf)");

        var folder2 = folder1.getNextSibling();
        assertEquals(folder2.getTitle(), "Folder 2");
        assertEquals(folder2.getFirstChild().getTitle(), "Title (Document2.pdf)");

        doc.close();
    }

    @Test
    void testStitchBundleWithOnlyOneDocumentOutline() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOneDocumentWithAOutline();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocument doc = Loader.loadPDF(stitchedFile);
        final PDDocumentOutline stitchedOutline = doc.getDocumentCatalog().getDocumentOutline();

        FileUtils.deleteQuietly(stitchedFile);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        assertEquals(bundle.getBundleTitle(), bundleOutline.getTitle());
        assertEquals(INDEX_TITLE, bundleOutline.getFirstChild().getTitle());
        assertEquals(bundle.getDocuments().getFirst().getDocTitle(),
            bundleOutline.getFirstChild().getNextSibling().getTitle());

        assertEquals("Title (Document 2)", bundleOutline.getFirstChild().getNextSibling().getNextSibling().getTitle());
        assertNull(bundleOutline.getFirstChild().getNextSibling().getNextSibling().getNextSibling());

        doc.close();
    }

    @Test
    void testStitchBundleOutlineDestination() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundle();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocument doc = Loader.loadPDF(stitchedFile);
        final PDDocumentOutline stitchedOutline = doc.getDocumentCatalog().getDocumentOutline();

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();
        final int bundlePage = getOutlinePage(bundleOutline);

        PDOutlineItem tocOutline = bundleOutline.getFirstChild();
        final int tocPage = getOutlinePage(tocOutline);

        PDOutlineItem firstDocumentCoverSheetOutline = tocOutline.getNextSibling();
        final int document1CoversheetPage = getOutlinePage(firstDocumentCoverSheetOutline);

        PDOutlineItem secondDocumentFirstOutline = tocOutline.getNextSibling().getNextSibling();
        final int secondDocumentFirstPage = getOutlinePage(secondDocumentFirstOutline);


        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(bundle.getBundleTitle(), bundleOutline.getTitle());
        assertEquals(1, bundlePage);
        assertEquals(INDEX_TITLE, tocOutline.getTitle());
        assertEquals(1, tocPage);
        assertEquals(bundle.getDocuments().getFirst().getDocTitle(), firstDocumentCoverSheetOutline.getTitle());
        assertEquals(7, document1CoversheetPage);
        assertEquals("Title (Document 2)", secondDocumentFirstOutline.getTitle());
        assertEquals(108, secondDocumentFirstPage);

        doc.close();
    }

    @Test
    void testStitchBundleOutlineWithNoDestination() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleOutlineWithNoDestination();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocument doc = Loader.loadPDF(stitchedFile);
        final PDDocumentOutline stitchedOutline = doc.getDocumentCatalog().getDocumentOutline();

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();
        final int bundlePage = getOutlinePage(bundleOutline);

        PDOutlineItem outlineWithNoPage = bundleOutline.getFirstChild();
        final int document1CoversheetPage = getOutlinePage(outlineWithNoPage);

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(bundle.getBundleTitle(), bundleOutline.getTitle());
        assertEquals(1, bundlePage);
        assertEquals(INDEX_TITLE, outlineWithNoPage.getTitle());
        assertEquals(1, document1CoversheetPage);

        doc.close();
    }
}
