package uk.gov.hmcts.reform.em.stitching.service;

import uk.gov.hmcts.reform.em.stitching.service.dto.StitchedDocumentDto;

import java.util.Optional;

public interface BundleService {

    /**
     * Get the "id" bundle.
     *
     * @param id the id of the entity
     * @return the entity
     */
    Optional<StitchedDocumentDto> findOne(Long id);
}
