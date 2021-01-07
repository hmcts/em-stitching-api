package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.assertj.core.util.Files;
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

        Files.delete(stitchedFile);

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle Title");
        Assert.assertEquals(bundleOutline.getNextSibling().getTitle(), "Index Page");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getTitle(), "Title (Document 1)");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getNextSibling().getTitle(), "Title (Document 2)");
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

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle Title");
        Assert.assertEquals(bundleOutline.getNextSibling().getTitle(), "Index Page");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getTitle(), "Title (Document 1)");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getNextSibling().getTitle(),
                documentOutline.getFirstChild().getTitle());
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

        final PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals(bundleOutline.getTitle(),
                "Bundle with folders");
        Assert.assertEquals(bundleOutline.getNextSibling().getTitle(),
                "Index Page");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getTitle(),
                "Folder 1");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getNextSibling().getTitle(),
                "Title (Document1.pdf)");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getNextSibling().getNextSibling().getTitle(),
                "Folder 2");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getNextSibling().getNextSibling().getNextSibling().getTitle(),
                "Title (Document2.pdf)");
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

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle with folders");
        Assert.assertEquals(bundleOutline.getNextSibling().getTitle(), "Index Page");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getTitle(), "Folder 1");

        bundleOutline = bundleOutline.getNextSibling().getNextSibling();

        Assert.assertEquals(bundleOutline.getNextSibling().getTitle(), "Title (Document1.pdf)");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getTitle(), "Folder 1a");

        bundleOutline = bundleOutline.getNextSibling().getNextSibling();

        Assert.assertEquals(bundleOutline.getNextSibling().getTitle(), "Title (Document1a.pdf)");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getTitle(), "Folder 1b");

        bundleOutline = bundleOutline.getNextSibling().getNextSibling();

        Assert.assertEquals(bundleOutline.getNextSibling().getTitle(), "Title (Document1b.pdf)");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getTitle(), "Folder 2");
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

        PDOutlineItem bundleOutline = stitchedOutline.getFirstChild();

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle Title");
        Assert.assertEquals(bundleOutline.getNextSibling().getTitle(), "Index Page");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getTitle(), "Title (Document 1)");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling().getNextSibling().getTitle(),
                documentWithOutline.getFirstChild().getTitle());

        bundleOutline = bundleOutline.getNextSibling().getNextSibling().getNextSibling();

        Assert.assertEquals(bundleOutline.getNextSibling().getTitle(),"Title (Document 2)");
        Assert.assertEquals(bundleOutline.getNextSibling().getNextSibling(), null);
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

        PDOutlineItem tocOutline = bundleOutline.getNextSibling();
        final int tocPage = getOutlinePage(tocOutline);

        PDOutlineItem firstDocumentCoverSheetOutline = tocOutline.getNextSibling();
        final int document1CoversheetPage = getOutlinePage(firstDocumentCoverSheetOutline);

        PDOutlineItem firstDocumentFirstOutline = firstDocumentCoverSheetOutline.getNextSibling();
        final int firstDocumentFirstPage = getOutlinePage(firstDocumentFirstOutline);


        Files.delete(stitchedFile);

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
        Assert.assertEquals(firstDocumentFirstOutline.getTitle(),
                "Slide 1");
        Assert.assertEquals(firstDocumentFirstPage,
                8);
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

        PDOutlineItem outlineWithNoPage = bundleOutline.getNextSibling().getNextSibling().getNextSibling().getFirstChild();
        final int document1CoversheetPage = getOutlinePage(outlineWithNoPage);

        Files.delete(stitchedFile);

        Assert.assertEquals(bundleOutline.getTitle(), "Bundle Title");
        Assert.assertEquals(bundlePage, 1);
        Assert.assertEquals(outlineWithNoPage.getTitle(), "Index Page");
        Assert.assertEquals(document1CoversheetPage, -1);
    }
}
