package uk.gov.hmcts.reform.em.stitching.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleFolder;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.countSubstrings;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundle;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithAdditionalDoc;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithSameDocNameAsSubtitle;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithSpecialChars;

public class PDFMergerTest {
    private static final File FILE_1 = new File(
            ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath()
    );
    private static final File FILE_2 = new File(
            ClassLoader.getSystemResource("annotationTemplate.pdf").getPath()
           );
    private static final File FILE_3 = new File(
            ClassLoader.getSystemResource("Potential_Energy_PDF.pdf").getPath()
    );

    private Bundle bundle;
    private HashMap<BundleDocument, File> documents;
    private File coverPageFile;
    private JsonNode coverPageData;

    private static final String COVER_PAGE_TEMPLATE = "FL-FRM-GOR-ENG-12345";

    @Before
    public void setup() {
        bundle = createFlatTestBundle();
        documents = new HashMap<>();
        coverPageFile = new File(ClassLoader.getSystemResource(COVER_PAGE_TEMPLATE + ".pdf").getPath());

        coverPageData = JsonNodeFactory.instance.objectNode().put("caseNo", "12345");

        documents.put(bundle.getDocuments().get(0), FILE_1);
        documents.put(bundle.getDocuments().get(1), FILE_2);
    }

    @Test
    public void mergeWithTableOfContents() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(true);
        File merged = merger.merge(bundle, documents, null);
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
    public void mergeWithTableOfContentsAndCoverPage() throws IOException {
        PDFMerger merger = new PDFMerger();

        bundle.setCoverpageTemplateData(coverPageData);
        bundle.setHasTableOfContents(true);
        bundle.setCoverpageTemplate(COVER_PAGE_TEMPLATE);
        File merged = merger.merge(bundle, documents, coverPageFile);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfPagesInTableOfContents = 1;
        final int numberOfPagesCoverPage = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages()
                + numberOfPagesInTableOfContents + numberOfPagesCoverPage;

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    public void mergeWithoutTableOfContents() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(false);

        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages();

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    public void mergeWithoutTableOfContentsAndCoverPage() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(false);

        File merged = merger.merge(bundle, documents, coverPageFile);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);

        final int numberOfPagesCoverPage = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesCoverPage;

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    public void noTableOfContentsBundleTitleFrequencyTest() throws IOException {
        PDFMerger merger = new PDFMerger();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        bundle.setHasTableOfContents(false);
        File stitched = merger.merge(bundle, documents, null);

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

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument stitchedDocument = PDDocument.load(stitched);
        final int numberOfPagesInTableOfContents = 14;
        final int documentPages = doc1.getNumberOfPages() * numDocuments + numberOfPagesInTableOfContents;
        final int expectedPages = documentPages;

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

        int numFolders = 4;
        int numDocuments = 0;

        for (int i = 0; i < numFolders; i++) {
            BundleFolder folder = new BundleFolder();
            folder.setFolderName("Folder " + i);
            bundle.getFolders().add(folder);

            for (int j = 0; j < 10; j++) {
                BundleDocument bundleDocument = new BundleDocument();
                bundleDocument.setDocTitle("Bundle Doc " + numDocuments++);
                folder.getDocuments().add(bundleDocument);

                documents.put(bundleDocument, FILE_3);
            }
        }

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDDocument doc1 = PDDocument.load(FILE_3);
        PDDocument stitchedDocument = PDDocument.load(stitched);

        final int documentPages = numFolders + (doc1.getNumberOfPages() * numDocuments);
        final int numOfSubtitle = bundle.getSubtitles(bundle, documents);
        final int tocItems = numDocuments + (numFolders * 3) + numOfSubtitle;
        final int tocPages = (int) Math.ceil((double) tocItems / 30);
        final int expectedPages = documentPages + tocPages;
        final int actualPages = stitchedDocument.getNumberOfPages();

        doc1.close();
        stitchedDocument.close();

        assertEquals(expectedPages, actualPages);
    }

