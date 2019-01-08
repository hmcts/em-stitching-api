package uk.gov.hmcts.reform.em.stitching.service;

import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.File;

public interface DmStoreDownloader {

    File downloadFile(String id) throws DocumentTaskProcessingException;

}
