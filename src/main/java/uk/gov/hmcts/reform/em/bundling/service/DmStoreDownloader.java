package uk.gov.hmcts.reform.em.bundling.service;

import uk.gov.hmcts.reform.em.bundling.service.impl.DocumentTaskProcessingException;

import java.io.File;

public interface DmStoreDownloader {

    File downloadFile(String id) throws DocumentTaskProcessingException;

}
