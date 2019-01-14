package uk.gov.hmcts.reform.em.stitching.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.File;

public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskItemProcessor.class);

    private final DmStoreDownloader dmStoreDownloader;
    private final DmStoreUploader dmStoreUploader;


    public DocumentTaskItemProcessor(DmStoreDownloader dmStoreDownloader, DmStoreUploader dmStoreUploader) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
    }

    @Override
    public DocumentTask process(DocumentTask item) {

        try {

            File originalFile = dmStoreDownloader.downloadFile(item.getOutputDocumentId());
//
//            AnnotationSetDTO annotationSetDTO = annotationSetFetcher.fetchAnnotationSet(item.getBundle(), item.getJwt());
//
//            File annotatedPdf = pdfAnnotator.annotatePdf(originalFile, annotationSetDTO);
//
//            dmStoreUploader.uploadFile(annotatedPdf, item);

            item.setTaskState(TaskState.DONE);

        } catch (DocumentTaskProcessingException e) {
            log.error(e.getMessage(), e);

            item.setTaskState(TaskState.FAILED);

            item.setFailureDescription(e.getMessage());

        }

        return item;

    }

}
