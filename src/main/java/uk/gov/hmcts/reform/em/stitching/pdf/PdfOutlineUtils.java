package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.SortableBundleItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;
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
                .mapToInt(getItemsFromOutline)
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
                .map(getItemTitlesFromOutline)
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

    private static final ToIntFunction<PDDocumentOutline> getItemsFromOutline = outline -> {
        if (Objects.isNull(outline)) {
            return 0;
        }
        int count = 0;
        PDOutlineItem current = outline.getFirstChild();
        while (Objects.nonNull(current)) {
            count++;
            current = current.getNextSibling();
        }
        return count;
    };

    private static final Function<PDDocumentOutline, List<String>> getItemTitlesFromOutline = outline -> {
        if (Objects.isNull(outline)) {
            return Collections.emptyList();
        }
        List<String> titles = new ArrayList<>();
        PDOutlineItem current = outline.getFirstChild();
        while (Objects.nonNull(current)) {
            titles.add(current.getTitle());
            current = current.getNextSibling();
        }
        return titles;
    };
}