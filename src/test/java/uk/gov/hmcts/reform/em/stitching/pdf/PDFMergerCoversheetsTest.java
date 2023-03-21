package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.*;
import org.junit.*;
import uk.gov.hmcts.reform.em.stitching.domain.*;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.*;

public class PDFMergerCoversheetsTest {

    private static final File FILE_1 = new File(
            ClassLoader.getSystemResource("test-files/TEST_INPUT_FILE.pdf").getPath()
    );

    private static final File FILE_2 = new File(
            ClassLoader.getSystemResource("test-files/annotationTemplate.pdf").getPath()
    );

    private Bundle bundle;

    private final File coverPageFile
        = new File(ClassLoader.getSystemResource("test-files/" + COVER_PAGE_TEMPLATE + ".pdf").getPath());
    private static final String COVER_PAGE_TEMPLATE = "FL-FRM-GOR-ENG-12345";

    @Test
    public void addFolderCoversheetsTest() throws IOException, DocumentTaskProcessingException {
        bundle = createFolderedTestBundle();
        bundle.setHasCoversheets(false);
        BundleFolder bundleFolder = bundle.getFolders().get(0);
        BundleDocument bundleDocument = bundleFolder.getDocuments().get(0);
        BundleDocument bundleDocument2 = bundle.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfTOCPages = 1;
        final int numberOfDocCoversheets = 0;
        final int numberOfFolderCoversheets = 1;
        final int numberOfExtraPages = numberOfTOCPages + numberOfDocCoversheets + numberOfFolderCoversheets;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfExtraPages;
        final int actualPages = mergedDocument.getNumberOfPages();

        assertEquals(expectedPages, actualPages);

        PDFTextStripper pdfStripper = new PDFTextStripper();
        String stitchedDocumentText = pdfStripper.getText(mergedDocument);
        int noOfBundleFolderTitles = countSubstrings(stitchedDocumentText, bundleFolder.getTitle());

        assertEquals(2, noOfBundleFolderTitles);

        doc1.close();
        doc2.close();
        mergedDocument.close();
    }

    @Test
    public void addFolderCoversheetsCoverPageOnTest() throws IOException, DocumentTaskProcessingException {
        bundle = createFolderedTestBundle();
        bundle.setHasCoversheets(false);
        BundleFolder bundleFolder = bundle.getFolders().get(0);
        BundleDocument bundleDocument = bundleFolder.getDocuments().get(0);
        BundleDocument bundleDocument2 = bundle.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents, coverPageFile);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfCoverPagePages = 1;
        final int numberOfTOCPages = 1;
        final int numberOfDocCoversheets = 0;
        final int numberOfFolderCoversheets = 1;
        final int numberOfExtraPages = numberOfCoverPagePages + numberOfTOCPages + numberOfDocCoversheets + numberOfFolderCoversheets;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfExtraPages;
        final int actualPages = mergedDocument.getNumberOfPages();

        assertEquals(expectedPages, actualPages);

        PDFTextStripper pdfStripper = new PDFTextStripper();
        String stitchedDocumentText = pdfStripper.getText(mergedDocument);
        int noOfBundleFolderTitles = countSubstrings(stitchedDocumentText, bundleFolder.getTitle());

        assertEquals(2, noOfBundleFolderTitles);

