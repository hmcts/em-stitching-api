package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class PDFOutlineServiceTest {

    @MockBean
    private DocumentTaskMapper documentTaskMapper;

    private static final String inputFileName = ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath();
    private static final File INPUT_FILE = new File(inputFileName);
    private Bundle bundle;

    @Before
    public void setup() {
        bundle = BundleTest.getTestBundle();
    }

    @Test
    public void createOutlineForDocument() throws IOException {
        PDDocument document = new PDDocument();
        document.addPage(new PDPage());
        PDFOutlineService pdfOutlineService = new PDFOutlineService(document, bundle);

        pdfOutlineService.createOutlines();
        pdfOutlineService.setParentDest(document);

        PDOutlineItem outlines = pdfOutlineService.getParentOutline();

        assertEquals(outlines.getTitle(), "Bundle");
        assertNotEquals(outlines.getDestination(), null);
        assertEquals(outlines.getFirstChild(), null);
     }

    @Test
    public void createOutlineWithTOCForDocument() {
        PDDocument document = new PDDocument();
        PDPage tocPage = new PDPage();
        document.addPage(tocPage);
        bundle.setHasTableOfContents(true);

        PDFOutlineService pdfOutlineService = new PDFOutlineService(document, bundle);
        pdfOutlineService.createOutlines();

        PDOutlineItem outlines = pdfOutlineService.getParentOutline();

        assertEquals(outlines.getTitle(), "Bundle");
        assertEquals(outlines.getFirstChild().getTitle(), "Index Page");
    }

    @Test
    public void copyDocumentOutline() throws IOException {
        PDDocument document = new PDDocument();
        document.addPage(new PDPage());
        document.addPage(new PDPage());
        PDFOutlineService pdfOutlineService = new PDFOutlineService(document, bundle);

        pdfOutlineService.createParentOutline();
        PDOutlineItem outlineItem = pdfOutlineService.createChildOutline(pdfOutlineService.getParentOutline(), 0, "Page 1");

        PDDocument newDoc = PDDocument.load(INPUT_FILE);
        PDDocumentOutline newDocOutline = newDoc.getDocumentCatalog().getDocumentOutline();
        pdfOutlineService.copyDocumentOutline(document, newDocOutline, outlineItem,1);

        PDOutlineItem outlines = pdfOutlineService.getParentOutline();

        assertEquals(outlines.getTitle(), "Bundle");
        assertEquals(outlines.getFirstChild().getTitle(), "Page 1");
        assertEquals(outlines.getFirstChild().getFirstChild().getTitle(), newDocOutline.getFirstChild().getTitle());
    }

    @Test
    public void getOutlinePage() throws IOException {
        PDFOutlineService pdfOutlineService = new PDFOutlineService();

        PDDocument newDoc = PDDocument.load(INPUT_FILE);
        PDDocumentOutline newDocOutline = newDoc.getDocumentCatalog().getDocumentOutline();

        int pageNumber = pdfOutlineService.getOutlinePage(newDocOutline.getFirstChild());

        assertEquals(pageNumber, 0);
    }

    @Test
    public void removeOutline() throws IOException {
        PDFOutlineService pdfOutlineService = new PDFOutlineService();

        PDDocument newDoc = PDDocument.load(INPUT_FILE);
        pdfOutlineService.removeAllOutlines(newDoc);
        PDDocumentOutline newDocOutline = newDoc.getDocumentCatalog().getDocumentOutline();

        assertEquals(newDocOutline, null);
    }

    @Test
    public void createDocumentCoversheet() throws IOException {
        PDDocument document = new PDDocument();
        document.addPage(new PDPage());

        PDFOutlineService pdfOutlineService = new PDFOutlineService();
        pdfOutlineService.createDocumentCoversheetOutline(document, "document coversheet");

        PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
        assertEquals(outline.getFirstChild().getTitle(), "document coversheet");
    }
}
