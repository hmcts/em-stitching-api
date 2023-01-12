package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

public class PDFOutline {

    private final Logger log = LoggerFactory.getLogger(PDFOutline.class);

    private final PDDocument document;
    private final TreeNode<SortableBundleItem> outlineTree;
    private PDOutlineItem rootOutline;

    public PDFOutline(PDDocument document, TreeNode<SortableBundleItem> outlineTree) {
        this.document = document;
        this.outlineTree = outlineTree;
    }

    public void addBundleItem(SortableBundleItem item) {
        PDDocumentOutline bundleOutline = new PDDocumentOutline();
        document.getDocumentCatalog().setDocumentOutline(bundleOutline);
        bundleOutline.openNode();

        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setTitle(item.getTitle());

        outlineItem.getCOSObject().setItem(createItemKey(item), COSNull.NULL);

        bundleOutline.addLast(outlineItem);
        rootOutline = outlineItem;
    }

    public void setRootOutlineItemDest() {
        PDOutlineItem rootElement = document.getDocumentCatalog().getDocumentOutline().getFirstChild();
        if (rootElement != null) {
            rootElement.setDestination(document.getPage(0));
        }
    }

    private void addOutline(PDOutlineItem outlineItem, String key) {

        var node
                = outlineTree.findTreeNode(createBundleItemComparable(key), outlineTree);
        if (node == null) {
            this.rootOutline.addLast(outlineItem);
        } else {
            PDOutlineItem parentFound = findPdOutlineItem(document.getDocumentCatalog().getDocumentOutline().getFirstChild(), createItemKey(node.getParentData()));
            if (parentFound == null) {
                document.getDocumentCatalog().getDocumentOutline().addLast(outlineItem);
            } else {
                parentFound.addLast(outlineItem);
            }
        }
    }

    public void addItem(SortableBundleItem item, int page) {
        log.info("AddItem title {}", item.getTitle());
        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setDestination(document.getPage(page));
        outlineItem.setTitle(trimOutlineTitle(item.getTitle()));
        outlineItem.setBold(true);
        var key = createItemKey(item);
        outlineItem.getCOSObject().setItem(key, COSNull.NULL);
        addOutline(outlineItem, key);
    }

    public void addItem(int page, String title) {
        log.info("AddItem title {}", title);
        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setDestination(document.getPage(page));
        outlineItem.setTitle(title);
        outlineItem.setBold(true);
        addOutline(outlineItem, title);
    }

    public PDOutlineItem findPdOutlineItem(PDOutlineItem pdOutlineItem, String key) {

        if (pdOutlineItem.getCOSObject().getItem(key) != null) {
            return pdOutlineItem;
        }

        for (var child : pdOutlineItem.children()) {
            log.info("findPdOutlineItem title====> {}", child.getTitle());
            var find = findPdOutlineItem(child, key);
            if (find != null) {
                return find;
            }
        }

        return null;
    }

    private Comparable<SortableBundleItem> createBundleItemComparable(String key) {
        return c -> key.equalsIgnoreCase(createItemKey(c)) ? 0 : 1;
    }

    private String createItemKey(SortableBundleItem item) {
        return item.getId() + item.getTitle();
    }

    private String trimOutlineTitle(String title) {
        if (title.length() > 400) {
            return title.substring(0, 399) + "...";
        }
        return title;
    }
}