    @Test
    public void testAddSpaceAfterEndOfFolder() throws IOException {
        bundle.setHasTableOfContents(true);
        bundle.setHasFolderCoversheets(true);
        bundle.setDocuments(new ArrayList<>());
        documents = new HashMap<>();

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocTitle("Bundle Doc 1");
        bundleDocument.setSortIndex(1);
        bundle.getDocuments().add(bundleDocument);
        documents.put(bundleDocument, FILE_1);

        BundleFolder folder = new BundleFolder();
        folder.setFolderName("Folder 1");
        folder.setSortIndex(2);
        bundle.getFolders().add(folder);

        BundleDocument folderDocument = new BundleDocument();
        folderDocument.setDocTitle("Folder Doc 1");
        folder.getDocuments().add(folderDocument);
        documents.put(folderDocument, FILE_1);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocTitle("Bundle Doc 2");
        bundleDocument2.setSortIndex(3);
        bundle.getDocuments().add(bundleDocument2);
        documents.put(bundleDocument2, FILE_1);

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument stitchedDocument = PDDocument.load(stitched);

        final int documentPages = doc1.getNumberOfPages() * 3;
        final int additionalSpaceAfterEndOfFolder = 1;
        final int folderItems = 3;
        final int documentItems = 3;
        final int numOfSubtitle = bundle.getSubtitles(bundle, documents);
        final int tocItems = documentItems + folderItems + additionalSpaceAfterEndOfFolder + numOfSubtitle;
        final int tocPages = (int) Math.ceil((double) tocItems / 40);
        final int folderPages = 1;
        final int expectedPages = documentPages + tocPages + folderPages;
        final int actualPages = stitchedDocument.getNumberOfPages();

        doc1.close();
        stitchedDocument.close();

        assertEquals(expectedPages, actualPages);
    }


    @Test
    public void testPageNumbersPrintedOnCorrectPagesWithPaginationOptionSelected() throws IOException {
        bundle.setHasTableOfContents(true);
        bundle.setDocuments(new ArrayList<>());
        bundle.setPaginationStyle(PaginationStyle.topLeft);
        documents = new HashMap<>();

        final int numDocuments = 20;

        for (int i = 0; i < numDocuments; i++) {
            BundleDocument bundleDocument = new BundleDocument();
            bundleDocument.setDocTitle("Document");
            bundle.getDocuments().add(bundleDocument);

            documents.put(bundleDocument, FILE_1);
        }

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDDocument stitchedDocument = PDDocument.load(stitched);
        PDFTextStripper stripper = new PDFTextStripper();

        for (int pageNumber = 1; pageNumber <= stitchedDocument.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            String text = stripper.getText(stitchedDocument);
            String[] linesOfText = text.split(System.getProperty("line.separator"));
            if (pageNumber == 1 || pageNumber == 2) {
                assertFalse(linesOfText[linesOfText.length - 2].equals(String.valueOf(pageNumber)));

            } else {
                assertTrue(linesOfText[linesOfText.length - 1].equals(String.valueOf(pageNumber)));
            }
        }

        stitchedDocument.close();
    }

    @Test
    public void testPageNumbersPrintedOnCorrectPagesWithPaginationOptionAndCoverSheetsSelected() throws IOException {
        bundle.setHasTableOfContents(false);
        bundle.setHasCoversheets(true);
        bundle.setDocuments(new ArrayList<>());
        bundle.setPaginationStyle(PaginationStyle.topLeft);
        documents = new HashMap<>();

        final int numDocuments = 2;

        for (int i = 0; i < numDocuments; i++) {
            BundleDocument bundleDocument = new BundleDocument();
            bundleDocument.setDocTitle("Document");
            bundle.getDocuments().add(bundleDocument);

            documents.put(bundleDocument, FILE_1);
        }

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDDocument stitchedDocument = PDDocument.load(stitched);
        PDFTextStripper stripper = new PDFTextStripper();

        for (int pageNumber = 1; pageNumber <= stitchedDocument.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            String text = stripper.getText(stitchedDocument);
            String[] linesOfText = text.split(System.getProperty("line.separator"));
            if (Arrays.asList(1,3).contains(pageNumber)) {
                assertFalse(linesOfText[linesOfText.length - 1].equals(String.valueOf(pageNumber)));

            } else {
                assertTrue(linesOfText[linesOfText.length - 1].equals(String.valueOf(pageNumber)));
            }

        }

        stitchedDocument.close();
    }

