package uk.gov.hmcts.reform.em.stitching.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleFolder;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.countSubstrings;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundle;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithAdditionalDoc;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithMultilineDocumentTitlesWithAdditionalDoc;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithMultilineTitles;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithSameDocNameAsSubtitle;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundleWithSpecialChars;

class PDFMergerTest {
    private static final File FILE_1 = new File(
            ClassLoader.getSystemResource("test-files/TEST_INPUT_FILE.pdf").getPath()
    );
    private static final File FILE_2 = new File(
            ClassLoader.getSystemResource("test-files/annotationTemplate.pdf").getPath()
           );
    private static final File FILE_3 = new File(
            ClassLoader.getSystemResource("test-files/Potential_Energy_PDF.pdf").getPath()
    );

    private Bundle bundle;
    private Bundle bundleWithMultilineDocumentTitles;
    private HashMap<BundleDocument, File> documents;
    private HashMap<BundleDocument, File> documentsWithMultilineTitles;
    private File coverPageFile;
    private JsonNode coverPageData;

    private static final String COVER_PAGE_TEMPLATE = "FL-FRM-GOR-ENG-12345";

    @BeforeEach
    void setup() {
        bundle = createFlatTestBundle();
        bundleWithMultilineDocumentTitles = createFlatTestBundleWithMultilineTitles();

        coverPageFile
            = new File(ClassLoader.getSystemResource("test-files/" + COVER_PAGE_TEMPLATE + ".pdf").getPath());

        coverPageData = JsonNodeFactory.instance.objectNode().put("caseNo", "12345");

        documents = new HashMap<>();
        documents.put(bundle.getDocuments().get(0), FILE_1);
        documents.put(bundle.getDocuments().get(1), FILE_2);

        documentsWithMultilineTitles = new HashMap<>();
        documentsWithMultilineTitles.put(bundleWithMultilineDocumentTitles.getDocuments().get(0), FILE_1);
        documentsWithMultilineTitles.put(bundleWithMultilineDocumentTitles.getDocuments().get(1), FILE_2);
    }

