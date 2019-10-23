package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.io.IOException;
import java.util.Stack;

public class PDFOutline {
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

    public PDOutlineItem addItem(int page, String title) {
        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setDestination(document.getPage(page));
        outlineItem.setTitle(title);
        parentOutlineItems.peek().addLast(outlineItem);
        parentOutlineItems.peek().openNode();

        return outlineItem;
    }

    public void addParentItem(int page, String title) {
        PDOutlineItem lastItem = addItem(page, title);
        parentOutlineItems.push(lastItem);
    }

    public void closeParentItem() {
        if (parentOutlineItems.size() > 1) {
            parentOutlineItems.pop();
        }
    }

    public void mergeDocumentOutline(int currentPageNumber, PDDocumentOutline originalOutline) throws IOException {
        PDOutlineItem item = originalOutline.getFirstChild();
        while (item != null) {
            int page = getOutlinePage(item) + currentPageNumber;
            PDOutlineItem outlineItem = addItem(page, item.getTitle());
            PDOutlineItem child = item.getFirstChild();
            if (child != null) {
                parentOutlineItems.push(outlineItem);
                while (child != null) {
                    page = getOutlinePage(child) + currentPageNumber;
                    addItem(page, child.getTitle());
                    child = child.getNextSibling();
                }
                parentOutlineItems.pop();
            }
            item = item.getNextSibling();
        }
    }


    public int getOutlinePage(PDOutlineItem outlineItem) throws IOException {
        PDPageDestination dest = (PDPageDestination) outlineItem.getDestination();
        return Math.max(dest.retrievePageNumber(), 0);
    }
}
