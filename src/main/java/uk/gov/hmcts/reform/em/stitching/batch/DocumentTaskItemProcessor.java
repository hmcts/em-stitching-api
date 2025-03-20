package uk.gov.hmcts.reform.em.stitching.batch;

import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFMerger;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFWatermark;
import uk.gov.hmcts.reform.em.stitching.service.CdamService;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;
import uk.gov.hmcts.reform.em.stitching.template.DocmosisClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static pl.touk.throwing.ThrowingFunction.unchecked;
import static uk.gov.hmcts.reform.em.stitching.config.BatchConfiguration.DOCUMENT_TASK_RETRY_COUNT;

@Service
@Transactional(propagation = Propagation.REQUIRED)
@SuppressWarnings("java:S899")
public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {
    private final Logger log = LoggerFactory.getLogger(DocumentTaskItemProcessor.class);

    private static final String FAILURE_DESCRIPTION_TEMPLATE =
            "Document taskId %d, caseId: %s reached max retry count: %d";

    private final DmStoreDownloader dmStoreDownloader;
    private final DmStoreUploader dmStoreUploader;
    private final DocumentConversionService documentConverter;
    private final PDFMerger pdfMerger;
    private final DocmosisClient docmosisClient;
    private final PDFWatermark pdfWatermark;
    private final CdamService cdamService;
    private final EntityManager entityManager;

    private final StoreDocumentTaskRetryCount storeDocumentTaskRetryCount;

    public DocumentTaskItemProcessor(
        DmStoreDownloader dmStoreDownloader,
        DmStoreUploader dmStoreUploader,
        DocumentConversionService documentConverter,
        PDFMerger pdfMerger,
        DocmosisClient docmosisClient,
        PDFWatermark pdfWatermark,
        CdamService cdamService, EntityManager entityManager,
        StoreDocumentTaskRetryCount storeDocumentTaskRetryCount
    ) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
        this.documentConverter = documentConverter;
        this.pdfMerger = pdfMerger;
        this.docmosisClient = docmosisClient;
        this.pdfWatermark = pdfWatermark;
        this.cdamService = cdamService;
        this.entityManager = entityManager;
        this.storeDocumentTaskRetryCount = storeDocumentTaskRetryCount;
    }

    @Override
    public DocumentTask process(DocumentTask documentTask) {
        log.debug("DocumentTask : {}  started processing at {}",
                documentTask.getId(), LocalDateTime.now());

        if (checkAlreadyInProgress(documentTask)) {
            log.info("DocumentTask : {} is already being processed", documentTask.getId());
            return null;
        }

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        Map<BundleDocument, File> bundleFiles = null;
        File outputFile = null;

        if (documentTask.getRetryAttempts() >= DOCUMENT_TASK_RETRY_COUNT - 1) {
            documentTask.setTaskState(TaskState.FAILED);
            String errorDescription = String.format(
                    FAILURE_DESCRIPTION_TEMPLATE,
                    documentTask.getId(),
                    documentTask.getCaseTypeId(),
                    DOCUMENT_TASK_RETRY_COUNT
            );
            documentTask.setFailureDescription(errorDescription);
            log.error(errorDescription);
            return documentTask;
        }
        this.storeDocumentTaskRetryCount.incrementRetryAttempts(documentTask);
        log.info(
            "DocumentTask : {}, CoverPage template {}",
            documentTask.getId(),
            documentTask.getBundle().getCoverpageTemplate()
        );
        try {
            final File coverPageFile = StringUtils.isNotBlank(documentTask.getBundle().getCoverpageTemplate())
                ? docmosisClient.renderDocmosisTemplate(
                documentTask.getBundle().getCoverpageTemplate(),
                documentTask.getBundle().getCoverpageTemplateData()) : null;

            final File documentImage =
                    documentTask.getBundle().getDocumentImage() != null
                            && documentTask.getBundle().getDocumentImage().getDocmosisAssetId() != null
                        ? docmosisClient.getDocmosisImage(
                                documentTask.getBundle().getDocumentImage().getDocmosisAssetId())
                        : null;

            if (StringUtils.isNotBlank(documentTask.getCaseTypeId())
                && StringUtils.isNotBlank(documentTask.getJurisdictionId())) {
                bundleFiles = cdamService
                    .downloadFiles(documentTask)
                    .map(unchecked(documentConverter::convert))
                    .map(file -> pdfWatermark.processDocumentWatermark(
                            documentImage, file,
                            documentTask.getBundle().getDocumentImage()))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
                log.info("Documents downloaded through CDAM for DocumentTask Id : #{} ", documentTask.getId());
                outputFile = pdfMerger.merge(documentTask.getBundle(), bundleFiles, coverPageFile);
                log.info("Documents merged  outputFile Name: {} ", outputFile != null ? outputFile.getName() : "null");

                cdamService.uploadDocuments(outputFile, documentTask);

                log.info("Documents uploaded through CDAM for DocumentTask Id : #{} ", documentTask.getId());
            } else {
                bundleFiles = dmStoreDownloader
                    .downloadFiles(documentTask.getBundle().getSortedDocuments())
                    .map(unchecked(documentConverter::convert))
                    .map(file -> pdfWatermark.processDocumentWatermark(
                            documentImage, file,
                            documentTask.getBundle().getDocumentImage()))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

                outputFile = pdfMerger.merge(documentTask.getBundle(), bundleFiles, coverPageFile);

                dmStoreUploader.uploadFile(outputFile, documentTask);
            }

            documentTask.setTaskState(TaskState.DONE);
            log.info(
                "Stitching completed for caseId:{}, DocumentTask Id:{}, created file:{}",
                documentTask.getCaseId(),
                documentTask.getId(),
                outputFile == null ? null : outputFile.getName()
            );

        } catch (Exception e) {
            log.error(
                "Failed DocumentTask id: {}, caseId: {}, Error: {}",
                documentTask.getId(),
                documentTask.getCaseId(),
                e
            );

            documentTask.setTaskState(TaskState.FAILED);
            documentTask.setFailureDescription(e.getMessage());
        }
        deleteFile(outputFile);
        if (Objects.nonNull(bundleFiles)) {
            bundleFiles.forEach((bundleDocument, file) -> deleteFile(file));
        }
        stopwatch.stop();
        long timeElapsed = TimeUnit.MILLISECONDS.toSeconds(stopwatch.getTime());

        log.info("Time taken for DocumentTask completion: {}  was {} seconds",
                documentTask.getId(),timeElapsed);
        return documentTask;
    }

    private boolean checkAlreadyInProgress(DocumentTask documentTask) {
        DocumentTask freshTask = entityManager.find(
            DocumentTask.class,
            documentTask.getId()
        );
        return freshTask.getTaskState() != TaskState.NEW;
    }

    private void deleteFile(File outputFile) {
        try {
            if (Objects.nonNull(outputFile)) {
                Path path = outputFile.toPath();
                if (Objects.nonNull(path)) {
                    Files.deleteIfExists(outputFile.toPath());
                }
            }
        } catch (IOException ioException) {
            log.error("Cleaning up files failed with error: {}", ioException.getMessage());
        }
    }
}
