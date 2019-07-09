package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.assertj.core.util.Files;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.io.File;
import java.io.IOException;

import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getNumPages;

public class BundleFolderScenarios {

    private final TestUtil testUtil = new TestUtil();
    private final File document1 = new File(ClassLoader.getSystemResource("Document1.pdf").getPath());
    private final File document2 = new File(ClassLoader.getSystemResource("Document2.pdf").getPath());
    private static final String STITCHED_DOCUMENT_URI = "bundle.stitchedDocumentURI";

    @Test
    public void testStitchBundleWithFlatFolders() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithFlatFolders();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numDocCoversheetsPages = 2;
        final int numFolderCoversheetsPages = 0;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;

        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testStitchBundleWithNestedFolders() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numDocCoversheetsPages = 4;
        final int numFoldercoversheetsPages = 0;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;

        final int expectedPages = (getNumPages(document1) * 3) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testStitchWithFlatFoldersAndFolderCoversheets() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithFlatFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numDocCoversheetsPages = 2;
        final int numFoldercoversheetsPages = 2;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testStitchWithNestedFoldersAndFolderCoversheets() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numDocCoversheetsPages = 4;
        final int numFoldercoversheetsPages = 4;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;
        final int expectedPages = (getNumPages(document1) * 3) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testStitchSkipsEmptyFlatFolderCoversheets() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithFlatFolders();
        bundle.getFolders().get(1).getDocuments().clear();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numDocCoversheetsPages = 1;
        final int numFoldercoversheetsPages = 1;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testStitchSkipsEmptyNestedFolderCoversheets() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        bundle.getFolders().get(0).getFolders().get(0).getDocuments().clear();
        bundle.setHasFolderCoversheets(true);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numDocCoversheetsPages = 3;
        final int numFoldercoversheetsPages = 3;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;
        final int expectedPages = (getNumPages(document1) * 2) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }
}
