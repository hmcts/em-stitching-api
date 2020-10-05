package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
public class PDFOutlineTest {
    private static final File FILE_1 = new File(
            ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath()
    );

    @Test
    public void createOutlineForDocument() throws IOException {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document);
        document.addPage(new PDPage());

        pdfOutline.addBundleItem("Bundle");
        pdfOutline.setRootOutlineItemDest();

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();

        assertEquals(documentOutline.getFirstChild().getTitle(), "Bundle");
        assertNotNull(documentOutline.getFirstChild().getDestination());
        assertNotNull(documentOutline.getFirstChild());
    }

    @Test
    public void createSubOutlinesForDocument() throws IOException {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document);
        document.addPage(new PDPage());
        document.addPage(new PDPage());

        pdfOutline.addBundleItem("Bundle");
        pdfOutline.addItem(0, "Folder Item 1");
        pdfOutline.addItem(1, "Folder Item 2");
        pdfOutline.setRootOutlineItemDest();

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem bundleOutline = documentOutline.getFirstChild();

        assertNotNull(bundleOutline);
        assertNotNull(bundleOutline.getDestination());
        assertEquals(bundleOutline.getTitle(), "Bundle");
        assertEquals(bundleOutline.getNextSibling().getTitle(), "Folder Item 1");
        assertEquals(bundleOutline.getNextSibling().getNextSibling().getTitle(), "Folder Item 2");
    }

    @Test
    public void createHeadingItem() throws IOException {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document);
        document.addPage(new PDPage());
        document.addPage(new PDPage());

        pdfOutline.addBundleItem("Bundle");
        PDOutlineItem item = pdfOutline.createHeadingItem(document.getPage(0), "heading item");
        document.getDocumentCatalog().getDocumentOutline().addFirst(item);

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem bundleOutline = documentOutline.getFirstChild();

        assertNotNull(bundleOutline);
        assertEquals("Bundle", bundleOutline.getNextSibling().getTitle());
        assertEquals("heading item", bundleOutline.getTitle());
        assertEquals(0, ((PDPageDestination) bundleOutline.getDestination()).retrievePageNumber());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void addItemWithInvalidPage() {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document);
        pdfOutline.addItem(-2,"test Invalid Page");
    }
}
