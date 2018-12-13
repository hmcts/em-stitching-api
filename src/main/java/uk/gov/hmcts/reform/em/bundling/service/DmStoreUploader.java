package uk.gov.hmcts.reform.em.bundling.service;

import uk.gov.hmcts.reform.em.bundling.domain.DocumentTask;
import uk.gov.hmcts.reform.em.bundling.service.impl.DocumentTaskProcessingException;

import java.io.File;

public interface DmStoreUploader {

    void uploadFile(File file, DocumentTask documentTask) throws DocumentTaskProcessingException;;

}
