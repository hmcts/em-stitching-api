package uk.gov.hmcts.reform.em.stitching.service;

import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.impl.FileAndMediaType;

import java.io.File;
import java.io.IOException;

public interface DocumentConversionService {

    Pair<BundleDocument, File> convert(Pair<BundleDocument, FileAndMediaType> originalFile) throws IOException;

}
