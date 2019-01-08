package uk.gov.hmcts.reform.em.stitching.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import uk.gov.hmcts.reform.em.stitching.service.dto.external.annotation.AnnotationDTO;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.util.Set;

public interface AnnotationSetDTOToPDAnnotationMapper {

    void toNativeAnnotationsPerPage(PDDocument document, Set<AnnotationDTO> annotations) throws DocumentTaskProcessingException;


}
