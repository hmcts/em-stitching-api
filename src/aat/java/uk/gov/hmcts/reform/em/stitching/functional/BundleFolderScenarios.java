package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.assertj.core.util.Files;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getNumPages;

public class BundleFolderScenarios {

    private final TestUtil testUtil = new TestUtil();
    private final File document1 = new File(ClassLoader.getSystemResource("Document1.pdf").getPath());
    private final File document2 = new File(ClassLoader.getSystemResource("Document2.pdf").getPath());
    private static final String STITCHED_DOCUMENT_URI = "bundle.stitchedDocumentURI";
    // private static final String COVER_PAGE_TEMPLATE_ID = "bundle-cover-sheet.docx";
    private static final String COVER_PAGE_TEMPLATE_ID = Base64.getEncoder().encodeToString("bundle-cover-sheet.docx".getBytes());



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
        final int numFolderCoversheetsPages = 0;
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
        final int numFolderCoversheetsPages = 2;
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
        final int numFolderCoversheetsPages = 4;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;
        final int expectedPages = (getNumPages(document1) * 3) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testStitchWithFlatFoldersAndCoverPageAndFolderCoversheets() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithFlatFolders();
        bundle.setHasFolderCoversheets(true);
        bundle.setCoverpageTemplate(Base64.getEncoder().encodeToString(COVER_PAGE_TEMPLATE_ID.getBytes()));

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numCoverPagePages = 1;
        final int numDocCoversheetsPages = 2;
        final int numFolderCoversheetsPages = 2;
        final int numExtraPages = numContentsPages + numCoverPagePages + numDocCoversheetsPages + numFolderCoversheetsPages;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testStitchWithNestedFoldersAndCoverPageAndFolderCoversheets() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        bundle.setHasFolderCoversheets(true);
        bundle.setCoverpageTemplate(COVER_PAGE_TEMPLATE_ID);

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numCoverPagePages = 1;
        final int numDocCoversheetsPages = 4;
        final int numFolderCoversheetsPages = 4;
        final int numExtraPages = numContentsPages + numCoverPagePages + numDocCoversheetsPages + numFolderCoversheetsPages;
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
        final int numFolderCoversheetsPages = 1;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;
        final int expectedPages = getNumPages(document2) + numExtraPages;
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
        final int numFolderCoversheetsPages = 3;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;
        final int expectedPages = (getNumPages(document1) * 2) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        Files.delete(stitchedFile);

        Assert.assertEquals(expectedPages, actualPages);
    }
}
