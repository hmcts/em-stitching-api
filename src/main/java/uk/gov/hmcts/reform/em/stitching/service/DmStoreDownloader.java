package uk.gov.hmcts.reform.em.stitching.service;

import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.impl.FileAndMediaType;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.util.stream.Stream;

public interface DmStoreDownloader {

    Stream<Pair<BundleDocument, FileAndMediaType>> downloadFiles(Stream<BundleDocument> bundleDocuments)
        throws DocumentTaskProcessingException;
}
