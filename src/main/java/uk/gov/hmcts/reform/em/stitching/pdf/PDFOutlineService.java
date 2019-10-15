package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;

import java.io.IOException;

@Service
public class PDFOutlineService {

    private PDDocument doc;
    private Bundle bundle;
    private PDOutlineItem parentOutline;

    PDFOutlineService() {
    }

    PDFOutlineService(PDDocument doc, Bundle bundle) {
        this.doc = doc;
        this.bundle = bundle;
    }

    // get outline for document and if it does not exist, create one
    private PDDocumentOutline getOutline() {
        if (doc.getDocumentCatalog().getDocumentOutline() == null) {
            PDDocumentOutline tempOutline = new PDDocumentOutline();
            doc.getDocumentCatalog().setDocumentOutline(tempOutline);
        }
        return doc.getDocumentCatalog().getDocumentOutline();
    }

    public void createOutlines() {
        createBundleOutline();
        int TOCPage = 0;

        if (bundle.hasCoversheets()) {
            this.createChildOutline(parentOutline, TOCPage, "Cover Page");
            TOCPage = 1;
        }

        if (bundle.hasTableOfContents()) {
            this.createChildOutline(parentOutline, TOCPage, "Index Page");
        }
    }

    public void createBundleOutline() {
        PDDocumentOutline parentOutline = new PDDocumentOutline();
        doc.getDocumentCatalog().setDocumentOutline(parentOutline);
        parentOutline.openNode();

        PDOutlineItem parentOutlineItem = new PDOutlineItem();
        parentOutlineItem.setTitle("Bundle");
        parentOutline.addLast(parentOutlineItem);

        this.parentOutline = parentOutlineItem;
    }

    public void setBundleDest() {
        PDPageDestination dest = new PDPageFitWidthDestination();
        dest.setPage(doc.getPages().get(0));
        parentOutline.getFirstChild().setDestination(dest);
    }

    public PDOutlineItem createChildOutline(PDOutlineItem parentOutline, int page, String title) {
        PDPageDestination dest = new PDPageFitWidthDestination();
        dest.setPage(doc.getPages().get(page));
        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setDestination(dest);
        outlineItem.setTitle(title);
        parentOutline.addLast(outlineItem);
        parentOutline.openNode();

        return outlineItem;
    }

    public PDOutlineItem getParentOutline() {
        return parentOutline;
    }

    public void copyDocumentOutline(PDDocument doc, PDDocumentOutline documentOutline, PDOutlineItem parentOutline, int totalPageNumber) throws IOException {
        this.doc = doc;
        PDOutlineItem item = documentOutline.getFirstChild();
        while( item != null )
        {
            PDOutlineItem outlineItem = copyOutline(item, parentOutline, totalPageNumber);
            PDOutlineItem child = item.getFirstChild();
            while( child != null )
            {
                parentOutline = outlineItem;
                copyOutline(child, parentOutline, totalPageNumber);
                child = child.getNextSibling();
            }
            item = item.getNextSibling();
        }
    }

    public PDOutlineItem copyOutline(PDOutlineItem pdOutlineItem, PDOutlineItem parentOutline, int totalPageNumber) throws IOException {
        PDOutlineItem outlineItem = new PDOutlineItem();
        outlineItem.setTitle(pdOutlineItem.getTitle());
        if (pdOutlineItem.getDestination() != null) {
            int page = getPage(pdOutlineItem);
            outlineItem.setDestination(doc.getPages().get(page + totalPageNumber));
        }
        parentOutline.addLast(outlineItem);
        return outlineItem;
    }

    public int getPage(PDOutlineItem outlineItem) throws IOException {
        PDPageDestination dest = (PDPageDestination) outlineItem.getDestination();
        return Math.max(dest.retrievePageNumber() - 1, 0);
    }

    public void removeAllOutlines(PDDocument pdDocument) {
        pdDocument.getDocumentCatalog().setDocumentOutline(null);
    }

    public void createDocumentCoversheetOutline(PDDocument pdDocument, String title) {
        PDDocumentOutline pdDocumentOutline = pdDocument.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem pdOutlineItem = new PDOutlineItem();
        pdOutlineItem.setTitle(title);
        pdOutlineItem.setDestination(pdDocument.getPages().get(0));
        pdDocumentOutline.addFirst(pdOutlineItem);
    }
}
