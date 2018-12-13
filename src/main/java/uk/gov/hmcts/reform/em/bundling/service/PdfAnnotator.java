package uk.gov.hmcts.reform.em.bundling.service;

import uk.gov.hmcts.reform.em.bundling.service.dto.external.annotation.AnnotationSetDTO;
import uk.gov.hmcts.reform.em.bundling.service.impl.DocumentTaskProcessingException;

import java.io.File;

public interface PdfAnnotator {

    File annotatePdf(File file, AnnotationSetDTO annotationSetDTO) throws DocumentTaskProcessingException;

}
