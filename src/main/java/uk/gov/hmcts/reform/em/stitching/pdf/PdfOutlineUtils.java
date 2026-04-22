package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class PdfOutlineUtils {

    private static final Logger log = LoggerFactory.getLogger(PdfOutlineUtils.class);

    private PdfOutlineUtils() {
        // Prevent instantiation of utility class
    }

    public static Integer getNumberOfSubtitles(SortableBundleItem container,
                                               Map<BundleDocument, File> documentBundledFilesRef) {
        if (container.getSortedDocuments().count() == documentBundledFilesRef.size()) {
            List<PDDocument> docsToClose = new ArrayList<>();
            int subtitles = extractDocumentOutlineStream(container, documentBundledFilesRef, docsToClose)
                .mapToInt(outline -> Objects.isNull(outline)
                    ? 0
                    : countNestedItems(outline, 0, new HashSet<>()))
                .sum();
            closeDocuments(docsToClose);
            return subtitles;
        }
        return 0;
    }

    public static List<String> getSubtitles(SortableBundleItem container,
                                            Map<BundleDocument, File> documentBundledFilesRef) {
        if (container.getSortedDocuments().count() == documentBundledFilesRef.size()) {
            List<PDDocument> docsToClose = new ArrayList<>();
            List<String> subtitles = extractDocumentOutlineStream(container, documentBundledFilesRef, docsToClose)
                .map(outline -> Objects.isNull(outline)
                    ? Collections.<String>emptyList()
                    : extractNestedTitles(outline, 0, new HashSet<>()))
                .flatMap(List::stream)
                .toList();
            closeDocuments(docsToClose);
            return subtitles;
        }
        return new ArrayList<>();
    }

    private static Stream<PDDocumentOutline> extractDocumentOutlineStream(SortableBundleItem container,
                                                                   Map<BundleDocument, File> documentBundledFilesRef,
                                                                   List<PDDocument> docsToClose) {
        return container
            .getSortedItems().flatMap(SortableBundleItem::getSortedDocuments)
            .map(bundleDocument -> {
                try {
                    PDDocument pdDocument = Loader.loadPDF(documentBundledFilesRef.get(bundleDocument));
                    docsToClose.add(pdDocument);
                    return pdDocument;
                } catch (IOException ioException) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .map(pdDocument -> pdDocument.getDocumentCatalog().getDocumentOutline())
            .filter(pdDocumentOutline ->
                Objects.nonNull(pdDocumentOutline) && Objects.nonNull(pdDocumentOutline.getFirstChild()));
    }

    private static void closeDocuments(List<PDDocument> docsToClose) {
        docsToClose.forEach(doc -> {
            try {
                doc.close();
            } catch (IOException e) {
                log.error("Failed to close PDDocument after extracting outline", e);
            }
        });
    }

    private static int countNestedItems(PDOutlineNode node, int depth, Set<PDOutlineItem> visited) {
        if (depth > 10 || Objects.isNull(node)) {
            return 0;
        }
        int count = 0;
        PDOutlineItem current = node.getFirstChild();
        while (Objects.nonNull(current)) {
            if (!visited.add(current)) {
                break;
            }
            if (Objects.nonNull(current.getTitle())) {
                count++;
            }
            count += countNestedItems(current, depth + 1, visited);
            current = current.getNextSibling();
        }
        return count;
    }

    private static List<String> extractNestedTitles(PDOutlineNode node, int depth, Set<PDOutlineItem> visited) {
        List<String> titles = new ArrayList<>();
        if (depth > TableOfContents.MAX_OUTLINE_DEPTH || Objects.isNull(node)) {
            return titles;
        }
        PDOutlineItem current = node.getFirstChild();
        while (Objects.nonNull(current)) {
            if (!visited.add(current)) {
                break;
            }
            if (Objects.nonNull(current.getTitle())) {
                titles.add(current.getTitle());
            }
            titles.addAll(extractNestedTitles(current, depth + 1, visited));
            current = current.getNextSibling();
        }
        return titles;
    }
}