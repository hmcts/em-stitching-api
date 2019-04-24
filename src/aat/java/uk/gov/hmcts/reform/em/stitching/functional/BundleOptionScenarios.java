package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.io.File;
import java.io.IOException;

public class BundleOptionScenarios {

    private final TestUtil testUtil = new TestUtil();
    private final File document1 = new File(ClassLoader.getSystemResource("Document1.pdf").getPath());
    private final File document2 = new File(ClassLoader.getSystemResource("Document2.pdf").getPath());

    @Test
    public void testDefaultValuesForTableOfContentsAndCoversheets() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString("bundle.stitchedDocumentURI");
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final long numExtraPages = 3;
        final long expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final long actualPages = getNumPages(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testTableOfContentsOffCoversheetsOn() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasTableOfContents(false);
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString("bundle.stitchedDocumentURI");
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final long numExtraPages = 2;
        final long expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final long actualPages = getNumPages(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testTableOfContentsOnCoversheetsOff() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasCoversheets(false);
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString("bundle.stitchedDocumentURI");
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final long numExtraPages = 1;
        final long expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final long actualPages = getNumPages(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testTableOfContentsOffCoversheetsOff() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasCoversheets(false);
        bundle.setHasTableOfContents(false);
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString("bundle.stitchedDocumentURI");
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final long numExtraPages = 0;
        final long expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final long actualPages = getNumPages(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    private int getNumPages(File file) throws IOException {
        final PDDocument doc = PDDocument.load(file);
        final int numPages = doc.getNumberOfPages();

        doc.close();

        return numPages;
    }
}
