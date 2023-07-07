package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.multipdf.PDFCloneUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

import java.io.IOException;

public class PDFOutline {

    private final Logger log = LoggerFactory.getLogger(PDFOutline.class);

    private final PDDocument document;
    private final TreeNode<SortableBundleItem> outlineTree;
    private PDOutlineItem rootOutline;
    private PDFCloneUtility cloner;

    public PDFOutline(PDDocument document, TreeNode<SortableBundleItem> outlineTree) {
        this.document = document;
        this.outlineTree = outlineTree;
        this.cloner = new PDFCloneUtility(document);
    }

    public void addBundleItem(SortableBundleItem item) {
        log.info("AddBundleItem AddItem title {}", item.getTitle());

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
            PDPageDestination newDestination = new PDPageFitDestination();
            newDestination.setPage(document.getPage(0));
            rootElement.setDestination(newDestination);
        }
    }

    private void addOutline(PDOutlineItem outlineItem, String key) {

        var node
                = outlineTree.findTreeNode(createBundleItemComparable(key), outlineTree);
        if (node == null) {
            this.rootOutline.addLast(outlineItem);
        } else {
            PDOutlineItem parentFound =
                findPdOutlineItem(
                    document.getDocumentCatalog().getDocumentOutline().getFirstChild(),
                    createItemKey(node.getParentData())
                );
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
        PDPageDestination newDestination = new PDPageFitDestination();
        newDestination.setPage(document.getPage(page));
        outlineItem.setDestination(newDestination);
        outlineItem.setTitle(trimOutlineTitle(item.getTitle()));
        outlineItem.setBold(true);
        var key = createItemKey(item);
        outlineItem.getCOSObject().setItem(key, COSNull.NULL);
        addOutline(outlineItem, key);
    }

    public void addItem(int page, String title) {
        log.info("AddItem title {}", title);
        PDOutlineItem outlineItem = new PDOutlineItem();
        PDPageDestination newDestination = new PDPageFitDestination();
        newDestination.setPage(document.getPage(page));
        outlineItem.setDestination(newDestination);
        outlineItem.setTitle(title);
        outlineItem.setBold(true);
        addOutline(outlineItem, title);
    }

    public PDOutlineItem findPdOutlineItem(PDOutlineItem pdOutlineItem, String key) {

        if (pdOutlineItem.getCOSObject().getItem(key) != null) {
            return pdOutlineItem;
        }

        for (var child : pdOutlineItem.children()) {
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

    public void copyOutline(PDDocumentOutline srcOutline, PDDocumentCatalog documentCatalog, String key, int currentPageNumber) throws IOException {
        PDOutlineItem destLastOutlineItem;
        var node =
            outlineTree.findTreeNode(createBundleItemComparable(key), outlineTree);
        if (node == null) {
            destLastOutlineItem = this.rootOutline.getLastChild();
        } else {
            PDOutlineItem parentFound =
                findPdOutlineItem(
                    document.getDocumentCatalog().getDocumentOutline().getFirstChild(),
                    createItemKey(node.getParentData())
                );
            if (parentFound == null) {
                destLastOutlineItem = document.getDocumentCatalog().getDocumentOutline().getLastChild();
            } else {
                destLastOutlineItem = parentFound;
            }
        }

        //document coversheet outline or doc outline should already be there.
        destLastOutlineItem = findPdOutlineItem(destLastOutlineItem, key);
        for (PDOutlineItem item : srcOutline.children()) {
            // get each child, clone its dictionary, remove siblings info,
            // append outline item created from there
            COSDictionary clonedDict = (COSDictionary) cloner.cloneForNewDocument(item);
            clonedDict.removeItem(COSName.PREV);
            clonedDict.removeItem(COSName.NEXT);
            PDOutlineItem clonedItem = new PDOutlineItem(clonedDict);
            setUpDestinations(clonedItem, currentPageNumber, documentCatalog);
            destLastOutlineItem.addLast(clonedItem);

        }
    }

    private void setUpDestinations(PDOutlineItem subItem, int currentPageNumber, PDDocumentCatalog documentCatalog) {
        if (subItem != null) {
            int pageNum = getOutlinePage(subItem, documentCatalog);
            PDPageDestination newDestination = new PDPageFitDestination();
            newDestination.setPage(pageNum != -1 ? document.getPage(pageNum + currentPageNumber) : null);
            subItem.setDestination(newDestination);
            setUpDestinations(subItem.getFirstChild(), currentPageNumber, documentCatalog);
        } else {
            return;
        }

        if (subItem.getNextSibling() != null) {
            setUpDestinations(subItem.getNextSibling(), currentPageNumber, documentCatalog);
        }

    }

    public int getOutlinePage(PDOutlineItem outlineItem, PDDocumentCatalog documentCatalog) {
        try {
            PDDestination pdDestination = outlineItem.getDestination();

            if (pdDestination == null) {
                PDAction outlineAction = outlineItem.getAction();
                if (outlineAction instanceof PDActionGoTo) {
                    pdDestination = ((PDActionGoTo) outlineAction).getDestination();
                    log.info("PDActionGoTo Title: {}", outlineItem.getTitle());
                }
            }

            if (pdDestination instanceof PDNamedDestination) {
                pdDestination = documentCatalog.findNamedDestinationPage((PDNamedDestination) pdDestination);
                log.info("PDNamedDestination Title: {}", outlineItem.getTitle());
            }

            if (pdDestination instanceof PDPageDestination) {
                var dest = (PDPageDestination) pdDestination;
                log.info("outlineItem Title: {}: dest page num{}", outlineItem.getTitle(), dest.retrievePageNumber());
                return Math.max(dest.retrievePageNumber(), 0);
            }
        } catch (Exception e) {
            log.error("GetOutlinePage Error message: " + e);
        }
        return -1;
    }

}
