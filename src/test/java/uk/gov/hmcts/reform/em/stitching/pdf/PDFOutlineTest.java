package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
public class PDFOutlineTest {
    private static final File FILE_1 = new File(
            ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath()
    );

    private final BundleDocument item = new BundleDocument();
    private TreeNode<SortableBundleItem> outlineTree = null;

    @Test
    public void createOutlineForDocument() throws IOException {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document, outlineTree);
        document.addPage(new PDPage());
        item.setId(1l);
        item.setDocTitle("Bundle");
        pdfOutline.addBundleItem(item);
        pdfOutline.setRootOutlineItemDest();

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();

        assertEquals(documentOutline.getFirstChild().getTitle(), "Bundle");
        assertNotNull(documentOutline.getFirstChild().getDestination());
        assertNotNull(documentOutline.getFirstChild());
    }

    @Test
    public void createSubOutlinesForDocument() throws IOException {
        PDDocument document = new PDDocument();
        outlineTree = new TreeNode(item);
        PDFOutline pdfOutline = new PDFOutline(document, outlineTree);
        document.addPage(new PDPage());
        document.addPage(new PDPage());
        item.setId(2l);
        item.setDocTitle("Bundle");
        pdfOutline.addBundleItem(item);
        pdfOutline.addItem(0, "Folder Item 1");
        pdfOutline.addItem(1, "Folder Item 2");
        pdfOutline.setRootOutlineItemDest();

        PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem bundleOutline = documentOutline.getFirstChild();

        assertNotNull(bundleOutline);
        assertNotNull(bundleOutline.getDestination());
        assertEquals("Bundle", bundleOutline.getTitle());
        assertEquals("Folder Item 1", bundleOutline.getFirstChild().getTitle());
        assertEquals("Folder Item 2", bundleOutline.getLastChild().getTitle());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void addItemWithInvalidPage() {
        PDDocument document = new PDDocument();
        PDFOutline pdfOutline = new PDFOutline(document, outlineTree);
        pdfOutline.addItem(-2,"test Invalid Page");
    }
}
