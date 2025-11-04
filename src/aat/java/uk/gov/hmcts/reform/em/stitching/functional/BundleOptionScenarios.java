package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getNumPages;

class BundleOptionScenarios extends BaseTest  {

    private final File document1 = new File(ClassLoader.getSystemResource("Document1.pdf").getPath());
    private final File document2 = new File(ClassLoader.getSystemResource("Document2.pdf").getPath());
    private final File document3 = new File(ClassLoader.getSystemResource("five-hundred-page.pdf").getPath());
    private final File document4 = new File(ClassLoader.getSystemResource("annotationTemplate.pdf").getPath());
    private final File document5 = new File(ClassLoader.getSystemResource(
            "SamplePDF_special_characters.pdf")
            .getPath()
    );
    private static final String BUNDLE_STITCHED_DOCUMENT_URI = "bundle.stitchedDocumentURI";

    @Autowired
    protected BundleOptionScenarios(TestUtil testUtil) {
        super(testUtil);
    }

    @Test
    void testDefaultValuesForTableOfContentsAndCoversheets() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(BUNDLE_STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final int numExtraPages = 3;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testTableOfContentsOffCoversheetsOn() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasTableOfContents(false);
        bundle.setHasCoversheets(true);
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(BUNDLE_STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final int numExtraPages = 2;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testTableOfContentsOnCoversheetsOff() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(BUNDLE_STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final int numExtraPages = 1;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testTableOfContentsOffCoversheetsOff() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasCoversheets(false);
        bundle.setHasTableOfContents(false);
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(BUNDLE_STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final int numExtraPages = 0;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testLargeValuesForTableOfContents() throws IOException, InterruptedException {

        final BundleDTO bundle = testUtil.getTestBundleWithLargeToc();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(BUNDLE_STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);
        final int numExtraPages = 17;
        final int expectedPages = getNumPages(document3) + getNumPages(document4) + getNumPages(document5)
                + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }
}
