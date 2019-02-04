package uk.gov.hmcts.reform.em.stitching.batch;

import org.springframework.batch.item.ItemProcessor;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;


public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {

    private final DocumentTaskService documentTaskService;

    public DocumentTaskItemProcessor(DocumentTaskService documentTaskService) {
        this.documentTaskService = documentTaskService;
    }

    @Override
    public DocumentTask process(DocumentTask item) {
        documentTaskService.process(item);

        return item;
    }

}
