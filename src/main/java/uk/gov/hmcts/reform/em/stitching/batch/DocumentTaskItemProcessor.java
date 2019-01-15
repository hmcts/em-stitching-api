package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.UUID;

import static pl.touk.throwing.ThrowingConsumer.unchecked;

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
        PDFMergerUtility merger = this.createPDFMerger();

        try {
            dmStoreDownloader
                .downloadFiles(item.getBundle().getDocuments())
                .forEach(unchecked(merger::addSource));

            merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
            File outputFile = new File(merger.getDestinationFileName());

            dmStoreUploader.uploadFile(outputFile, item);

            item.setOutputDocumentId(merger.getDestinationFileName());
            item.setTaskState(TaskState.DONE);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);

            item.setTaskState(TaskState.FAILED);
            item.setFailureDescription(e.getMessage());
        }

        return item;
    }

    private PDFMergerUtility createPDFMerger() {
        String filename = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + "-stitched.pdf";
        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        pdfMergerUtility.setDestinationFileName(filename);

        return pdfMergerUtility;
    }

}