    @Test
    void mergeWithTableOfContents() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(true);
        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_2);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void mergeWithTableOfContentsWithMultilineTitles() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundleWithMultilineDocumentTitles.setHasTableOfContents(true);
        File merged = merger.merge(bundleWithMultilineDocumentTitles, documentsWithMultilineTitles, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_2);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void mergeWithTableOfContentsAndCoverPage() throws IOException {
        PDFMerger merger = new PDFMerger();

        bundle.setCoverpageTemplateData(coverPageData);
        bundle.setHasTableOfContents(true);
        bundle.setCoverpageTemplate(COVER_PAGE_TEMPLATE);
        File merged = merger.merge(bundle, documents, coverPageFile);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_2);

        final int numberOfPagesInTableOfContents = 1;
        final int numberOfPagesCoverPage = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages()
                + numberOfPagesInTableOfContents + numberOfPagesCoverPage;

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void mergeWithTableOfContentsWithMultilineTitlesAndCoverPage() throws IOException {

        bundle.setCoverpageTemplateData(coverPageData);
        bundle.setHasTableOfContents(true);
        bundle.setCoverpageTemplate(COVER_PAGE_TEMPLATE);
        bundleWithMultilineDocumentTitles.setHasTableOfContents(true);
        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(bundleWithMultilineDocumentTitles, documentsWithMultilineTitles, coverPageFile);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_2);

        final int numberOfPagesInTableOfContents = 1;
        final int numberOfPagesCoverPage = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages()
                + numberOfPagesInTableOfContents + numberOfPagesCoverPage;

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void mergeWithoutTableOfContents() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(false);

        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_2);

        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages();

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void mergeWithoutTableOfContentsAndCoverPage() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(false);

        File merged = merger.merge(bundle, documents, coverPageFile);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_2);

        final int numberOfPagesCoverPage = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesCoverPage;

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void noTableOfContentsBundleTitleFrequencyTest() throws IOException {
        PDFMerger merger = new PDFMerger();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        bundle.setHasTableOfContents(false);
        File stitched = merger.merge(bundle, documents, null);

        PDDocument stitchedDocument = Loader.loadPDF(stitched);
        String stitchedDocumentText = pdfStripper.getText(stitchedDocument);
        stitchedDocument.close();
        int stitchedDocBundleTitleFrequency = countSubstrings(stitchedDocumentText, bundle.getBundleTitle());

        PDDocument firstDocument = Loader.loadPDF(FILE_1);
        String firstFileDocumentText = pdfStripper.getText(firstDocument);
        firstDocument.close();

        PDDocument secondDocument = Loader.loadPDF(FILE_2);
        String secondFileDocumentText = pdfStripper.getText(secondDocument);
        secondDocument.close();

        int firstDocBundleTitleFrequency = countSubstrings(firstFileDocumentText, bundle.getBundleTitle());
        int secondDocBundleTitleFrequency = countSubstrings(secondFileDocumentText, bundle.getBundleTitle());
        int expectedBundleTitleFrequency = firstDocBundleTitleFrequency + secondDocBundleTitleFrequency;
        assertEquals(stitchedDocBundleTitleFrequency, expectedBundleTitleFrequency);
    }

    @Test
    void testMultipleTableOfContentsPages() throws IOException {
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

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument stitchedDocument = Loader.loadPDF(stitched);
        final int numberOfPagesInTableOfContents = 11;
        final int documentPages = doc1.getNumberOfPages() * numDocuments + numberOfPagesInTableOfContents;
        final int expectedPages = documentPages;

        final int actualPages = stitchedDocument.getNumberOfPages();

        doc1.close();
        stitchedDocument.close();

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testMultipleTableOfContentsPagesAndFolders() throws IOException {
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

        PDDocument doc1 = Loader.loadPDF(FILE_3);
        PDDocument stitchedDocument = Loader.loadPDF(stitched);

        final int documentPages = numFolders + (doc1.getNumberOfPages() * numDocuments);
        final int numOfSubtitle = bundle.getNumberOfSubtitles(bundle, documents);
        final int tocItems = numDocuments + (numFolders * 3) + numOfSubtitle;
        final int tocPages = (int) Math.ceil((double) tocItems / 38);
        final int expectedPages = documentPages + tocPages;
        final int actualPages = stitchedDocument.getNumberOfPages();

        doc1.close();
        stitchedDocument.close();

        assertEquals(expectedPages, actualPages);
    }

    @Test
    void testAddSpaceAfterEndOfFolder() throws IOException {
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

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument stitchedDocument = Loader.loadPDF(stitched);

        final int documentPages = doc1.getNumberOfPages() * 3;
        final int additionalSpaceAfterEndOfFolder = 1;
        final int folderItems = 3;
        final int documentItems = 3;
        final int numOfSubtitle = bundle.getNumberOfSubtitles(bundle, documents);
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
    void testPageNumbersPrintedOnCorrectPagesWithPaginationOptionSelected() throws IOException {
        bundle.setHasTableOfContents(true);
        bundle.setDocuments(new ArrayList<>());
        bundle.setPaginationStyle(PaginationStyle.topLeft);
        documents = new HashMap<>();

        final int numDocuments = 20;

        for (int i = 0; i < numDocuments; i++) {
            BundleDocument bundleDocument = new BundleDocument();
            bundleDocument.setDocTitle("Document");
            bundleDocument.setId((long)i);
            bundle.getDocuments().add(bundleDocument);

            documents.put(bundleDocument, FILE_1);
        }

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDDocument stitchedDocument = Loader.loadPDF(stitched);
        PDFTextStripper stripper = new PDFTextStripper();

        for (int pageNumber = 1; pageNumber <= stitchedDocument.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            String text = stripper.getText(stitchedDocument);
            Pattern pattern = Pattern.compile("\\b" + pageNumber + "\\b");
            assertTrue(
                pattern.matcher(text).find(),
                "Page " + pageNumber + " should be numbered."
            );
        }

        stitchedDocument.close();
    }

    @Test
    void testPageNumbersPrintedOnCorrectPagesWithPaginationOptionAndCoverSheetsSelected() throws IOException {
        bundle.setHasTableOfContents(false);
        bundle.setHasCoversheets(true);
        bundle.setDocuments(new ArrayList<>());
        bundle.setPaginationStyle(PaginationStyle.topLeft);
        documents = new HashMap<>();

        final int numDocuments = 2;

        for (int i = 0; i < numDocuments; i++) {
            BundleDocument bundleDocument = new BundleDocument();
            bundleDocument.setDocTitle("Document");
            bundleDocument.setId((long)i);
            bundle.getDocuments().add(bundleDocument);

            documents.put(bundleDocument, FILE_1);
        }

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDDocument stitchedDocument = Loader.loadPDF(stitched);
        PDFTextStripper stripper = new PDFTextStripper();

        for (int pageNumber = 1; pageNumber <= stitchedDocument.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            String text = stripper.getText(stitchedDocument);
            Pattern pattern = Pattern.compile("\\b" + pageNumber + "\\b");
            assertTrue(
                pattern.matcher(text).find(),
                "Page " + pageNumber + " (a coversheet or document page) should be numbered."
            );

        }

        stitchedDocument.close();
    }

    @Test
    void testPageNumbersAddedToTableOfContents() throws IOException {
        bundle.setHasTableOfContents(true);
        bundle.setPaginationStyle(PaginationStyle.bottomRight);
        PDFMerger merger = new PDFMerger();
        File stitched = null;
        try {
            stitched = merger.merge(bundle, documents, null);
            try (PDDocument stitchedDocument = Loader.loadPDF(stitched)) {
                PDFTextStripper stripper = new PDFTextStripper();

                stripper.setStartPage(1);
                stripper.setEndPage(1);
                String tocPageText = stripper.getText(stitchedDocument);
                assertTrue(
                    Pattern.compile("\\b1\\b").matcher(tocPageText).find(),
                    "Table of Contents page should be numbered '1'"
                );

                stripper.setStartPage(2);
                stripper.setEndPage(2);
                String docPageText = stripper.getText(stitchedDocument);
                assertTrue(
                    Pattern.compile("\\b2\\b").matcher(docPageText).find(),
                    "Document page should be numbered correctly '2' after TOC"
                );
            }
        } finally {
            if (stitched != null) {
                Files.deleteIfExists(stitched.toPath());
            }
        }
    }

    @Test
    void testPageNumbersAddedToCoversheets() throws IOException {
        bundle.setHasCoversheets(true);
        bundle.setHasTableOfContents(false);
        bundle.setPaginationStyle(PaginationStyle.bottomRight);

        PDFMerger merger = new PDFMerger();
        File stitched = null;

        try {
            stitched = merger.merge(bundle, documents, null);
            try (PDDocument stitchedDocument = Loader.loadPDF(stitched);
                PDDocument doc1 = Loader.loadPDF(FILE_1)) {
                PDFTextStripper stripper = new PDFTextStripper();
                int doc1Pages = doc1.getNumberOfPages();

                stripper.setStartPage(1);
                stripper.setEndPage(1);
                String coversheet1Text = stripper.getText(stitchedDocument);
                assertTrue(
                    Pattern.compile("\\b1\\b").matcher(coversheet1Text).find(),
                    "Coversheet 1 should be numbered '1'"
                );

                stripper.setStartPage(2);
                stripper.setEndPage(2);
                String doc1Page1Text = stripper.getText(stitchedDocument);
                assertTrue(
                    Pattern.compile("\\b2\\b").matcher(doc1Page1Text).find(),
                    "First page of Document 1 should be numbered '2'"
                );

                int coversheet2PageNumber = 1 + doc1Pages + 1;
                stripper.setStartPage(coversheet2PageNumber);
                stripper.setEndPage(coversheet2PageNumber);
                String coversheet2Text = stripper.getText(stitchedDocument);
                Pattern pattern = Pattern.compile("\\b" + coversheet2PageNumber + "\\b");
                assertTrue(
                    pattern.matcher(coversheet2Text).find(),
                    "Coversheet 2 should be numbered correctly"
                );
            }
        } finally {
            if (stitched != null) {
                Files.deleteIfExists(stitched.toPath());
            }
        }
    }

    @Test
    void testPageNumbersNotPrintedOnCorrectPagesWithPaginationOptionOff() throws IOException {
        bundle.setHasTableOfContents(true);
        bundle.setDocuments(new ArrayList<>());
        bundle.setPaginationStyle(PaginationStyle.off);
        documents = new HashMap<>();

        final int numDocuments = 20;

        for (int i = 0; i < numDocuments; i++) {
            BundleDocument bundleDocument = new BundleDocument();
            bundleDocument.setDocTitle("Document Title");
            bundle.getDocuments().add(bundleDocument);
            bundleDocument.setId((long)i);
            documents.put(bundleDocument, FILE_1);
        }

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDDocument stitchedDocument = Loader.loadPDF(stitched);
        PDFTextStripper stripper = new PDFTextStripper();

        for (int pageNumber = 1; pageNumber <= stitchedDocument.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            String text = stripper.getText(stitchedDocument);
            String[] linesOfText = text.split(System.lineSeparator());
            assertNotEquals(linesOfText[linesOfText.length - 2], String.valueOf(pageNumber));
        }

        stitchedDocument.close();
    }

    @Test
    void throwNiceException() {
        bundle.setDocuments(new ArrayList<>());
        documents = new HashMap<>();

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocTitle("Bundle Doc 1");
        bundleDocument.setSortIndex(1);
        bundle.getDocuments().add(bundleDocument);

        File file = new File(ClassLoader.getSystemResource("test-files/TestExcelConversion.xlsx").getPath());
        documents.put(bundleDocument, file);

        PDFMerger merger = new PDFMerger();

        IOException exception = assertThrows(
            IOException.class,
            () -> merger.merge(bundle, documents, null)
        );

        assertEquals(
                "Error processing, document title: Bundle Doc 1, file name: TestExcelConversion.xlsx",
                exception.getMessage()
        );
    }

    @Test
    void mergeWithTableOfContentsWithNoBundleDescription() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundle.setHasTableOfContents(true);
        bundle.setDescription("");
        File merged = merger.merge(bundle, documents, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_2);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void mergeWithTableOfContentsWithMultilineTitlesWithNoBundleDescription() throws IOException {
        PDFMerger merger = new PDFMerger();
        bundleWithMultilineDocumentTitles.setHasTableOfContents(true);
        bundleWithMultilineDocumentTitles.setDescription("");
        File merged = merger.merge(bundleWithMultilineDocumentTitles, documentsWithMultilineTitles, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_2);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void mergeWithTableOfContentsWithUnevenDocumentsAndBundleDocs() throws IOException {
        HashMap<BundleDocument, File> documents2;

        Bundle newBundle = createFlatTestBundleWithAdditionalDoc();
        documents2 = new HashMap<>();
        documents2.put(newBundle.getDocuments().get(0), FILE_1);
        documents2.put(newBundle.getDocuments().get(1), FILE_3);
        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(newBundle, documents2, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_3);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void mergeWithTableOfContentsbundleWithMultilineDocumentTitlesWithUnevenDocumentsAndBundleDocs()
            throws IOException {
        HashMap<BundleDocument, File> documents2;

        Bundle newBundle = createFlatTestBundleWithMultilineDocumentTitlesWithAdditionalDoc();
        documents2 = new HashMap<>();
        documents2.put(newBundle.getDocuments().get(0), FILE_1);
        documents2.put(newBundle.getDocuments().get(1), FILE_3);
        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(newBundle, documents2, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);
        PDDocument doc2 = Loader.loadPDF(FILE_3);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        doc2.close();
        mergedDocument.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void subtitleSameAsDocumentTitle() throws IOException {

        Bundle newBundle = createFlatTestBundleWithSameDocNameAsSubtitle();
        newBundle.setHasTableOfContents(true);
        HashMap<BundleDocument, File> newDocuments2 = new HashMap<>();
        newDocuments2.put(newBundle.getDocuments().get(0), FILE_1);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(newBundle, newDocuments2, null);
        PDDocument mergedDocument = Loader.loadPDF(merged);

        PDDocument doc1 = Loader.loadPDF(FILE_1);

        final int numberOfPagesInTableOfContents = 1;
        final int expectedPages = doc1.getNumberOfPages() + numberOfPagesInTableOfContents;

        doc1.close();
        mergedDocument.close();
        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

    @Test
    void specialCharactersInIndexPage() throws IOException {

        Bundle newBundle = createFlatTestBundleWithSpecialChars();
        newBundle.setHasTableOfContents(true);
        HashMap<BundleDocument, File> newDocuments2 = new HashMap<>();
        newDocuments2.put(newBundle.getDocuments().get(0), FILE_3);

        PDFMerger merger = new PDFMerger();
        File merged = merger.merge(newBundle, newDocuments2, null);
        try (PDDocument mergedDocument = Loader.loadPDF(merged)) {
            assertEquals("ąćęłńóśźż",
                    mergedDocument.getDocumentCatalog().getDocumentOutline().getFirstChild().getTitle());
        }
    }

    @Test
    void longDocTitle() throws IOException {

        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);

        String docTitle = Stream.generate(() -> "DocName ").limit(20).collect(Collectors.joining());
        bundle.getDocuments().get(0).setDocTitle(docTitle);

        PDFMerger merger = new PDFMerger();
        File stitched = merger.merge(bundle, documents, null);

        PDFTextStripper pdfStripper = new PDFTextStripper();
        PDDocument pdDocument = Loader.loadPDF(stitched);
        String stitchedDocumentText = pdfStripper.getText(pdDocument);
        stitchedDocumentText = stitchedDocumentText.replace("\n", " ");
        int stitchedDocTitleFrequency = countSubstrings(stitchedDocumentText, docTitle.trim());

        pdDocument.close();

        assertEquals(2, stitchedDocTitleFrequency);
    }

    @Test
    void exceptionOnDocCloseContinues() throws IOException {
        Bundle testBundle = new Bundle();
        testBundle.setBundleTitle("Test Bundle For Finally Block");
        testBundle.setFileName("test-finally.pdf");
        testBundle.setHasCoversheets(false);
        testBundle.setHasTableOfContents(false);

        BundleDocument doc1Item = new BundleDocument();
        doc1Item.setDocTitle("NormalDoc");
        doc1Item.setId(1L);
        testBundle.getDocuments().add(doc1Item);

        BundleDocument doc2ItemFaultyClose = new BundleDocument();
        doc2ItemFaultyClose.setDocTitle("FaultyCloseDoc");
        doc2ItemFaultyClose.setId(2L);
        testBundle.getDocuments().add(doc2ItemFaultyClose);

        HashMap<BundleDocument, File> testDocuments = new HashMap<>();
        File normalFile = FILE_1;
        File faultyFile = FILE_2;
        testDocuments.put(doc1Item, normalFile);
        testDocuments.put(doc2ItemFaultyClose, faultyFile);

        PDDocument realNormalPdfDoc = null;
        PDDocument spiedNormalPdfDoc;
        PDDocument realFaultyPdfDoc = null;
        PDDocument spiedFaultyPdfDoc;
        File mergedFileResult = null;

        try {
            realNormalPdfDoc = Loader.loadPDF(normalFile);
            spiedNormalPdfDoc = spy(realNormalPdfDoc);
            realFaultyPdfDoc = Loader.loadPDF(faultyFile);
            spiedFaultyPdfDoc = spy(realFaultyPdfDoc);
            doThrow(new IOException("Simulated error closing faultyDoc")).when(spiedFaultyPdfDoc).close();

            try (MockedStatic<Loader> mockedLoader = Mockito.mockStatic(Loader.class)) {
                mockedLoader.when(() -> Loader.loadPDF(normalFile)).thenReturn(spiedNormalPdfDoc);
                mockedLoader.when(() -> Loader.loadPDF(faultyFile)).thenReturn(spiedFaultyPdfDoc);

                PDFMerger merger = new PDFMerger();
                mergedFileResult = merger.merge(testBundle, testDocuments, null);
                assertNotNull(mergedFileResult, "Merged file should be created.");

                verify(spiedNormalPdfDoc, times(1)).close();
                verify(spiedFaultyPdfDoc, times(1)).close();
            }
        } finally {
            if (Objects.nonNull(realNormalPdfDoc)) {
                realNormalPdfDoc.close();
            }
            if (Objects.nonNull(realFaultyPdfDoc)) {
                realFaultyPdfDoc.close();
            }
            if (Objects.nonNull(mergedFileResult)) {
                Files.deleteIfExists(mergedFileResult.toPath());
            }
        }
    }

    @Test
    void retriesAppendOnIndexOutOfBoundsException() throws IOException {
        Bundle testBundle = new Bundle();
        testBundle.setBundleTitle("Test Bundle Append Retry");
        testBundle.setFileName("test-append-retry.pdf");
        testBundle.setHasCoversheets(false);
        testBundle.setHasTableOfContents(false);

        BundleDocument docItem = new BundleDocument();
        docItem.setDocTitle("DocToAppend");
        docItem.setId(1L);
        testBundle.getDocuments().add(docItem);

        File realFileForDocItem = FILE_1;
        HashMap<BundleDocument, File> testDocuments = new HashMap<>();
        testDocuments.put(docItem, realFileForDocItem);

        PDDocument spiedNewDoc;
        PDDocumentCatalog spiedCatalog;
        File mergedFileResult = null;
        try (PDDocument realNewDoc = Loader.loadPDF(realFileForDocItem)) {
            assertTrue(realNewDoc.getNumberOfPages() > 0, "FILE_1 must have pages for this test to be valid.");
            spiedNewDoc = spy(realNewDoc);
            spiedCatalog = spy(spiedNewDoc.getDocumentCatalog());
            when(spiedNewDoc.getDocumentCatalog()).thenReturn(spiedCatalog);

            PDDocument finalSpiedNewDoc = spiedNewDoc;
            try (MockedStatic<Loader> mockedLoader = Mockito.mockStatic(Loader.class);
                 MockedConstruction<PDFMergerUtility> mockedMergerUtilityConstruction = Mockito.mockConstruction(
                     PDFMergerUtility.class,
                     (constructedMockMergerUtility, context) -> Mockito.doAnswer(invocation -> {
                         throw new IndexOutOfBoundsException("Simulated first append failure");
                     }).doAnswer(invocation -> {
                         PDDocument mainDocumentArg = invocation.getArgument(0);
                         PDDocument newDocArg = invocation.getArgument(1);
                         for (PDPage page : newDocArg.getPages()) {
                             mainDocumentArg.addPage(page);
                         }
                         return null;
                     }).when(constructedMockMergerUtility).appendDocument(any(PDDocument.class), eq(finalSpiedNewDoc))
                 )
            ) {
                mockedLoader.when(() -> Loader.loadPDF(realFileForDocItem)).thenReturn(spiedNewDoc);
                PDFMerger merger = new PDFMerger();
                mergedFileResult = merger.merge(testBundle, testDocuments, null);
                assertNotNull(mergedFileResult);

                List<PDFMergerUtility> constructedMergerUtilities = mockedMergerUtilityConstruction.constructed();
                assertEquals(1, constructedMergerUtilities.size());
                PDFMergerUtility usedMergerUtilityMock = constructedMergerUtilities.getFirst();

                verify(usedMergerUtilityMock, times(2))
                    .appendDocument(any(PDDocument.class), eq(spiedNewDoc));
                verify(spiedCatalog, times(1))
                    .setStructureTreeRoot(any(PDStructureTreeRoot.class));
            }
        } finally {
            if (Objects.nonNull(mergedFileResult)) {
                Files.deleteIfExists(mergedFileResult.toPath());
            }
        }
    }
}