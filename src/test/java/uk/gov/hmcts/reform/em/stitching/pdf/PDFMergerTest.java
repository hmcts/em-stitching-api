package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

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
        bundle = createTestBundle();
        documents = new HashMap<>();

        documents.put(bundle.getDocuments().get(0), FILE_1);
        documents.put(bundle.getDocuments().get(1), FILE_2);
    }

    @Test
    public void mergeWithTableOfContents() throws IOException {
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
    public void mergeWithFolderCoversheets() throws IOException {
        PDFMerger merger = new PDFMerger();
        Bundle bundleWithFolders = new Bundle();

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocTitle("Bundle Doc 1");
        BundleFolder folder1 = new BundleFolder();
        folder1.getDocuments().add(bundleDocument);
        bundleWithFolders.getFolders().add(folder1);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocTitle("Bundle Doc 2");
        BundleFolder folder2 = new BundleFolder();
        folder2.getDocuments().add(bundleDocument2);
        bundleWithFolders.getFolders().add(folder2);

        bundleWithFolders.setHasTableOfContents(false);
        bundleWithFolders.setHasFolderCoversheets(true);

        HashMap<BundleDocument, File> documents = new HashMap<>();
        documents.put(bundleDocument, FILE_1);
        documents.put(bundleDocument2, FILE_2);

        File merged = merger.merge(bundleWithFolders, documents);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfPagesInTableOfContents = 2;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    public void mergeWithoutTableOfContents() throws IOException {
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

    // Utils //
    private Bundle createTestBundle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("AAAAAAA");
        bundleDocument.setDocTitle("Bundle Doc 1");
        bundleDocument.setId(1L);
        bundle.getDocuments().add(bundleDocument);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocumentURI("BBBBBBB");
        bundleDocument2.setDocTitle("Bundle Doc 2");
        bundleDocument2.setId(1L);
        bundle.getDocuments().add(bundleDocument2);

        return bundle;
    }

    private static int countSubstrings(String text, String find) {
        int index = 0;
        int count = 0;
        int length = find.length();
        while ((index = text.indexOf(find, index)) != -1) {
            index += length;
            count++;
        }
        return count;
    }

}
