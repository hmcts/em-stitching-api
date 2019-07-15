package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.countSubstrings;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundle;

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
}
