package uk.gov.hmcts.reform.em.stitching.batch;

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
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;
import uk.gov.hmcts.reform.em.stitching.template.DocmosisClient;

import java.io.File;
import java.util.Map;
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

    public DocumentTaskItemProcessor(DmStoreDownloader dmStoreDownloader,
                                     DmStoreUploader dmStoreUploader,
                                     DocumentConversionService documentConverter,
                                     PDFMerger pdfMerger,
                                     DocmosisClient docmosisClient) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
        this.documentConverter = documentConverter;
        this.pdfMerger = pdfMerger;
        this.docmosisClient = docmosisClient;
    }

    @Override
    public DocumentTask process(DocumentTask documentTask) {
        try {
            final File coverPageFile = StringUtils.isNotBlank(documentTask.getBundle().getCoverpageTemplate())
                ? docmosisClient.renderDocmosisTemplate(
                documentTask.getBundle().getCoverpageTemplate(),
                documentTask.getBundle().getCoverpageTemplateData()) : null;

            final File documentImage = documentTask.getBundle().getDocumentImage() != null
                    && documentTask.getBundle().getDocumentImage().getEnabled()
                    ? docmosisClient.getDocmosisTemplate(
                    documentTask.getBundle().getDocumentImage().getDocmosisAssetId(),
                    documentTask.getBundle().getDocumentImage().getImageCoordinates().getFirst(),
                    documentTask.getBundle().getDocumentImage().getImageCoordinates().getSecond())
                    : null;

            Map<BundleDocument, File> bundleFiles = dmStoreDownloader
                .downloadFiles(documentTask.getBundle().getSortedDocuments())
                .map(unchecked(documentConverter::convert))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

            final File outputFile = pdfMerger.merge(documentTask.getBundle(), bundleFiles, coverPageFile, documentImage);

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
