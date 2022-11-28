package uk.gov.hmcts.reform.em.stitching.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static pl.touk.throwing.ThrowingFunction.unchecked;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {
    private final Logger log = LoggerFactory.getLogger(DocumentTaskItemProcessor.class);
    private final DmStoreDownloader dmStoreDownloader;
    private final DmStoreUploader dmStoreUploader;
    private final DocumentConversionService documentConverter;
    private final PDFMerger pdfMerger;
    private final DocmosisClient docmosisClient;
    private final PDFWatermark pdfWatermark;
    private final CdamService cdamService;


    private ObjectMapper objectMapper = new ObjectMapper();

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
        try {

            if(documentTask != null) {
                log.info("doc process {}", documentTask);
                if(documentTask.getBundle().getCoverpageTemplateData() != null) {
                    log.info(
                        "getCoverpageTemplateData {}",
                        objectMapper.writeValueAsString(documentTask.getBundle().getCoverpageTemplateData())
                    );
                }
            }
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
                Map<BundleDocument, File> bundleFiles = cdamService
                    .downloadFiles(documentTask)
                    .map(unchecked(documentConverter::convert))
                    .map(file -> pdfWatermark.processDocumentWatermark(documentImage, file, documentTask.getBundle().getDocumentImage()))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
                log.info(String.format("Documents downloaded through CDAM for DocumentTask Id : #%d ",
                                       documentTask.getId()));
                final File outputFile = pdfMerger.merge(documentTask.getBundle(), bundleFiles, coverPageFile);

                cdamService.uploadDocuments(outputFile, documentTask);

                log.info(String.format("Documents uploaded through CDAM for DocumentTask Id : #%d ",
                                       documentTask.getId()));
            } else {
                Map<BundleDocument, File> bundleFiles = dmStoreDownloader
                    .downloadFiles(documentTask.getBundle().getSortedDocuments())
                    .map(unchecked(documentConverter::convert))
                    .map(file -> pdfWatermark.processDocumentWatermark(documentImage, file, documentTask.getBundle().getDocumentImage()))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

                final File outputFile = pdfMerger.merge(documentTask.getBundle(), bundleFiles, coverPageFile);

                dmStoreUploader.uploadFile(outputFile, documentTask);
            }

            documentTask.setTaskState(TaskState.DONE);
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
        if (Objects.nonNull(documentTask.getId())) {
            log.info(String.format("Stitching completed for DocumentTask Id : #%d",
                                   documentTask.getId()));
        }
        return documentTask;
    }
}
