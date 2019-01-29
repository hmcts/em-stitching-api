package uk.gov.hmcts.reform.em.stitching.service.callback.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.batch.DocumentTaskItemProcessor;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.callback.CcdBundleMapper;
import uk.gov.hmcts.reform.em.stitching.service.callback.CcdCaseUpdater;

import javax.transaction.Transactional;

@Service
@Transactional
public class CcdBundleStitchingService implements CcdCaseUpdater {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskItemProcessor.class);

    private CcdBundleMapper ccdBundleMapper;

    private DocumentTaskItemProcessor documentTaskItemProcessor;

    private String stitchedDocumentURIProperty = "stitchedDocumentURI";

    public CcdBundleStitchingService(CcdBundleMapper ccdBundleMapper, DocumentTaskItemProcessor documentTaskItemProcessor) {
        this.ccdBundleMapper = ccdBundleMapper;
        this.documentTaskItemProcessor = documentTaskItemProcessor;
    }

    @Override
    public ObjectNode updateCase(ObjectNode bundleData) {

        Bundle bundle = ccdBundleMapper.map(bundleData);

        DocumentTask documentTask = new DocumentTask();
        documentTask.setBundle(bundle);

        documentTaskItemProcessor.process(documentTask);

        if (isTaskComplete(documentTask)) {
            bundleData.put(stitchedDocumentURIProperty, documentTask.getBundle().getStitchedDocumentURI());
        } else {
            log.error("CCD bundle could not be stitched: {}", documentTask);
        }

        return bundleData;
    }

    private boolean isTaskComplete(DocumentTask documentTask) {
        return TaskState.DONE.equals(documentTask.getTaskState()) &&
                StringUtils.isNotBlank(documentTask.getBundle().getStitchedDocumentURI());
    }
}
