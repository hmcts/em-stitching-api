package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import pl.touk.throwing.ThrowingConsumer;
import pl.touk.throwing.ThrowingFunction;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;

import java.io.File;

public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskItemProcessor.class);
    private final DmStoreDownloader dmStoreDownloader;
    private final DmStoreUploader dmStoreUploader;
    private final DocumentConversionService documentConverter;
    private final PDFMergerFactory pdfMergerFactory;
    private final DocumentFormatter documentFormatter;

    public DocumentTaskItemProcessor(DmStoreDownloader dmStoreDownloader,
                                     DmStoreUploader dmStoreUploader,
                                     DocumentConversionService documentConverter,
                                     PDFMergerFactory pdfMergerFactory,
                                     DocumentFormatter documentFormatter) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
        this.documentConverter = documentConverter;
        this.pdfMergerFactory = pdfMergerFactory;
        this.documentFormatter = documentFormatter;
    }

    @Override
    public DocumentTask process(DocumentTask item) {
        PDFMergerUtility merger = pdfMergerFactory.create();

        try {
            dmStoreDownloader
                .downloadFiles(item.getBundle().getSortedItems())
                .map(ThrowingFunction.unchecked(documentConverter::convert))
                .map(ThrowingFunction.unchecked(documentFormatter::addCoverSheetToDocument))
                .forEachOrdered(ThrowingConsumer.unchecked(merger::addSource));


            merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
            File outputFile = new File(merger.getDestinationFileName());

            dmStoreUploader.uploadFile(outputFile, item);

            item.setTaskState(TaskState.DONE);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);

            item.setTaskState(TaskState.FAILED);
            item.setFailureDescription(e.getMessage());
        }

        return item;
    }

}
