package uk.gov.hmcts.reform.em.stitching.service;

import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;

import java.util.Optional;

/**
 * Service Interface for managing DocumentTask.
 */
public interface DocumentTaskService {
    public static final int CURRENT_VERSION = 1;

    /**
     * Save a documentTask.
     *
     * @param documentTaskDTO the entity to save
     * @return the persisted entity
     */
    DocumentTaskDTO save(DocumentTaskDTO documentTaskDTO);

    /**
     * Get the "id" documentTask.
     *
     * @param id the id of the entity
     * @return the entity
     */
    Optional<DocumentTaskDTO> findOne(Long id);

    /**
     * Process a document task.
     *
     * @param documentTask task to process
     * @return updated dto
     */
    DocumentTask process(DocumentTask documentTask);

}