        doc1.close();
        doc2.close();
        mergedDocument.close();
    }

    @Test
    public void folderCoversheetsToggleOffTest() throws IOException, DocumentTaskProcessingException {
        bundle = createFolderedTestBundle();
        bundle.setHasFolderCoversheets(false);
        bundle.setHasCoversheets(false);

        BundleDocument bundleDocument = bundle.getFolders().get(0).getDocuments().get(0);
        BundleDocument bundleDocument2 = bundle.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfTOCPages = 1;
        final int numberOfDocCoversheets = 0;
        final int numberOfFolderCoversheets = 0;
        final int numberOfExtraPages = numberOfTOCPages + numberOfDocCoversheets + numberOfFolderCoversheets;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfExtraPages;
        final int actualPages = mergedDocument.getNumberOfPages();

        assertEquals(expectedPages, actualPages);

        doc1.close();
        doc2.close();
        mergedDocument.close();
    }

    @Test
    public void mergeWithMultipleFolderCoversheets() throws IOException, DocumentTaskProcessingException {
        Bundle bundle = createMultiFolderedTestBundle();
        bundle.setHasTableOfContents(true);
        bundle.setHasFolderCoversheets(true);
        bundle.setHasCoversheets(false);

        BundleDocument bundleDocument1 = bundle.getFolders().get(0).getDocuments().get(0);
        BundleDocument bundleDocument2 = bundle.getFolders().get(1).getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument1, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfTOCPages = 1;
        final int numberOfDocCoversheets = 0;
        final int numberOfFolderCoversheets = 2;
        final int numberOfExtraPages = numberOfTOCPages + numberOfDocCoversheets + numberOfFolderCoversheets;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfExtraPages;

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());

        PDFTextStripper pdfStripper = new PDFTextStripper();
        String folder1Name = bundle.getFolders().get(0).getFolderName();
        pdfStripper.setStartPage(0);
        pdfStripper.setEndPage(1);
        String firstPageText = pdfStripper.getText(mergedDocument);
        int folder1FrequencyInTOC = countSubstrings(firstPageText, folder1Name);

        assertEquals(1, folder1FrequencyInTOC);

        doc1.close();
        doc2.close();
        mergedDocument.close();
    }

    @Test
    public void mergeWithMultipleFolderCoversheetsAndDocumentCoversheets() throws IOException, DocumentTaskProcessingException {
        Bundle bundle = createMultiFolderedTestBundle();
        bundle.setHasTableOfContents(true);
        bundle.setHasFolderCoversheets(true);
        bundle.setHasCoversheets(true);

        BundleDocument bundleDocument1 = bundle.getFolders().get(0).getDocuments().get(0);
        BundleDocument bundleDocument2 = bundle.getFolders().get(1).getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument1, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfTOCPages = 1;
        final int numberOfDocCoversheets = 2;
        final int numberOfFolderCoversheets = 2;
        final int numberOfExtraPages = numberOfTOCPages + numberOfDocCoversheets + numberOfFolderCoversheets;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfExtraPages;

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());

        PDFTextStripper pdfStripper = new PDFTextStripper();
        String stitchedDocumentText = pdfStripper.getText(mergedDocument);
        String folder1Title = bundle.getFolders().get(0).getTitle();
        String folder2Title = bundle.getFolders().get(1).getTitle();
        int stitchedDocFolder1TitleFrequency = countSubstrings(stitchedDocumentText, folder1Title);
        int stitchedDocFolder2TitleFrequency = countSubstrings(stitchedDocumentText, folder2Title);

        assertEquals(2, stitchedDocFolder1TitleFrequency);
        assertEquals(2, stitchedDocFolder2TitleFrequency);

        int indexOfFolder1Title = stitchedDocumentText.indexOf(folder1Title);
        int indexOfFolder2Title = stitchedDocumentText.indexOf(folder2Title);
        Assert.assertTrue(indexOfFolder1Title < indexOfFolder2Title);

        String folder1Name = bundle.getFolders().get(0).getFolderName();
        pdfStripper.setStartPage(0);
        pdfStripper.setEndPage(1);
        String firstPageText = pdfStripper.getText(mergedDocument);
        int folder1FrequencyInTOC = countSubstrings(firstPageText, folder1Name);

        assertEquals(1, folder1FrequencyInTOC);

        doc1.close();
        doc2.close();
        mergedDocument.close();
    }

    @Test
    public void mergeWithSubfolderCoversheets() throws IOException, DocumentTaskProcessingException {
        Bundle bundle = createSubFolderedTestBundle();
        bundle.setHasTableOfContents(true);
        bundle.setHasFolderCoversheets(true);
        bundle.setHasCoversheets(false);

        BundleFolder folder1 = bundle.getFolders().get(0);
        BundleDocument bundleDocument1 = folder1.getDocuments().get(0);
        BundleFolder subfolder1 = folder1.getFolders().get(0);
        BundleDocument bundleDocument2 = subfolder1.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument1, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfTOCPages = 1;
        final int numberOfDocCoversheets = 0;
        final int numberOfFolderCoversheets = 2;
        final int numberOfExtraPages = numberOfTOCPages + numberOfDocCoversheets + numberOfFolderCoversheets;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfExtraPages;
        assertEquals(expectedPages, mergedDocument.getNumberOfPages());

        PDFTextStripper pdfStripper = new PDFTextStripper();
        pdfStripper.setStartPage(0);
        pdfStripper.setEndPage(1);
        String firstPageText = pdfStripper.getText(mergedDocument);
        int folder1Frequency = countSubstrings(firstPageText, folder1.getFolderName());

        assertEquals(1, folder1Frequency);

        doc1.close();
        doc2.close();
        mergedDocument.close();
    }

    @Test
    public void ignoresEmptyFoldersTest() throws IOException, DocumentTaskProcessingException {
        bundle = createFolderedTestBundle();
        bundle.setHasCoversheets(false);
        BundleFolder bundleFolder = bundle.getFolders().get(0);
        bundleFolder.getDocuments().clear();
        BundleDocument bundleDocument2 = bundle.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfTOCPages = 1;
        final int numberOfDocCoversheets = 0;
        final int numberOfFolderCoversheets = 0;
        final int numberOfExtraPages = numberOfTOCPages + numberOfDocCoversheets + numberOfFolderCoversheets;
        final int expectedPages = doc2.getNumberOfPages() + numberOfExtraPages;
        final int actualPages = mergedDocument.getNumberOfPages();

        assertEquals(expectedPages, actualPages);

        PDFTextStripper pdfStripper = new PDFTextStripper();
        String stitchedDocumentText = pdfStripper.getText(mergedDocument);
        int noOfBundleFolderDescriptions = countSubstrings(stitchedDocumentText, bundleFolder.getDescription());

        assertEquals(0, noOfBundleFolderDescriptions);

        doc2.close();
        mergedDocument.close();
    }
}
