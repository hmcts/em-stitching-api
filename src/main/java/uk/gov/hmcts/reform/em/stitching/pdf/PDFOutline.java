package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;

public class PDFOutline {

    private final Logger log = LoggerFactory.getLogger(PDFOutline.class);

    private final PDDocument document;
    private final TreeNode<SortableBundleItem> outlineTree;
    private PDOutlineItem rootOutline;
    private Set<COSObjectKey> cosObjectKeys;

    public PDFOutline(PDDocument document, TreeNode<SortableBundleItem> outlineTree) {
        this.document = document;
        this.outlineTree = outlineTree;
        this.cosObjectKeys = new HashSet<>();
    }

    public void addBundleItem(SortableBundleItem item) {
        log.debug("AddBundleItem AddItem title {}", item.getTitle());

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
        log.debug("AddItem title {}", item.getTitle());
        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setDestination(document.getPage(page));
        outlineItem.setTitle(trimOutlineTitle(item.getTitle()));
        outlineItem.setBold(true);
        var key = createItemKey(item);
        outlineItem.getCOSObject().setItem(key, COSNull.NULL);
        addOutline(outlineItem, key);
    }

    public void addItem(int page, String title) {
        log.debug("AddItem title {}", title);
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

    public void copyOutline(
            PDDocumentOutline srcOutline,
            PDDocumentCatalog documentCatalog,
            String key, int currentPageNumber)
            throws IOException {
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

        destLastOutlineItem.getCOSObject().setItem(COSName.FILTER, COSName.FLATE_DECODE);

        List<PDOutlineItem> itemList = StreamSupport
            .stream(srcOutline.children().spliterator(), true).toList();
        for (PDOutlineItem item : itemList) {
            cosObjectKeys.add(item.getCOSObject().getKey());
            item.getCOSObject().setKey(null);
            item.getCOSObject().removeItem(COSName.PREV);
            item.getCOSObject().removeItem(COSName.PARENT);
            item.getCOSObject().removeItem(COSName.NEXT);
            item.setStructureElement(null);
            setUpDestinations(item, currentPageNumber, documentCatalog);
            if (item.getCOSObject().containsKey(COSName.DEST)) {
                item.getCOSObject().removeItem(COSName.A);
                // This will remove the old destination info.
                // Like navigating to the old/original document page number.
            }
            item.getCOSObject().setItem(COSName.FIRST, removeNullObject(item.getFirstChild()));
            item.getCOSObject().setItem(COSName.LAST, removeNullObject(item.getLastChild()));
            destLastOutlineItem.addLast(item);
        }
    }

    public PDOutlineItem removeNullObject(PDOutlineItem outline) {
        if (Objects.isNull(outline) || Objects.isNull(outline.getCOSObject())) {
            return outline;
        }
        List<COSName> removeCOSName = new ArrayList<COSName>();
        for (Map.Entry<COSName, COSBase> entry : outline.getCOSObject().entrySet()) {
            if (Objects.nonNull(entry.getValue())) {
                if (entry.getValue() instanceof COSObject csCosObject) {
                    if (Objects.isNull((csCosObject).getObject())) {
                        removeCOSName.add(entry.getKey());
                    }
                }
            }
        }

        for (COSName key : removeCOSName) {
            log.debug("COSName element removed {} ", key.getName());
            outline.getCOSObject().removeItem(key);
        }
        return outline;
    }

    private void setUpDestinations(PDOutlineItem subItem, int currentPageNumber, PDDocumentCatalog documentCatalog)
            throws IOException {
        if (subItem != null) {
            COSObjectKey key = subItem.getCOSObject().getKey();
            if (Objects.nonNull(key) && cosObjectKeys.contains(key)) {
                log.warn("key already exists: {}", key);
                subItem.getCOSObject().setKey(null);
                return;
            }
            cosObjectKeys.add(key);

            subItem.getCOSObject().setKey(null);

            int pageNum = getOutlinePage(subItem, documentCatalog);
            subItem.getCOSObject().removeItem(COSName.PARENT);
            subItem.setDestination(pageNum != -1 ? document.getPage(pageNum + currentPageNumber) : null);
            if (subItem.getCOSObject().containsKey(COSName.DEST)) {
                subItem.getCOSObject().removeItem(COSName.A);
                // This will remove the old destination info.
                // Like navigating to the old/original document page number.
            }
            setUpDestinations(subItem.getFirstChild(), currentPageNumber, documentCatalog);
            subItem.getCOSObject().setItem(COSName.FIRST, removeNullObject(subItem.getFirstChild()));
            subItem.getCOSObject().setItem(COSName.LAST, removeNullObject(subItem.getLastChild()));
            subItem.getCOSObject().setItem(COSName.NEXT, removeNullObject(subItem.getNextSibling()));
            subItem.getCOSObject().setItem(COSName.PREV, removeNullObject(subItem.getPreviousSibling()));
        } else {
            return;
        }

        if (subItem.getNextSibling() != null) {
            log.debug("sibling key is {}", subItem.getNextSibling().getCOSObject().getKey());
            setUpDestinations(subItem.getNextSibling(), currentPageNumber, documentCatalog);
        }

    }

    public int getOutlinePage(PDOutlineItem outlineItem, PDDocumentCatalog documentCatalog) {
        try {
            PDDestination pdDestination = outlineItem.getDestination();

            if (Objects.isNull(pdDestination) && outlineItem.getAction() instanceof PDActionGoTo pdActionGoTo) {
                pdDestination = pdActionGoTo.getDestination();
                log.debug("PDActionGoTo Title: {}", outlineItem.getTitle());
            }

            if (pdDestination instanceof PDNamedDestination pdNamedDestination) {
                pdDestination = documentCatalog.findNamedDestinationPage(pdNamedDestination);
                log.debug("PDNamedDestination Title: {}", outlineItem.getTitle());
            }

            if (pdDestination instanceof PDPageDestination pdPageDestination) {
                if (outlineItem.getTitle() == null) {
                    outlineItem.setTitle("   ");
                }
                log.debug("outlineItem Title: {}: dest page num{}",
                    outlineItem.getTitle(), pdPageDestination.retrievePageNumber());
                return Math.max(pdPageDestination.retrievePageNumber(), 0);
            }
        } catch (Exception e) {
            log.error("GetOutlinePage Error message: {}", e.toString());
        }
        return -1;
    }

}
