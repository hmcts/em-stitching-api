package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PDFMergerTest {
    private static final File FILE_1 = new File(
        ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath()
    );

    private static final File FILE_2 = new File(
        ClassLoader.getSystemResource("annotationTemplate.pdf").getPath()
    );

    private List<Pair<BundleDocument, File>> documents;
    private Pair<BundleDocument, File> document1;
    private Pair<BundleDocument, File> document2;

    @Before
    public void setup() {
        Bundle bundle = createTestBundle();
        document1 = Pair.of(bundle.getDocuments().get(0), FILE_1);
        document2 = Pair.of(bundle.getDocuments().get(0), FILE_2);
        documents = new ArrayList<>();

        documents.add(document1);
        documents.add(document2);
    }

    // Table of Contents tests
    @Test
    public void mergeWithTableOfContents() throws IOException {
        PDFMerger merger = new PDFMerger();
        Bundle bundle = createTestBundle();
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
        Bundle bundle = createTestBundle();
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

        Bundle bundle = createTestBundle();
        bundle.setHasTableOfContents(true);
        File stitched = merger.merge(bundle, documents);

        String stitchedDocumentText = pdfStripper.getText(PDDocument.load(stitched));
        String firstFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_1));
        String secondFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_2));

        int stitchedDocBundleTitleFrequency = count(stitchedDocumentText, bundle.getBundleTitle());
        int firstDocBundleTitleFrequency = count(firstFileDocumentText, bundle.getBundleTitle());
        int secondDocBundleTitleFrequency = count(secondFileDocumentText, bundle.getBundleTitle());
        Assert.assertEquals(stitchedDocBundleTitleFrequency, firstDocBundleTitleFrequency + secondDocBundleTitleFrequency + bundleTextInTableOfContentsFrequency);
    }

    @Test
    public void noTableOfContentsBundleTitleFrequencyTest() throws IOException {
        PDFMerger merger = new PDFMerger();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        Bundle bundle = createTestBundle();
        bundle.setHasTableOfContents(false);
        File stitched = merger.merge(bundle, documents);

        String stitchedDocumentText = pdfStripper.getText(PDDocument.load(stitched));
        String firstFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_1));
        String secondFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_2));

        int stitchedDocBundleTitleFrequency = count(stitchedDocumentText, bundle.getBundleTitle());
        int firstDocBundleTitleFrequency = count(firstFileDocumentText, bundle.getBundleTitle());
        int secondDocBundleTitleFrequency = count(secondFileDocumentText, bundle.getBundleTitle());
        Assert.assertEquals(stitchedDocBundleTitleFrequency, firstDocBundleTitleFrequency + secondDocBundleTitleFrequency);
    }

    // TODO All file name tests
    @Test
    public void mergeWithFilename() throws IOException {
        PDFMerger merger = new PDFMerger();
        Bundle bundle = createTestBundle();
        bundle.setFileName("bundle_path.pdf");

        File stitched = merger.merge(bundle, documents);
        PDDocument stitchedDocument = PDDocument.load(stitched);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages();

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, stitchedDocument.getNumberOfPages());
    }

    // Utils
    private Bundle createTestBundle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("AAAAAAA");
        bundleDocument.setDocTitle("Bundle Doc 1");
        bundleDocument.setId(1L);
        bundle.getDocuments().add(bundleDocument);

        return bundle;
    }

    private static int count(String text, String find) {
        int index = 0;
        int count = 0;
        int length = find.length();
        while( (index = text.indexOf(find, index)) != -1 ) {
            index += length;
            count++;
        }
        return count;
    }

}
