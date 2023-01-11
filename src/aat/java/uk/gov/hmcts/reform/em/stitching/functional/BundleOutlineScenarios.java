package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.io.File;
import java.io.IOException;

import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getDocumentOutline;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getOutlinePage;

public class BundleOutlineScenarios extends BaseTest {

    private final File onePageDocument = new File(ClassLoader.getSystemResource("one-page.pdf").getPath());
    private final File hundredPageDocument = new File(ClassLoader.getSystemResource("hundred-page.pdf").getPath());
    private static final String STITCHED_DOCUMENT_URI = "bundle.stitchedDocumentURI";

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Test
    public void testStitchBundleWithNoOutlines() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle Title");
        Assert.assertEquals(bundleOutline.getFirstChild().getTitle(), "Index Page");
        Assert.assertEquals(bundleOutline.getFirstChild().getNextSibling().getTitle(), "Title (Document 1)");
        Assert.assertEquals(bundleOutline.getFirstChild().getNextSibling().getNextSibling().getTitle(), "Title (Document 2)");
    }

    @Test
    public void testStitchBundleWithDocumentOutlines() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundle();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);
        final PDDocumentOutline documentOutline = getDocumentOutline(hundredPageDocument);

        FileUtils.deleteQuietly(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle Title");
        Assert.assertEquals(bundleOutline.getFirstChild().getTitle(), "Index Page");
        Assert.assertEquals(bundleOutline.getFirstChild().getNextSibling().getTitle(), "Title (Document 1)");
    //    Assert.assertEquals(bundleOutline.getFirstChild().getNextSibling().getNextSibling().getTitle(),
    //            documentOutline.getFirstChild().getTitle());
    }

    @Test
    public void testStitchBundleWithFolderStructure() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithFlatFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals("Bundle with folders", bundleOutline.getTitle());
        Assert.assertEquals("Index Page", bundleOutline.getFirstChild().getTitle());
        var folder1 = bundleOutline.getFirstChild().getNextSibling();
        Assert.assertEquals("Folder 1", folder1.getTitle());
        Assert.assertEquals("Title (Document1.pdf)", folder1.getFirstChild().getTitle());

        var folder2 = folder1.getNextSibling();
        Assert.assertEquals("Folder 2", folder2.getTitle());
        Assert.assertEquals("Title (Document2.pdf)", folder2.getFirstChild().getTitle());
    }

    @Test
    public void testStitchBundleWithNestedFolders() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle with folders");
        var child = bundleOutline.getFirstChild();
        Assert.assertEquals(child.getTitle(), "Index Page");
        var folder1 = child.getNextSibling();
        Assert.assertEquals(folder1.getTitle(), "Folder 1");

        var folder1Subs = folder1.getFirstChild();

        Assert.assertEquals("Title (Document1.pdf)", folder1Subs.getTitle());
        var folder1a = folder1Subs.getNextSibling();
        Assert.assertEquals("Folder 1a", folder1a.getTitle());
        Assert.assertEquals(folder1a.getFirstChild().getTitle(), "Title (Document1a.pdf)");

        var folder1b = folder1a.getNextSibling();

        Assert.assertEquals(folder1b.getTitle(), "Folder 1b");
        child = folder1b.getFirstChild();
        Assert.assertEquals(child.getTitle(), "Title (Document1b.pdf)");

        var folder2 = folder1.getNextSibling();
        Assert.assertEquals(folder2.getTitle(), "Folder 2");
        Assert.assertEquals(folder2.getFirstChild(), "Title (Document2.pdf)");
    }

    @Test
    public void testStitchBundleWithOnlyOneDocumentOutline() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOneDocumentWithAOutline();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);
        final PDDocumentOutline documentWithOutline = getDocumentOutline(onePageDocument);

        FileUtils.deleteQuietly(stitchedFile);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle Title");
        Assert.assertEquals(bundleOutline.getFirstChild().getTitle(), "Index Page");
        Assert.assertEquals(bundleOutline.getFirstChild().getNextSibling().getTitle(), "Title (Document 1)");
//        Assert.assertEquals(bundleOutline.getFirstChild().getNextSibling().getNextSibling().getTitle(),
//                documentWithOutline.getFirstChild().getTitle());
//
//        bundleOutline = bundleOutline.getNextSibling().getNextSibling().getNextSibling();

        Assert.assertEquals(bundleOutline.getFirstChild().getNextSibling().getNextSibling().getTitle(),"Title (Document 2)");
        Assert.assertEquals(bundleOutline.getFirstChild().getNextSibling().getNextSibling().getNextSibling(), null);
    }

    @Test
    public void testStitchBundleOutlineDestination() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundle();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();
        final int bundlePage = getOutlinePage(bundleOutline);

        PDOutlineItem tocOutline = bundleOutline.getFirstChild();
        final int tocPage = getOutlinePage(tocOutline);

        PDOutlineItem firstDocumentCoverSheetOutline = tocOutline.getNextSibling();
        final int document1CoversheetPage = getOutlinePage(firstDocumentCoverSheetOutline);

        PDOutlineItem secondDocumentFirstOutline = tocOutline.getNextSibling().getNextSibling();
        final int secondDocumentFirstPage = getOutlinePage(secondDocumentFirstOutline);


        FileUtils.deleteQuietly(stitchedFile);

        Assert.assertEquals(bundleOutline.getTitle(),
                "Bundle Title");
        Assert.assertEquals(bundlePage,
                1);
        Assert.assertEquals(tocOutline.getTitle(),
                "Index Page");
        Assert.assertEquals(tocPage,
                1);
        Assert.assertEquals(firstDocumentCoverSheetOutline.getTitle(),
                "Title (Document 1)");
        Assert.assertEquals(document1CoversheetPage,
                7);
        Assert.assertEquals(secondDocumentFirstOutline.getTitle(),
                "Title (Document 2)");
        Assert.assertEquals(108, secondDocumentFirstPage);
    }

    @Test
    public void testStitchBundleOutlineWithNoDestination() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleOutlineWithNoDestination();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final PDDocumentOutline stitchedOutline = getDocumentOutline(stitchedFile);

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();
        final int bundlePage = getOutlinePage(bundleOutline);

        PDOutlineItem outlineWithNoPage = bundleOutline.getFirstChild();
        final int document1CoversheetPage = getOutlinePage(outlineWithNoPage);

        FileUtils.deleteQuietly(stitchedFile);

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle Title");
        Assert.assertEquals(bundlePage, 1);
        Assert.assertEquals(outlineWithNoPage.getTitle(), "Index Page");
        Assert.assertEquals(document1CoversheetPage, 1);
    }
}
