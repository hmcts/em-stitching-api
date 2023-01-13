package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleItemType;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addCenterText;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addPageNumbers;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.addRightLink;

@Service
public class PDFMerger {

    public static final String INDEX_PAGE = "Index Page";

    public File merge(Bundle bundle, Map<BundleDocument, File> documents, File coverPage) throws IOException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger(documents, bundle, coverPage);

        return statefulPDFMerger.merge();
    }

    private static class StatefulPDFMerger {
        private final Logger log = LoggerFactory.getLogger(StatefulPDFMerger.class);
        private final PDFMergerUtility merger = new PDFMergerUtility();
        private final PDDocument document = new PDDocument();
        private final PDFOutline pdfOutline;
        private TableOfContents tableOfContents;
        private final Map<BundleDocument, File> documents;
        private final Bundle bundle;
        private static final String BACK_TO_TOP = "Back to index";
        private int currentPageNumber = 0;
        private final File coverPage;
        private TreeNode<SortableBundleItem> treeRoot;

        private StatefulPDFMerger(Map<BundleDocument, File> documents, Bundle bundle, File coverPage) {
            this.documents = documents;
            this.bundle = bundle;
            this.coverPage = coverPage;
            this.treeRoot = createOutline(bundle);

            this.pdfOutline = new PDFOutline(document, treeRoot);

        }

        private File merge() throws IOException {
            try {
                pdfOutline.addBundleItem(bundle);

                if (coverPage != null) {
                    try (PDDocument coverPageDocument = PDDocument.load(coverPage)) {
                        coverPageDocument.getDocumentCatalog().setDocumentOutline(null);
                        merger.appendDocument(document, coverPageDocument);
                        currentPageNumber += coverPageDocument.getNumberOfPages();
                        pdfOutline.addItem(0, "Cover Page");
                    }
                }

                if (bundle.hasTableOfContents()) {
                    this.tableOfContents = new TableOfContents(document, bundle, documents);
                    pdfOutline.addItem(currentPageNumber, INDEX_PAGE);
                    currentPageNumber += tableOfContents.getNumberPages();
                }

                addContainer(bundle);

                pdfOutline.setRootOutlineItemDest();

                final File file = File.createTempFile("stitched", ".pdf");
                document.save(file);
                return file;
            } finally {
                document.close();
            }
        }

        private void addContainer(SortableBundleItem container) throws IOException {
            for (SortableBundleItem item : container.getSortedItems().collect(Collectors.toList())) {
                if (item.getSortedItems().count() > 0) {
                    if (bundle.hasFolderCoversheets()) {
                        addCoversheet(item);
                    }
                    addContainer(item);
                } else if (documents.containsKey(item)) {
                    if (bundle.hasCoversheets()) {
                        addCoversheet(item);
                    }

                    try {
                        addDocument(item);
                    } catch (Exception e) {
                        String filename = documents.get(item).getName();
                        String name = item.getTitle();
                        String error = String.format("Error processing %s, %s", name, filename);
                        log.error(error, e);

                        throw new IOException(error, e);
                    }
                }
            }

            if (tableOfContents != null) {
                tableOfContents.setEndOfFolder(true);
            }
        }

        private void addCoversheet(SortableBundleItem item) throws IOException {
            PDPage page = new PDPage();
            document.addPage(page);

            if (tableOfContents != null) {
                if (item.getSortedItems().count() > 0) {
                    tableOfContents.addFolder(item.getTitle(), currentPageNumber);
                }
                addUpwardLink();
            }

            addCenterText(document, page, item.getTitle(), 330);

            if (item.getSortedItems().count() > 0) {
                pdfOutline.addItem(item, currentPageNumber);
            }
            currentPageNumber++;
        }

        private void addDocument(SortableBundleItem item) throws IOException {
            try (PDDocument newDoc = PDDocument.load(documents.get(item))) {
                addDocument(item, newDoc);
            }
        }

        private void addDocument(SortableBundleItem item, PDDocument newDoc) throws IOException {
            final PDDocumentOutline newDocOutline = newDoc.getDocumentCatalog().getDocumentOutline();

            if (bundle.hasCoversheets()) {
                log.info("hasCoversheets item.getTitle {} ", item.getTitle());
                pdfOutline.addItem(item, document.getNumberOfPages() - 1);
            } else if (newDocOutline != null) {
                log.info("existing DocOutline item.getTitle {} ", item.getTitle());
            }
            newDoc.getDocumentCatalog().setDocumentOutline(null);

            try {
                merger.appendDocument(document, newDoc);
            } catch (IndexOutOfBoundsException e) {
                newDoc.getDocumentCatalog().setStructureTreeRoot(new PDStructureTreeRoot());
                log.debug("Setting new PDF structure tree of " + item.getTitle());
                merger.appendDocument(document, newDoc);
            }

            if (bundle.getPaginationStyle() != PaginationStyle.off) {
                addPageNumbers(
                        document,
                        bundle.getPaginationStyle(),
                        currentPageNumber,
                        currentPageNumber + newDoc.getNumberOfPages());
            }

            if (tableOfContents != null && newDocOutline != null) {
                ArrayList<PDOutlineItem> siblings = new ArrayList<>();
                PDOutlineItem anySubtitlesForItem = newDocOutline.getFirstChild();
                while (anySubtitlesForItem != null) {
                    siblings.add(anySubtitlesForItem);
                    anySubtitlesForItem = anySubtitlesForItem.getNextSibling();
                }
                tableOfContents.addDocument(item.getTitle(), currentPageNumber, newDoc.getNumberOfPages());
                for (PDOutlineItem subtitle : siblings) {
                    tableOfContents.addDocumentWithOutline(item.getTitle(), currentPageNumber, subtitle);
                }
            }
            if (tableOfContents != null && newDocOutline == null) {
                tableOfContents.addDocument(item.getTitle(), currentPageNumber, newDoc.getNumberOfPages());
            }

            if (!bundle.hasCoversheets()) {
                pdfOutline.addItem(item, currentPageNumber);
            }

            currentPageNumber += newDoc.getNumberOfPages();
        }

        private void addUpwardLink() throws IOException {
            final float yOffset = 730f;
            final PDPage from = document.getPage(currentPageNumber);

            addRightLink(document, from, tableOfContents.getPage(), StatefulPDFMerger.BACK_TO_TOP, yOffset, PDType1Font.HELVETICA, 12);
        }

        private TreeNode<SortableBundleItem> createOutline(Bundle bundle) {

            TreeNode<SortableBundleItem> root = new TreeNode(bundle);

            var all = bundle.getSortedItems().collect(Collectors.toList());
            for (var item : all) {
                createSubs(item, root, bundle);
            }
            return root;
        }

        void createSubs(SortableBundleItem addItem, TreeNode<SortableBundleItem> treeNode, Bundle bundle) {
            if (bundle.hasFolderCoversheets() && addItem.getType() == BundleItemType.FOLDER) {
                treeNode = treeNode.addChild(addItem);
            } else if (addItem.getType() == BundleItemType.DOCUMENT) {
                treeNode.addChild(addItem);
                return;
            }
            if (addItem.getSortedItems().count() > 0) {
                var all = addItem.getSortedItems().collect(Collectors.toList());
                for (var item : all) {
                    createSubs(item, treeNode, bundle);
                }
            }
        }

    }
}
