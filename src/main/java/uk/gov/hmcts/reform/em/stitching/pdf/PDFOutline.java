package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFOutline {

    private final Logger log = LoggerFactory.getLogger(PDFOutline.class);

    private final PDDocument document;

    public PDFOutline(PDDocument document) {
        this.document = document;
    }

    public void addBundleItem(String title) {
        PDDocumentOutline bundleOutline = new PDDocumentOutline();
        document.getDocumentCatalog().setDocumentOutline(bundleOutline);
        bundleOutline.openNode();

        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setTitle(title);
        bundleOutline.addLast(outlineItem);
    }

    public void addItem(int page, String title) {
        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setDestination(document.getPage(page));
        outlineItem.setTitle(title);
        outlineItem.setBold(true);
        document.getDocumentCatalog().getDocumentOutline().addLast(outlineItem);
    }
}
