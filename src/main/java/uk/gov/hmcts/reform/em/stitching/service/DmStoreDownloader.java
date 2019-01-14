package uk.gov.hmcts.reform.em.stitching.service;

import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.File;
import java.util.List;

public interface DmStoreDownloader {

    List<File> downloadFiles(List<String> id) throws DocumentTaskProcessingException;

}
