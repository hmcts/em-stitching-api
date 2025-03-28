package uk.gov.hmcts.reform.em.stitching.functional;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleFolderDTO;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.getNumPages;

class BundleFolderScenarios extends BaseTest {

    private final File document1 = new File(ClassLoader.getSystemResource("Document1.pdf").getPath());
    private final File document2 = new File(ClassLoader.getSystemResource("Document2.pdf").getPath());
    private static final String STITCHED_DOCUMENT_URI = "bundle.stitchedDocumentURI";
    private static final String COVER_PAGE_TEMPLATE_ID = "FL-FRM-APP-ENG-00002.docx";

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

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testStitchBundleWithNestedFolders() throws IOException, InterruptedException {
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

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testStitchWithFlatFoldersAndFolderCoversheets() throws IOException, InterruptedException {
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

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testStitchWithNestedFoldersAndFolderCoversheets() throws IOException, InterruptedException {
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

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testRemovalOfEmptyFolders() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        bundle.setHasFolderCoversheets(true);

        BundleFolderDTO folder = new BundleFolderDTO();
        folder.setFolderName("Empty folder");

        BundleFolderDTO subFolder = new BundleFolderDTO();
        subFolder.setFolderName("Empty sub-folder");

        folder.getFolders().add(subFolder);
        bundle.getFolders().set(0, folder); // replace the first folder

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numDocCoversheetsPages = 1;
        final int numFolderCoversheetsPages = 1;
        final int numExtraPages = numContentsPages + numDocCoversheetsPages + numFolderCoversheetsPages;
        final int expectedPages = getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testStitchWithFlatFoldersAndCoverPageAndFolderCoversheets() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithFlatFolders();
        bundle.setHasFolderCoversheets(true);
        bundle.setCoverpageTemplate(COVER_PAGE_TEMPLATE_ID);
        bundle.setCoverpageTemplateData(JsonNodeFactory.instance.objectNode().put("caseNo", "12345"));

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numCoverPagePages = 2;
        final int numDocCoversheetsPages = 2;
        final int numFolderCoversheetsPages = 2;
        final int numExtraPages = numContentsPages + numCoverPagePages + numDocCoversheetsPages
                + numFolderCoversheetsPages;
        final int expectedPages = getNumPages(document1) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);
        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testStitchWithNestedFoldersAndCoverPageAndFolderCoversheets() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithNestedFolders();
        bundle.setHasFolderCoversheets(true);
        bundle.setCoverpageTemplate(COVER_PAGE_TEMPLATE_ID);
        bundle.setCoverpageTemplateData(JsonNodeFactory.instance.objectNode().put("caseNo", "12345"));

        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        final int numContentsPages = 1;
        final int numCoverPagePages = 2;
        final int numDocCoversheetsPages = 4;
        final int numFolderCoversheetsPages = 4;
        final int numExtraPages = numContentsPages + numCoverPagePages + numDocCoversheetsPages
                + numFolderCoversheetsPages;
        final int expectedPages = (getNumPages(document1) * 3) + getNumPages(document2) + numExtraPages;
        final int actualPages = getNumPages(stitchedFile);

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testStitchSkipsEmptyFlatFolderCoversheets() throws IOException, InterruptedException {
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

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testStitchSkipsEmptyNestedFolderCoversheets() throws IOException, InterruptedException {
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

        FileUtils.deleteQuietly(stitchedFile);

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testStitchBundleWithFlatFoldersAndLongDocumentTitle() throws IOException, InterruptedException {
        final BundleDTO bundle = testUtil.getTestBundleWithFlatFoldersAndLongDocumentTitle();
        final Response response = testUtil.processBundle(bundle);
        final String stitchedDocumentUri = response.getBody().jsonPath().getString(STITCHED_DOCUMENT_URI);
        final File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        PDFTextStripper pdfStripper = new PDFTextStripper();
        PDDocument pdDocument = Loader.loadPDF(stitchedFile);
        String stitchedDocumentText = pdfStripper.getText(pdDocument);
        pdDocument.close();
        stitchedDocumentText = stitchedDocumentText.replace("\n", " ");
        int stitchedDocTitleFrequency = StringUtils.countMatches(stitchedDocumentText,
            bundle.getFolders().get(0).getDocuments().get(0).getDocTitle());

        assertEquals(2, stitchedDocTitleFrequency);

        FileUtils.deleteQuietly(stitchedFile);
    }
}
