package uk.gov.hmcts.reform.em.bundling.service;

import uk.gov.hmcts.reform.em.bundling.service.dto.external.annotation.AnnotationSetDTO;
import uk.gov.hmcts.reform.em.bundling.service.impl.DocumentTaskProcessingException;

public interface AnnotationSetFetcher {

    AnnotationSetDTO fetchAnnotationSet(String documentId, String jwt) throws DocumentTaskProcessingException;;

}
