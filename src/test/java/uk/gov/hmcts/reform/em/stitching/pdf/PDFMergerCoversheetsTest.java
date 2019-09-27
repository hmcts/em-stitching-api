package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.*;

public class PDFMergerCoversheetsTest {

    private static final File FILE_1 = new File(
            ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath()
    );

    private static final File FILE_2 = new File(
            ClassLoader.getSystemResource("annotationTemplate.pdf").getPath()
    );

    private Bundle bundle;

    @Test
    public void addFolderCoversheetsTest() throws IOException {
        bundle = createFolderedTestBundle();
        BundleFolder bundleFolder = bundle.getFolders().get(0);
        BundleDocument bundleDocument = bundleFolder.getDocuments().get(0);
        BundleDocument bundleDocument2 = bundle.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
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

        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
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

        assertEquals(expectedPages, actualPages);

        doc1.close();
        doc2.close();
        mergedDocument.close();
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

        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
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
        String folder1Description = bundle.getFolders().get(0).getDescription();
        String folder2Description = bundle.getFolders().get(1).getDescription();
        int stitchedDocFolder1DescriptionFrequency = countSubstrings(stitchedDocumentText, folder1Description);
        int stitchedDocFolder2DescriptionFrequency = countSubstrings(stitchedDocumentText, folder2Description);

        assertEquals(1, stitchedDocFolder1DescriptionFrequency);
        assertEquals(1, stitchedDocFolder2DescriptionFrequency);

        int indexOfFolder1Description = stitchedDocumentText.indexOf(folder1Description);
        int indexOfFolder2Description = stitchedDocumentText.indexOf(folder2Description);
        Assert.assertTrue(indexOfFolder1Description < indexOfFolder2Description);

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
    public void mergeWithSubfolderCoversheets() throws IOException {
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

        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
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
    public void ignoresEmptyFoldersTest() throws IOException {
        bundle = createFolderedTestBundle();
        BundleFolder bundleFolder = bundle.getFolders().get(0);
        bundleFolder.getDocuments().clear();
        BundleDocument bundleDocument2 = bundle.getDocuments().get(0);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument2, FILE_2);

        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
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
