package uk.gov.hmcts.reform.em.stitching.service;

import uk.gov.hmcts.reform.em.stitching.service.dto.external.annotation.AnnotationSetDTO;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.File;

public interface PdfAnnotator {

    File annotatePdf(File file, AnnotationSetDTO annotationSetDTO) throws DocumentTaskProcessingException;

}
