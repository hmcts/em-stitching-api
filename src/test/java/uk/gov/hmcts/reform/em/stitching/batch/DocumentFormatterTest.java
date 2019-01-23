package uk.gov.hmcts.reform.em.stitching.batch;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.Application;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.em.stitching.batch.DocumentFormatter.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class DocumentFormatterTest {

    private static final File INPUT_FILE = new File("INPUT_FILE.pdf");
    private static final File LONG_TITLE_FILE = new File("REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_LONG_NAME_FILE.pdf");
    private static PDDocument document;
    private static String documentName;

    @Before
    public void setup() throws IOException {
        document = PDDocument.load(INPUT_FILE);
        documentName = INPUT_FILE.getName();
    }

    @After
    public void teardown() throws IOException {
        document.close();
    }



    @Test
    public void addNewPageCountTest() throws Exception{
        int pageCountBefore = document.getNumberOfPages();
        File outputFile = addCoverSheetToDocument(INPUT_FILE);
        int pageCountAfter = PDDocument.load(outputFile).getNumberOfPages();
        assertEquals(pageCountBefore+ 1, pageCountAfter);
    }

    @Test
    public void addTextToDocumentTest() throws Exception {
        PDFTextStripper pdfStripper = new PDFTextStripper();

        String documentTextBefore = pdfStripper.getText(document);
        int nameCountBefore = StringUtils.countMatches(documentTextBefore, documentName);

        File outputFile = addCoverSheetToDocument(INPUT_FILE);
        String documentTextAfter = pdfStripper.getText(PDDocument.load(outputFile));
        int nameCountAfter = StringUtils.countMatches(documentTextAfter, documentName);

        assertEquals(nameCountBefore + 1, nameCountAfter);
    }

    @Test
    public void addTextToFirstPageTest() throws Exception {
        File outputFile = addCoverSheetToDocument(INPUT_FILE);

        PDFTextStripper pdfStripper = new PDFTextStripper();
        pdfStripper.setStartPage(0);
        pdfStripper.setEndPage(1);
        String documentTextAfter = pdfStripper.getText(PDDocument.load(outputFile));
        int nameCountOnFirstPage = StringUtils.countMatches(documentTextAfter, documentName);
        assertEquals(1, nameCountOnFirstPage);
    }

    @Test
    public void fileIsCreatedTest() throws Exception {
        File outputFile = addCoverSheetToDocument(INPUT_FILE);
        boolean documentExists = outputFile.exists();
        assert documentExists;
    }

    @Test
    public void demoCreateNormalFile() throws Exception {
        PDDocument standardDocument = PDDocument.load(addCoverSheetToDocument(INPUT_FILE));
        standardDocument.save(File.createTempFile("normal_length_title_file",".pdf"));
        // Open /tmp/normal_length_title-file....pdf to see how the title is wrapped
    }

    @Test
    public void demoWrappedTextFile() throws Exception {
        PDDocument longTitleDocument = PDDocument.load(addCoverSheetToDocument(LONG_TITLE_FILE));
        longTitleDocument.save(File.createTempFile("long_title_file",".pdf"));
        // Open /tmp/long_title_file....pdf to see how the title is wrapped
    }

}