package uk.gov.hmcts.reform.em.stitching.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFCoversheetService;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFMerger;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

import static pl.touk.throwing.ThrowingFunction.unchecked;

@Component
public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {
    private final Logger log = LoggerFactory.getLogger(DocumentTaskItemProcessor.class);
    private final DmStoreDownloader dmStoreDownloader;
    private final DmStoreUploader dmStoreUploader;
    private final DocumentConversionService documentConverter;
    private final PDFCoversheetService coversheetService;
    private final PDFMerger pdfMerger;

    public DocumentTaskItemProcessor(DmStoreDownloader dmStoreDownloader,
                                     DmStoreUploader dmStoreUploader,
                                     DocumentConversionService documentConverter,
                                     PDFCoversheetService coversheetService,
                                     PDFMerger pdfMerger) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
        this.documentConverter = documentConverter;
        this.coversheetService = coversheetService;
        this.pdfMerger = pdfMerger;
    }

    @Override
    public DocumentTask process(DocumentTask documentTask) {
        try {
            Map<BundleDocument, File> bundleFiles = dmStoreDownloader
                .downloadFiles(documentTask.getBundle().getSortedDocuments())
                .map(unchecked(documentConverter::convert))
                .map(unchecked(docs -> documentTask.getBundle().hasCoversheets() ? coversheetService.addCoversheet(docs) : docs))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

            final File outputFile = pdfMerger.merge(documentTask.getBundle(), bundleFiles, documentTask.getCaseData());

            dmStoreUploader.uploadFile(outputFile, documentTask);

            documentTask.setTaskState(TaskState.DONE);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            documentTask.setTaskState(TaskState.FAILED);
            documentTask.setFailureDescription(e.getMessage());
        }

        return documentTask;
    }

}
