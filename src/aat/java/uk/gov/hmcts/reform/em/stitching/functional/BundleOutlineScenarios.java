package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.assertj.core.util.Files;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.io.File;
import java.io.IOException;

import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getDocumentOutline;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getOutlinePage;

public class BundleOutlineScenarios {

    private final TestUtil testUtil = new TestUtil();
    private final File onePageDocument = new File(ClassLoader.getSystemResource("one-page.pdf").getPath());
    private final File hundredPageDocument = new File(ClassLoader.getSystemResource("hundred-page.pdf").getPath());
    private static final String STITCHED_DOCUMENT_URI = "bundle.stitchedDocumentURI";

    @Test
    public void testStitchBundle() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(stitchedOutline.getFirstChild().getTitle(), "Bundle Title");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getTitle(), "Table of Contents");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getTitle(), "Title (Document 1)");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getNextSibling().getTitle(), "Title (Document 2)");
    }

    @Test
    public void testStitchBundleWithDocumentOutlines() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundle();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);
        final PDDocumentOutline documentOutline = getDocumentOutline(hundredPageDocument);

        Files.delete(stitchedFile);

        Assert.assertEquals(stitchedOutline.getFirstChild().getTitle(), "Bundle Title");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getTitle(), "Table of Contents");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getTitle(), "Title (Document 1)");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getNextSibling().getTitle(), documentOutline.getFirstChild().getTitle());
    }

    @Test
    public void testStitchBundleWithFolderStructure() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithFlatFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(stitchedOutline.getFirstChild().getTitle(), "Bundle with folders");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getTitle(), "Table of Contents");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getTitle(), "Folder 1");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getFirstChild().getTitle(), "Title (Document1.pdf)");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getNextSibling().getTitle(), "Folder 2");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getNextSibling().getFirstChild().getTitle(), "Title (Document2.pdf)");
    }

    @Test
    public void testStitchBundleWithNestedFolders() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(stitchedOutline.getFirstChild().getTitle(), "Bundle with folders");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getTitle(), "Table of Contents");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getTitle(), "Folder 1");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getFirstChild().getTitle(), "Title (Document1.pdf)");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getTitle(), "Folder 1a");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getFirstChild().getTitle(), "Title (Document1a.pdf)");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getTitle(), "Folder 1b");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getFirstChild().getTitle(), "Title (Document1b.pdf)");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getNextSibling().getTitle(), "Folder 2");
    }

    @Test
    public void testStitchBundleWithOnlyOneDocumentOutline() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOneDocumentWithAOutline();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);
        final PDDocumentOutline documentWithOutline = getDocumentOutline(onePageDocument);

        Files.delete(stitchedFile);

        Assert.assertEquals(stitchedOutline.getFirstChild().getTitle(), "Bundle Title");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getTitle(), "Table of Contents");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getTitle(), "Title (Document 1)");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getNextSibling().getTitle(), documentWithOutline.getFirstChild().getTitle());
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getTitle(), "Title (Document 2)");
        Assert.assertEquals(stitchedOutline.getFirstChild().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getNextSibling(), null);
    }

    @Test
    public void testStitchBundleOutlineDestination() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundle();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);
        final PDDocumentOutline documentWithOutline = getDocumentOutline(hundredPageDocument);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();
        int bundlePage = getOutlinePage(bundleOutline);

        PDOutlineItem tocOutline = bundleOutline.getFirstChild();
        int tocPage = getOutlinePage(tocOutline);

        PDOutlineItem Document1CoverSheetOutline = tocOutline.getNextSibling();
        int Document1CoversheetPage = getOutlinePage(Document1CoverSheetOutline);

        PDOutlineItem Document1FirstOutline = Document1CoverSheetOutline.getNextSibling();
        int Document1FirstPage = getOutlinePage(Document1FirstOutline);


        Files.delete(stitchedFile);

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle Title");
        Assert.assertEquals(bundlePage, 1);
        Assert.assertEquals(tocOutline.getTitle(), "Table of Contents");
        Assert.assertEquals(tocPage, 1);
        Assert.assertEquals(Document1CoverSheetOutline.getTitle(), "Title (Document 1)");
        Assert.assertEquals(Document1CoversheetPage, 2);
        Assert.assertEquals(Document1FirstOutline.getTitle(), "Slide 1");
        Assert.assertEquals(Document1FirstPage, 3);
    }
}