    @Test
    public void testPageNumbersNotPrintedOnCorrectPagesWithPaginationOptionOff() throws IOException {
        bundle.setHasTableOfContents(true);
        bundle.setDocuments(new ArrayList<>());
        bundle.setPaginationStyle(PaginationStyle.off);
        documents = new HashMap<>();

        final int numDocuments = 20;

        for (int i = 0; i < numDocuments; i++) {
            BundleDocument bundleDocument = new BundleDocument();
            bundleDocument.setDocTitle("Document Title");
            bundle.getDocuments().add(bundleDocument);

            documents.put(bundleDocument, FILE_1);
        }

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDDocument stitchedDocument = PDDocument.load(stitched);
        PDFTextStripper stripper = new PDFTextStripper();

        for (int pageNumber = 1; pageNumber <= stitchedDocument.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            String text = stripper.getText(stitchedDocument);
            String[] linesOfText = text.split(System.getProperty("line.separator"));
            assertFalse(linesOfText[linesOfText.length - 2].equals(String.valueOf(pageNumber)));
        }

        stitchedDocument.close();
    }

    @Test
    public void throwNiceException() {
        bundle.setDocuments(new ArrayList<>());
        documents = new HashMap<>();

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocTitle("Bundle Doc 1");
        bundleDocument.setSortIndex(1);
        bundle.getDocuments().add(bundleDocument);

        File file = new File(ClassLoader.getSystemResource("TestExcelConversion.xlsx").getPath());
        documents.put(bundleDocument, file);

        PDFMerger merger = new PDFMerger();

        IOException exception = assertThrows(
            IOException.class,
            () -> merger.merge(bundle, documents, null)
        );

        assertEquals("Error processing Bundle Doc 1, TestExcelConversion.xlsx", exception.getMessage());
    }

    @Test
    public void mergeWithTableOfContentsWithNoBundleDescription() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(true);
        bundle.setDescription("");
        File merged = merger.merge(bundle, documents, null);
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
    public void mergeWithTableOfContentsWithUnevenDocumentsAndBundleDocs() throws IOException {
        HashMap<BundleDocument, File> documents2;

        Bundle newBundle = createFlatTestBundleWithAdditionalDoc();
        documents2 = new HashMap<>();
        documents2.put(newBundle.getDocuments().get(0), FILE_1);
        documents2.put(newBundle.getDocuments().get(1), FILE_3);
        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(newBundle, documents2, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_3);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    public void subtitleSameAsDocumentTitle() throws IOException {

        Bundle newBundle = createFlatTestBundleWithSameDocNameAsSubtitle();
        newBundle.setHasTableOfContents(true);
        HashMap<BundleDocument, File> newDocuments2 = new HashMap<>();
        newDocuments2.put(newBundle.getDocuments().get(0), FILE_1);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(newBundle, newDocuments2, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    public void specialCharactersInIndexPage() throws IOException {

        Bundle newBundle = createFlatTestBundleWithSpecialChars();
        newBundle.setHasTableOfContents(true);
        HashMap<BundleDocument, File> newDocuments2 = new HashMap<>();
        newDocuments2.put(newBundle.getDocuments().get(0), FILE_3);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(newBundle, newDocuments2, null);
        PDDocument mergedDocument = PDDocument.load(merged);

        Assert.assertEquals("ąćęłńóśźż", mergedDocument.getDocumentCatalog().getDocumentOutline().getFirstChild().getTitle());
    }

    @Test
    public void longDocTitle() throws IOException {

        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);

        String docTitle = Stream.generate(() -> "DocName ").limit(20).collect(Collectors.joining());
        bundle.getDocuments().get(0).setDocTitle(docTitle);

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDFTextStripper pdfStripper = new PDFTextStripper();
        String stitchedDocumentText = pdfStripper.getText(PDDocument.load(stitched));
        stitchedDocumentText = stitchedDocumentText.replace("\n", "");
        int stitchedDocTitleFrequency = countSubstrings(stitchedDocumentText, docTitle);

        assertEquals(stitchedDocTitleFrequency, 2);

    }
}
