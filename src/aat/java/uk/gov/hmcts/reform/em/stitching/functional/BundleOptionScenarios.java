package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.io.File;
import java.io.IOException;

import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getNumPages;

@Ignore("Disabled to remove v14 DB")
public class BundleOptionScenarios extends BaseTest  {

    private final File document1 = new File(ClassLoader.getSystemResource("Document1.pdf").getPath());
    private final File document2 = new File(ClassLoader.getSystemResource("Document2.pdf").getPath());
    private final File document3 = new File(ClassLoader.getSystemResource("five-hundred-page.pdf").getPath());
    private final File document4 = new File(ClassLoader.getSystemResource("annotationTemplate.pdf").getPath());
    private final File document5 = new File(ClassLoader.getSystemResource(
            "SamplePDF_special_characters.pdf")
            .getPath()
    );

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Test
    public void testDefaultValuesForTableOfContentsAndCoversheets() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString("bundle.stitchedDocumentURI");
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final int numExtraPages = 3;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testTableOfContentsOffCoversheetsOn() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasTableOfContents(false);
        bundle.setHasCoversheets(true);
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString("bundle.stitchedDocumentURI");
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final int numExtraPages = 2;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testTableOfContentsOnCoversheetsOff() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString("bundle.stitchedDocumentURI");
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final int numExtraPages = 1;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

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
        final int numExtraPages = 0;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testLargeValuesForTableOfContents() throws IOException, InterruptedException {

        final BundleDTO bundle = testUtil.getTestBundleWithLargeToc();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString("bundle.stitchedDocumentURI");
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final int numExtraPages = 17;
        final int expectedPages = getNumPages(document3) + getNumPages(document4) + getNumPages(document5)
                + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

}
