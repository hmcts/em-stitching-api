package uk.gov.hmcts.reform.em.stitching.batch;

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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static pl.touk.throwing.ThrowingFunction.unchecked;

@Service
@Transactional(propagation = Propagation.REQUIRED)
@SuppressWarnings("java:S899")
public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {
    private final Logger log = LoggerFactory.getLogger(DocumentTaskItemProcessor.class);
    private final DmStoreDownloader dmStoreDownloader;
    private final DmStoreUploader dmStoreUploader;
    private final DocumentConversionService documentConverter;
    private final PDFMerger pdfMerger;
    private final DocmosisClient docmosisClient;
    private final PDFWatermark pdfWatermark;
    private final CdamService cdamService;

    public DocumentTaskItemProcessor(DmStoreDownloader dmStoreDownloader,
                                     DmStoreUploader dmStoreUploader,
                                     DocumentConversionService documentConverter,
                                     PDFMerger pdfMerger,
                                     DocmosisClient docmosisClient,
                                     PDFWatermark pdfWatermark,
                                     CdamService cdamService) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
        this.documentConverter = documentConverter;
        this.pdfMerger = pdfMerger;
        this.docmosisClient = docmosisClient;
        this.pdfWatermark = pdfWatermark;
        this.cdamService = cdamService;
    }

    @Override
    public DocumentTask process(DocumentTask documentTask) {
        log.debug("DocumentTask : {}  started processing at {}",
                documentTask.getId(), LocalDateTime.now());
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        Map<BundleDocument, File> bundleFiles = null;
        File outputFile = null;

        try {
            final File coverPageFile = StringUtils.isNotBlank(documentTask.getBundle().getCoverpageTemplate())
                ? docmosisClient.renderDocmosisTemplate(
                documentTask.getBundle().getCoverpageTemplate(),
                documentTask.getBundle().getCoverpageTemplateData()) : null;

            final File documentImage =
                    documentTask.getBundle().getDocumentImage() != null
                            && documentTask.getBundle().getDocumentImage().getDocmosisAssetId() != null
                        ? docmosisClient.getDocmosisImage(documentTask.getBundle().getDocumentImage().getDocmosisAssetId())
                        : null;

            if (StringUtils.isNotBlank(documentTask.getCaseTypeId())
                && StringUtils.isNotBlank(documentTask.getJurisdictionId())) {
                bundleFiles = cdamService
                    .downloadFiles(documentTask)
                    .map(unchecked(documentConverter::convert))
                    .map(file -> pdfWatermark.processDocumentWatermark(documentImage, file, documentTask.getBundle().getDocumentImage()))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
                log.info("Documents downloaded through CDAM for DocumentTask Id : #{} ", documentTask.getId());
                outputFile = pdfMerger.merge(documentTask.getBundle(), bundleFiles, coverPageFile);

                cdamService.uploadDocuments(outputFile, documentTask);

                log.info("Documents uploaded through CDAM for DocumentTask Id : #{} ", documentTask.getId());
            } else {
                bundleFiles = dmStoreDownloader
                    .downloadFiles(documentTask.getBundle().getSortedDocuments())
                    .map(unchecked(documentConverter::convert))
                    .map(file -> pdfWatermark.processDocumentWatermark(documentImage, file, documentTask.getBundle().getDocumentImage()))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

                outputFile = pdfMerger.merge(documentTask.getBundle(), bundleFiles, coverPageFile);

                dmStoreUploader.uploadFile(outputFile, documentTask);
            }

            documentTask.setTaskState(TaskState.DONE);
            log.info("Stitching completed for DocumentTask Id : #{}", documentTask.getId());

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
        if (Objects.nonNull(outputFile)) {
            outputFile.delete();
        }
        if (Objects.nonNull(outputFile)) {
            bundleFiles.entrySet().forEach(f -> f.getValue().delete());
        }
        stopwatch.stop();
        long timeElapsed = TimeUnit.MILLISECONDS.toSeconds(stopwatch.getTime());

        log.debug("Time taken for DocumentTask completion: {}  was {} seconds",
                documentTask.getId(),timeElapsed);
        return documentTask;
    }
}
