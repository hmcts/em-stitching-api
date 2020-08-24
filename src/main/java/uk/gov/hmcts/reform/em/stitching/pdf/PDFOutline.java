package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Stack;

public class PDFOutline {

    private final Logger log = LoggerFactory.getLogger(PDFOutline.class);

    private final PDDocument document;
    private Stack<PDOutlineItem> parentOutlineItems = new Stack<>();

    public PDFOutline(PDDocument document) {
        this.document = document;
    }

    public void addBundleItem(String title) {
        PDDocumentOutline parentOutline = new PDDocumentOutline();
        document.getDocumentCatalog().setDocumentOutline(parentOutline);
        parentOutline.openNode();

        PDOutlineItem parentOutlineItem = new PDOutlineItem();
        parentOutlineItem.setTitle(title);
        parentOutline.addLast(parentOutlineItem);
        parentOutlineItems.push(parentOutlineItem);
    }

    public void setRootOutlineItemDest(int page) {
        parentOutlineItems.firstElement().setDestination(document.getPage(page));
    }

    public void addItem(int page, String title) {
        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setDestination(document.getPage(page));
        outlineItem.setTitle(title);
        outlineItem.setBold(true);
        document.getDocumentCatalog().getDocumentOutline().addLast(outlineItem);
    }

    public void closeParentItem() {
        if (parentOutlineItems.size() > 1) {
            parentOutlineItems.pop();
        }
    }
}
