package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
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
    public void setup() throws IOException {
        Bundle bundle = BundleTest.getTestBundle();
        document1 = Pair.of(bundle.getDocuments().get(0), FILE_1);
        document2 = Pair.of(bundle.getDocuments().get(0), FILE_2);
        documents = new ArrayList<>();

        documents.add(document1);
        documents.add(document2);
    }

    @Test
    public void merge() throws IOException {
        PDFMerger merger = new PDFMerger();
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");

        File merged = merger.merge(bundle, documents);
        PDDocument mergedDocument = PDDocument.load(merged);

        PDDocument doc1 = PDDocument.load(FILE_1);
        PDDocument doc2 = PDDocument.load(FILE_2);
        int expectedPages = doc1.getNumberOfPages() + doc2.getNumberOfPages() + 1;
        doc1.close();
        doc2.close();

        assertEquals(expectedPages, mergedDocument.getNumberOfPages());
    }

}
