package uk.gov.hmcts.reform.em.stitching.pdf;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;

import java.io.File;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class PDFCoversheetServiceTest {

    private final PDFCoversheetService documentFormatter = new PDFCoversheetService();

    private static final String inputFileName = ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath();
    private static final File INPUT_FILE = new File(inputFileName);
    private static Pair<BundleDocument, File> pair;
    private static String documentName;

    @Before
    public void setup() {
        Bundle bundle = BundleTest.getTestBundle();
        pair = Pair.of(bundle.getDocuments().get(0), INPUT_FILE);
        documentName = bundle.getDocuments().get(0).getDocTitle();
    }

    @Test
    public void addNewPageCountTest() throws Exception {
        PDDocument doc1 = PDDocument.load(INPUT_FILE);
        int pageCountBefore = doc1.getNumberOfPages();
        doc1.close();

        Pair<BundleDocument, File> output = documentFormatter.addCoversheet(pair);
        PDDocument doc2 = PDDocument.load(output.getSecond());
        int pageCountAfter = doc2.getNumberOfPages();
        doc2.close();
        assertEquals(pageCountBefore + 1, pageCountAfter);
    }

    @Test
    public void addTextToDocumentTest() throws Exception {
        PDFTextStripper pdfStripper = new PDFTextStripper();
        PDDocument doc = PDDocument.load(pair.getSecond());
        String documentTextBefore = pdfStripper.getText(doc);
        doc.close();

        int nameCountBefore = StringUtils.countMatches(documentTextBefore, documentName);

        Pair<BundleDocument, File> output = documentFormatter.addCoversheet(pair);
        PDDocument doc2 = PDDocument.load(output.getSecond());
        String documentTextAfter = pdfStripper.getText(doc2);
        doc2.close();
        int nameCountAfter = StringUtils.countMatches(documentTextAfter, documentName);

        assertEquals(nameCountBefore + 1, nameCountAfter);
    }

    @Test
    public void addTextToFirstPageTest() throws Exception {
        Pair<BundleDocument, File> output = documentFormatter.addCoversheet(pair);

        PDFTextStripper pdfStripper = new PDFTextStripper();
        pdfStripper.setStartPage(0);
        pdfStripper.setEndPage(1);
        PDDocument doc = PDDocument.load(output.getSecond());
        String documentTextAfter = pdfStripper.getText(doc);
        doc.close();
        int nameCountOnFirstPage = StringUtils.countMatches(documentTextAfter, documentName);
        assertEquals(1, nameCountOnFirstPage);
    }

}
