package uk.gov.hmcts.reform.em.stitching.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;


public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskItemProcessor.class);
    private final DocumentTaskService documentTaskService;

    public DocumentTaskItemProcessor(DocumentTaskService documentTaskService) {
        this.documentTaskService = documentTaskService;
    }

    @Override
    public DocumentTask process(DocumentTask item) {
        log.info("Processing Task: " + item.getId());

        documentTaskService.process(item);

        return item;
    }

}
