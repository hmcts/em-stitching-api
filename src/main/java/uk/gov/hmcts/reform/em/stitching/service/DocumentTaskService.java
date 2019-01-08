package uk.gov.hmcts.reform.em.stitching.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;

import java.util.Optional;

/**
 * Service Interface for managing DocumentTask.
 */
public interface DocumentTaskService {

    /**
     * Save a documentTask.
     *
     * @param documentTaskDTO the entity to save
     * @return the persisted entity
     */
    DocumentTaskDTO save(DocumentTaskDTO documentTaskDTO);

    /**
     * Get all the documentTasks.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    Page<DocumentTaskDTO> findAll(Pageable pageable);


    /**
     * Get the "id" documentTask.
     *
     * @param id the id of the entity
     * @return the entity
     */
    Optional<DocumentTaskDTO> findOne(Long id);

    /**
     * Delete the "id" documentTask.
     *
     * @param id the id of the entity
     */
    void delete(Long id);
}
