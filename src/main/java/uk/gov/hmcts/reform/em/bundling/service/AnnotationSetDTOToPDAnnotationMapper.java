package uk.gov.hmcts.reform.em.bundling.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import uk.gov.hmcts.reform.em.bundling.service.dto.external.annotation.AnnotationDTO;
import uk.gov.hmcts.reform.em.bundling.service.impl.DocumentTaskProcessingException;

import java.util.Set;

public interface AnnotationSetDTOToPDAnnotationMapper {

    void toNativeAnnotationsPerPage(PDDocument document, Set<AnnotationDTO> annotations) throws DocumentTaskProcessingException;


}
