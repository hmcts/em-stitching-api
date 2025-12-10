package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import java.util.List;
import java.util.Map;

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
        // Keep docs open until merged file saved.
        private List<PDDocument> openDocs = new ArrayList<>();

        private StatefulPDFMerger(Map<BundleDocument, File> documents, Bundle bundle, File coverPage) {
            this.documents = documents;
            this.bundle = bundle;
            this.coverPage = coverPage;
            this.treeRoot = createOutline(bundle);

            this.pdfOutline = new PDFOutline(document, treeRoot);

        }

        private File merge() throws IOException {
            log.info("Starting merge for bundle: {}, documents: {}", 
                bundle != null ? bundle.getBundleTitle() : "null",
                getDocumentTitles());
            try {
                pdfOutline.addBundleItem(bundle);

                if (coverPage != null) {
                    PDDocument coverPageDocument = Loader.loadPDF(coverPage);
                    openDocs.add(coverPageDocument);
                    coverPageDocument.getDocumentCatalog().setDocumentOutline(null);
                    merger.appendDocument(document, coverPageDocument);
                    currentPageNumber += coverPageDocument.getNumberOfPages();
                    pdfOutline.addItem(0, "Cover Page");
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
                log.info("Merge completed successfully for bundle: {}", bundle.getBundleTitle());
                return file;
            } catch (Exception e) {
                log.error("Merge failed for bundle: {}, documents: {}, error: {}", 
                    bundle != null ? bundle.getBundleTitle() : "null",
                    getDocumentTitles(),
                    e.getMessage(), e);
                throw e;
            } finally {
                openDocs.forEach(newDoc -> {
                    try {
                        newDoc.close();
                    } catch (Exception e) {
                        log.info("Closing new documents failed, skipping");
                    }
                });
                document.close();
            }
        }

        private Object getDocumentTitles() {
            return documents != null 
                ? documents.keySet().stream()
                    .map(d -> d != null ? d.getDocTitle() : "null")
                    .toList() 
                : "null";
        }

        private void addContainer(SortableBundleItem container) throws IOException {
            for (SortableBundleItem item : container.getSortedItems().toList()) {
                if (item.getSortedItems().findAny().isPresent()) {
                    if (bundle.hasFolderCoversheets()) {
                        addCoversheet(item);
                    }
                    addContainer(item);
                } else if (documents.containsKey(item)) {
                    if (bundle.hasCoversheets()) {
                        addCoversheet(item);
                    }

                    try {
                        PDDocument newDoc = Loader.loadPDF(documents.get(item));
                        openDocs.add(newDoc);
                        addDocument(item, newDoc);
                    } catch (Exception e) {
                        String filename = documents.get(item).getName();
                        String docTitle = item.getTitle();
                        String error =
                                String.format(
                                        "Error processing, document title: %s, file name: %s",
                                        docTitle,
                                        filename
                                );
                        log.error("PDDocument load failed, docTitle:{}, filename: {}, error:",  docTitle, filename, e);
                        throw new IOException(error);
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

        private void addDocument(SortableBundleItem item, PDDocument newDoc) throws IOException {
            final PDDocumentCatalog newDocumentCatalog = newDoc.getDocumentCatalog();
            final PDDocumentOutline newDocOutline = newDocumentCatalog.getDocumentOutline();

            if (bundle.hasCoversheets()) {
                log.debug("hasCoversheets item.getTitle {} ", item.getTitle());
                pdfOutline.addItem(item, document.getNumberOfPages() - 1);
            } else if (newDocOutline != null) {
                log.debug("existing DocOutline item.getTitle {} ", item.getTitle());
            }
            newDoc.getDocumentCatalog().setDocumentOutline(null);

            try {
                merger.appendDocument(document, newDoc);
            } catch (IndexOutOfBoundsException e) {
                newDoc.getDocumentCatalog().setStructureTreeRoot(new PDStructureTreeRoot());
                log.debug("Setting new PDF structure tree of {}", item.getTitle());
                merger.appendDocument(document, newDoc);
            }

            if (bundle.getPaginationStyle() != PaginationStyle.off) {
                addPageNumbers(
                    document,
                    bundle.getPaginationStyle(),
                    currentPageNumber,
                    currentPageNumber + newDoc.getNumberOfPages()
                );
            }

            if (tableOfContents != null && newDocOutline != null) {
                ArrayList<PDOutlineItem> siblings = new ArrayList<>();
                PDOutlineItem anySubtitlesForItem = newDocOutline.getFirstChild();
                while (anySubtitlesForItem != null) {
                    siblings.add(anySubtitlesForItem);
                    anySubtitlesForItem = anySubtitlesForItem.getNextSibling();
                }
                tableOfContents.addDocument(item.getTitle(), currentPageNumber, newDoc.getNumberOfPages());
                log.info("Processing outline for document: {}, outline children count: {}", 
                    item.getTitle(), siblings.size());
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

            if (newDocOutline != null) {
                log.info("Copying outline for document: {}, currentPageNumber: {}", 
                    item.getTitle(), currentPageNumber);
                try {
                    pdfOutline.copyOutline(
                            newDocOutline,
                            newDocumentCatalog,
                            item.getId() + item.getTitle(),
                            currentPageNumber);
                } catch (Exception e) {
                    log.error("Error copying outline for document: {}, error: {}", 
                        item.getTitle(), e.getMessage(), e);
                    throw e;
                }
            }

            currentPageNumber += newDoc.getNumberOfPages();
        }

        private void addUpwardLink() throws IOException {
            final float yOffset = 730f;
            final PDPage from = document.getPage(currentPageNumber);

            addRightLink(
                document,
                from,
                tableOfContents.getPage(),
                StatefulPDFMerger.BACK_TO_TOP,
                yOffset,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                12
            );
        }

        private TreeNode<SortableBundleItem> createOutline(Bundle bundle) {

            TreeNode<SortableBundleItem> root = new TreeNode<>(bundle);

            var all = bundle.getSortedItems().toList();
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
            if (addItem.getSortedItems().findAny().isPresent()) {
                var all = addItem.getSortedItems().toList();
                for (var item : all) {
                    createSubs(item, treeNode, bundle);
                }
            }
        }
    }
}
