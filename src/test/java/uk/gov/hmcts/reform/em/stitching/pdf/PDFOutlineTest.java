package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.util.EmptyStackException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
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
        pdfOutline.setRootOutlineItemDest(0);

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();

        assertEquals("Bundle", documentOutline.getFirstChild().getTitle());
        assertNotEquals(documentOutline.getFirstChild().getDestination(), null);
        assertNotEquals(documentOutline.getFirstChild(), null);
    }

    @Test
    public void createSubOutlinesForDocument() throws IOException {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document);
        document.addPage(new PDPage());
        document.addPage(new PDPage());

        pdfOutline.addBundleItem("Bundle");
        pdfOutline.addParentItem(0, "Folder Item 1");
        pdfOutline.addItem(0, "Sub Item 1");
        pdfOutline.closeParentItem();
        pdfOutline.addParentItem(1, "Folder Item 2");
        pdfOutline.addItem(1, "Sub Item 2");
        pdfOutline.closeParentItem();
        pdfOutline.setRootOutlineItemDest(0);

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem bundleOutline = documentOutline.getFirstChild();

        assertNotEquals(bundleOutline, null);
        assertEquals("Bundle", bundleOutline.getTitle());
        assertNotEquals(bundleOutline.getDestination(), null);
        assertEquals("Folder Item 1", bundleOutline.getFirstChild().getTitle());
        assertEquals("Sub Item 1", bundleOutline.getFirstChild().getFirstChild().getTitle());
        assertEquals("Folder Item 2", bundleOutline.getFirstChild().getNextSibling().getTitle());
        assertEquals("Sub Item 2", bundleOutline.getFirstChild().getNextSibling().getFirstChild().getTitle());
    }

    @Test
    public void mergeOutlineFromAnotherDocument() throws IOException {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document);
        document.addPage(new PDPage());

        pdfOutline.addBundleItem("Bundle");
        pdfOutline.addParentItem(0, "Folder Item 1");

        PDDocument newDoc = new PDDocument();
        newDoc.addPage(new PDPage());
        PDDocumentOutline newDocOutline = new PDDocumentOutline();
        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setTitle("New Doc Outline");
        outlineItem.setDestination(newDoc.getPage(0));
        newDocOutline.addFirst(outlineItem);
        newDoc.getDocumentCatalog().setDocumentOutline(newDocOutline);

        pdfOutline.mergeDocumentOutline(0, newDocOutline);
        pdfOutline.closeParentItem();
        pdfOutline.setRootOutlineItemDest(0);

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem bundleOutline = documentOutline.getFirstChild();

        assertNotEquals(bundleOutline, null);
        assertEquals("Bundle", bundleOutline.getTitle());
        assertNotEquals(bundleOutline.getDestination(), null);
        assertEquals("Folder Item 1", bundleOutline.getFirstChild().getTitle());
        assertEquals("New Doc Outline", bundleOutline.getFirstChild().getFirstChild().getTitle());
    }

    @Test(expected = EmptyStackException.class)
    public void addItemWithInvalidPage() {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document);
        pdfOutline.addItem(-2,"test Invalid Page");
    }
}
