package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;

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
        Bundle bundle = BundleTest.getTestBundle();
        document1 = Pair.of(bundle.getDocuments().get(0), FILE_1);
        document2 = Pair.of(bundle.getDocuments().get(0), FILE_2);
        documents = new ArrayList<>();

        documents.add(document1);
        documents.add(document2);
    }

    @Test
    public void mergeWithTableOfContents() throws IOException {
        PDFMerger merger = new PDFMerger();
        Bundle bundle = createTestBundle();
        bundle.setHasTableOfContents(true);
        File merged = merger.merge(bundle, documents);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);
        int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + 1;
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
        int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages();
        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }


    // TODO Move these tests (coversheet and TOC mixes) to DocumentTaskServiceImplTest
    @Test
    public void tableOfContentsBundleTitleFrequencyTest() throws IOException {
        PDFMerger merger = new PDFMerger();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        Bundle bundle = createTestBundle();
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        File merged = merger.merge(bundle, documents);

        String mergedDocumentText = pdfStripper.getText(PDDocument.load(merged));
        String firstFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_1));
        String secondFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_2));

        int bundleTitleFrequency = count(mergedDocumentText, bundle.getBundleTitle());
        int firstDocBundleTitleFrequency = count(firstFileDocumentText, bundle.getBundleTitle());
        int secondDocBundleTitleFrequency = count(secondFileDocumentText, bundle.getBundleTitle());
        Assert.assertTrue(bundleTitleFrequency == firstDocBundleTitleFrequency + secondDocBundleTitleFrequency + 1);
    }

    @Test
    public void noTableOfContentsBundleTitleFrequencyTest() throws IOException {
        PDFMerger merger = new PDFMerger();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        Bundle bundle = createTestBundle();
        bundle.setHasTableOfContents(false);
        bundle.setHasCoversheets(true);
        File merged = merger.merge(bundle, documents);

        String mergedDocumentText = pdfStripper.getText(PDDocument.load(merged));
        String firstFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_1));
        String secondFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_2));

        int bundleTitleFrequency = count(mergedDocumentText, bundle.getBundleTitle());
        int firstDocBundleTitleFrequency = count(firstFileDocumentText, bundle.getBundleTitle());
        int secondDocBundleTitleFrequency = count(secondFileDocumentText, bundle.getBundleTitle());
        Assert.assertTrue(bundleTitleFrequency == firstDocBundleTitleFrequency + secondDocBundleTitleFrequency);
    }

    @Test
    public void tableOfContentsAndCoversheetsAddDocumentTitleToMergedDocumentTest() throws IOException {
        PDFMerger merger = new PDFMerger();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        Bundle bundle = createTestBundle();
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        File merged = merger.merge(bundle, documents);

        String mergedDocumentText = pdfStripper.getText(PDDocument.load(merged));
        String firstFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_1));
        String secondFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_2));

        int mergedDocFileTitleFrequency = count(mergedDocumentText, FILE_1.getName());
        int firstDocFileTitleFrequency = count(firstFileDocumentText, FILE_1.getName());
        int secondDocFileTitleFrequency = count(secondFileDocumentText, FILE_1.getName());
        System.out.println(mergedDocFileTitleFrequency);
        System.out.println(firstDocFileTitleFrequency);
        System.out.println(secondDocFileTitleFrequency);
        System.out.println("JJJ");
        System.out.println(firstFileDocumentText);
        System.out.println("file1 name");
        System.out.println(FILE_1.getName());
        Assert.assertTrue(mergedDocFileTitleFrequency == firstDocFileTitleFrequency + secondDocFileTitleFrequency + 1);
    }

    @Test
    public void TOCTrueCoversheetsFalse() throws IOException {
        PDFMerger merger = new PDFMerger();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        Bundle bundle = createTestBundle();
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        File merged = merger.merge(bundle, documents);

        String mergedDocumentText = pdfStripper.getText(PDDocument.load(merged));
        String firstFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_1));
        String secondFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_2));

        int bundleTitleFrequency = count(mergedDocumentText, bundle.getBundleTitle());
        int firstDocBundleTitleFrequency = count(firstFileDocumentText, bundle.getBundleTitle());
        int secondDocBundleTitleFrequency = count(secondFileDocumentText, bundle.getBundleTitle());
        Assert.assertTrue(bundleTitleFrequency == firstDocBundleTitleFrequency + secondDocBundleTitleFrequency + 1);
    }

    @Test
    public void TOCFalseCoversheetTrue() throws IOException {
        PDFMerger merger = new PDFMerger();
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
        bundle.setHasTableOfContents(false);
        bundle.setHasCoversheets(true);

        File merged = merger.merge(bundle, documents);
        PDDocument mergedDocument = PDDocument.load(merged);

        // bundle title shows up once
    }

    @Test
    public void TOCFalseCoversheetFalse() throws IOException {
        PDFMerger merger = new PDFMerger();
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
        bundle.setHasTableOfContents(false);
        bundle.setHasCoversheets(false);

        File merged = merger.merge(bundle, documents);
        PDDocument mergedDocument = PDDocument.load(merged);

        // bundle title shows up never
    }

    // file name unit tests
    // test for: non utf8 characters in file name, file already exists, standard
    private Bundle createTestBundle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
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
