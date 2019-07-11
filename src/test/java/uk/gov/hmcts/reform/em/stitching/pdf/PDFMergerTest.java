package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.*;

public class PDFMergerTest {
    private static final File FILE_1 = new File(
        ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath()
    );

    private static final File FILE_2 = new File(
        ClassLoader.getSystemResource("annotationTemplate.pdf").getPath()
    );

    private Bundle bundle;
    private HashMap<BundleDocument, File> documents;

    private void flatSetup() {
        bundle = createFlatTestBundle();
        documents = new HashMap<>();

        documents.put(bundle.getDocuments().get(0), FILE_1);
        documents.put(bundle.getDocuments().get(1), FILE_2);
    }

    @Test
    public void mergeWithTableOfContents() throws IOException {
        flatSetup();
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(true);
        File merged = merger.merge(bundle, documents);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    public void mergeWithoutTableOfContents() throws IOException {
        flatSetup();
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(false);

        File merged = merger.merge(bundle, documents);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages();

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    public void tableOfContentsBundleTitleFrequencyTest() throws IOException {
        flatSetup();
        PDFMerger merger = new PDFMerger();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        final int bundleTextInTableOfContentsFrequency = 1;

        bundle.setHasTableOfContents(true);
        File stitched = merger.merge(bundle, documents);

        String stitchedDocumentText = pdfStripper.getText(PDDocument.load(stitched));
        String firstFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_1));
        String secondFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_2));

        int stitchedDocBundleTitleFrequency = countSubstrings(stitchedDocumentText, bundle.getBundleTitle());
        int firstDocBundleTitleFrequency = countSubstrings(firstFileDocumentText, bundle.getBundleTitle());
        int secondDocBundleTitleFrequency = countSubstrings(secondFileDocumentText, bundle.getBundleTitle());
        int expectedBundleTitleFrequency = firstDocBundleTitleFrequency + secondDocBundleTitleFrequency + bundleTextInTableOfContentsFrequency;
        assertEquals(stitchedDocBundleTitleFrequency, expectedBundleTitleFrequency);
    }

    @Test
    public void noTableOfContentsBundleTitleFrequencyTest() throws IOException {
        flatSetup();
        PDFMerger merger = new PDFMerger();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        bundle.setHasTableOfContents(false);
        File stitched = merger.merge(bundle, documents);

        String stitchedDocumentText = pdfStripper.getText(PDDocument.load(stitched));
        String firstFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_1));
        String secondFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_2));

        int stitchedDocBundleTitleFrequency = countSubstrings(stitchedDocumentText, bundle.getBundleTitle());
        int firstDocBundleTitleFrequency = countSubstrings(firstFileDocumentText, bundle.getBundleTitle());
        int secondDocBundleTitleFrequency = countSubstrings(secondFileDocumentText, bundle.getBundleTitle());
        int expectedBundleTitleFrequency = firstDocBundleTitleFrequency + secondDocBundleTitleFrequency;
        assertEquals(stitchedDocBundleTitleFrequency, expectedBundleTitleFrequency);
    }


    //  Folder Coversheets stuff //

    @Test
    public void addFolderCoversheetsTest() throws IOException {
        bundle = createFolderedTestBundle();
        BundleFolder bundleFolder = bundle.getFolders().get(0);
        BundleDocument bundleDocument = bundleFolder.getDocuments().get(0);
        BundleDocument bundleDocument2 = bundle.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents);
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
        int noOfBundleFolderDescriptions = countSubstrings(stitchedDocumentText, bundleFolder.getDescription());

        assertEquals(1, noOfBundleFolderDescriptions);

        doc1.close();
        doc2.close();
        mergedDocument.close();
    }

    @Test
    public void folderCoversheetsToggleOffTest() throws IOException {
        bundle = createFolderedTestBundle();
        bundle.setHasFolderCoversheets(false);

        BundleDocument bundleDocument = bundle.getFolders().get(0).getDocuments().get(0);
        BundleDocument bundleDocument2 = bundle.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfTOCPages = 1;
        final int numberOfDocCoversheets = 0;
        final int numberOfFolderCoversheets = 0;
        final int numberOfExtraPages = numberOfTOCPages + numberOfDocCoversheets + numberOfFolderCoversheets;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfExtraPages;
        final int actualPages = mergedDocument.getNumberOfPages();

        doc1.close();
        doc2.close();
        mergedDocument.close();

        Assert.assertEquals(expectedPages, actualPages);
    }

    @Test
    public void mergeWithMultipleFolderCoversheets() throws IOException {
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
        File merged = merger.merge(bundle, documents);
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
        String stitchedDocumentText = pdfStripper.getText(mergedDocument);

        // Assert that folders are included in bundle TOC
        String folder1Name = bundle.getFolders().get(0).getFolderName();
        int stitchedDocFolder1NameFrequency = countSubstrings(stitchedDocumentText, folder1Name);
        assertEquals(2, stitchedDocFolder1NameFrequency);

        String folder1Description = bundle.getFolders().get(0).getDescription();
        String folder2Description = bundle.getFolders().get(1).getDescription();
        int indexOfFolder1Description = stitchedDocumentText.indexOf(folder1Description);
        int indexOfFolder2Description = stitchedDocumentText.indexOf(folder2Description);
        int stitchedDocFolder1DescriptionFrequency = countSubstrings(stitchedDocumentText, folder1Description);
        int stitchedDocFolder2DescriptionFrequency = countSubstrings(stitchedDocumentText, folder2Description);
        assertEquals(1, stitchedDocFolder1DescriptionFrequency);
        assertEquals(1, stitchedDocFolder2DescriptionFrequency);
        Assert.assertTrue(indexOfFolder1Description < indexOfFolder2Description);

        doc1.close();
        doc2.close();
        mergedDocument.close();
    }


    @Test
    public void ignoresEmptyFoldersTest() throws IOException {
        bundle = createFolderedTestBundle();
        BundleFolder bundleFolder = bundle.getFolders().get(0);
        bundleFolder.getDocuments().clear();
        BundleDocument bundleDocument2 = bundle.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundle, documents);
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
