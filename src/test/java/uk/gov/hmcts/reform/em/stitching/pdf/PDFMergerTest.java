package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.*;
import org.junit.*;
import uk.gov.hmcts.reform.em.stitching.domain.*;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;
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

    @Before
    public void setup() {
        bundle = createFlatTestBundle();
        documents = new HashMap<>();

        documents.put(bundle.getDocuments().get(0), FILE_1);
        documents.put(bundle.getDocuments().get(1), FILE_2);
    }

    @Test
    public void mergeWithTableOfContents() throws IOException {
        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
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
        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
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
        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
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
        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
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

    @Test
    public void testMultipleTableOfContentsPages() throws IOException {
        bundle.setHasTableOfContents(true);
        bundle.setDocuments(new ArrayList<>());
        documents = new HashMap<>();

        final int numDocuments = 200;

        for (int i = 0; i < numDocuments; i++) {
            BundleDocument bundleDocument = new BundleDocument();
            bundleDocument.setDocTitle("Bundle Doc " + i);
            bundle.getDocuments().add(bundleDocument);

            documents.put(bundleDocument, FILE_1);
        }

        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
        File stitched = merger.merge(bundle, documents);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument stitchedDocument = PDDocument.load(stitched);

        final int documentPages = doc1.getNumberOfPages() * numDocuments;
        final int expectedPages = documentPages + (int) Math.ceil((double)numDocuments / 40);
        final int actualPages = stitchedDocument.getNumberOfPages();

        doc1.close();
        stitchedDocument.close();

        assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testMultipleTableOfContentsPagesAndFolders() throws IOException {
        bundle.setHasTableOfContents(true);
        bundle.setHasFolderCoversheets(true);
        bundle.setDocuments(new ArrayList<>());
        documents = new HashMap<>();

        int numFolders = 50;
        int numDocuments = 0;

        for (int i = 0; i < numFolders; i++) {
            BundleFolder folder = new BundleFolder();
            folder.setFolderName("Folder " + i);
            bundle.getFolders().add(folder);

            for (int j = 0; j < 10; j++) {
                BundleDocument bundleDocument = new BundleDocument();
                bundleDocument.setDocTitle("Bundle Doc " + numDocuments++);
                folder.getDocuments().add(bundleDocument);

                documents.put(bundleDocument, FILE_1);
            }
        }

        PDFMerger merger = new PDFMerger(new PageRangeFormat(), new NumberOfPagesFormat());
        File stitched = merger.merge(bundle, documents);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument stitchedDocument = PDDocument.load(stitched);

        final int documentPages = numFolders + (doc1.getNumberOfPages() * numDocuments);
        final int tocItems = numDocuments + (numFolders * 3);
        final int tocPages = (int) Math.ceil((double) tocItems / 40);
        final int expectedPages = documentPages + tocPages;
        final int actualPages = stitchedDocument.getNumberOfPages();

        doc1.close();
        stitchedDocument.close();

        assertEquals(expectedPages, actualPages);
    }
}
