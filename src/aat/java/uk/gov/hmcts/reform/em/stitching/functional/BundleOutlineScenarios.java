package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getDocumentOutline;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getOutlinePage;

public class BundleOutlineScenarios extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleOutlineScenarios.class);

    private final File onePageDocument = new File(ClassLoader.getSystemResource("one-page.pdf").getPath());
    private final File hundredPageDocument = new File(ClassLoader.getSystemResource("hundred-page.pdf").getPath());
    private static final String STITCHED_DOCUMENT_URI = "bundle.stitchedDocumentURI";
    private File stitchedFile;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @AfterEach
    void clear() {
        FileUtils.deleteQuietly(stitchedFile);
    }

    @Test
    public void testStitchBundleWithNoOutlines() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        assertEquals("Bundle Title", bundleOutline.getTitle());
        assertEquals("Index Page", bundleOutline.getFirstChild().getTitle());
        assertEquals("Title (Document 1)", bundleOutline.getFirstChild().getNextSibling().getTitle());
        assertEquals("Title (Document 2)", bundleOutline.getFirstChild().getNextSibling().getNextSibling().getTitle());
    }

    @Test
    public void testStitchBundleWithDocumentOutlines() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundle();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        assertEquals("Bundle Title", bundleOutline.getTitle());
        assertEquals("Index Page", bundleOutline.getFirstChild().getTitle());
        assertEquals("Title (Document 1)", bundleOutline.getFirstChild().getNextSibling().getTitle());
    }

    @Test
    public void testStitchBundleWithFolderStructure() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithFlatFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        assertEquals("Bundle with folders", bundleOutline.getTitle());
        assertEquals("Index Page", bundleOutline.getFirstChild().getTitle());
        var folder1 = bundleOutline.getFirstChild().getNextSibling();
        assertEquals("Folder 1", folder1.getTitle());
        assertEquals("Title (Document1.pdf)", folder1.getFirstChild().getTitle());

        var folder2 = folder1.getNextSibling();
        assertEquals("Folder 2", folder2.getTitle());
        assertEquals("Title (Document2.pdf)", folder2.getFirstChild().getTitle());
    }

    @Test
    public void testStitchBundleWithNestedFolders() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        assertEquals("Bundle with folders", bundleOutline.getTitle());
        var child = bundleOutline.getFirstChild();
        assertEquals("Index Page", child.getTitle());
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
    }

    @Test
    public void testStitchBundleWithOnlyOneDocumentOutline() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOneDocumentWithAOutline();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        LOGGER.info("stitchedOutline hasChildren {} ", stitchedOutline.hasChildren());
        LOGGER.info("stitchedOutline keySet {} ", stitchedOutline.getCOSObject().keySet());
        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();


        assertEquals("Bundle Title", bundleOutline.getTitle());
        assertEquals("Index Page", bundleOutline.getFirstChild().getTitle());
        assertEquals("Title (Document 1)", bundleOutline.getFirstChild().getNextSibling().getTitle());

        assertEquals("Title (Document 2)", bundleOutline.getFirstChild().getNextSibling().getNextSibling().getTitle());
        assertNull(bundleOutline.getFirstChild().getNextSibling().getNextSibling().getNextSibling());
    }

    @Test
    public void testStitchBundleOutlineDestination() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundle();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();
        final int bundlePage = getOutlinePage(bundleOutline);

        PDOutlineItem tocOutline = bundleOutline.getFirstChild();
        final int tocPage = getOutlinePage(tocOutline);

        PDOutlineItem firstDocumentCoverSheetOutline = tocOutline.getNextSibling();
        final int document1CoversheetPage = getOutlinePage(firstDocumentCoverSheetOutline);

        PDOutlineItem secondDocumentFirstOutline = tocOutline.getNextSibling().getNextSibling();
        final int secondDocumentFirstPage = getOutlinePage(secondDocumentFirstOutline);

        assertEquals("Bundle Title", bundleOutline.getTitle());
        assertEquals(1, bundlePage);
        assertEquals("Index Page", tocOutline.getTitle());
        assertEquals(1, tocPage);
        assertEquals("Title (Document 1)", firstDocumentCoverSheetOutline.getTitle());
        assertEquals(7, document1CoversheetPage);
        assertEquals("Title (Document 2)", secondDocumentFirstOutline.getTitle());
        assertEquals(108, secondDocumentFirstPage);
    }

    @Test
    public void testStitchBundleOutlineWithNoDestination() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleOutlineWithNoDestination();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();
        final int bundlePage = getOutlinePage(bundleOutline);

        PDOutlineItem outlineWithNoPage = bundleOutline.getFirstChild();
        final int document1CoversheetPage = getOutlinePage(outlineWithNoPage);

        assertEquals("Bundle Title", bundleOutline.getTitle());
        assertEquals(1, bundlePage);
        assertEquals("Index Page", outlineWithNoPage.getTitle());
        assertEquals(1, document1CoversheetPage);
    }
}
