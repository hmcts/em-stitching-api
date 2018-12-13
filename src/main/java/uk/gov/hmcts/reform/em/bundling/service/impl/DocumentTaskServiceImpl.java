package uk.gov.hmcts.reform.em.bundling.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.bundling.batch.DocumentTaskItemProcessor;
import uk.gov.hmcts.reform.em.bundling.domain.DocumentTask;
import uk.gov.hmcts.reform.em.bundling.repository.DocumentTaskRepository;
import uk.gov.hmcts.reform.em.bundling.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.bundling.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.bundling.service.mapper.DocumentTaskMapper;

import java.util.Optional;

/**
 * Service Implementation for managing DocumentTask.
 */
@Service
@Transactional
public class DocumentTaskServiceImpl implements DocumentTaskService {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskServiceImpl.class);

    private final DocumentTaskRepository documentTaskRepository;

    private final DocumentTaskMapper documentTaskMapper;

    private final DocumentTaskItemProcessor documentTaskItemProcessor;


    public DocumentTaskServiceImpl(DocumentTaskRepository documentTaskRepository, DocumentTaskMapper documentTaskMapper, DocumentTaskItemProcessor documentTaskItemProcessor) {
        this.documentTaskRepository = documentTaskRepository;
        this.documentTaskMapper = documentTaskMapper;
        this.documentTaskItemProcessor = documentTaskItemProcessor;
    }

    /**
     * Save a documentTask.
     *
     * @param documentTaskDTO the entity to save
     * @return the persisted entity
     */
    @Override
    public DocumentTaskDTO save(DocumentTaskDTO documentTaskDTO) {
        log.debug("Request to save DocumentTask : {}", documentTaskDTO);
        DocumentTask documentTask = documentTaskMapper.toEntity(documentTaskDTO);
        documentTask = documentTaskRepository.save(documentTask);
        documentTaskItemProcessor.process(documentTask);
        return documentTaskMapper.toDto(documentTask);
    }

    /**
     * Get all the documentTasks.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentTaskDTO> findAll(Pageable pageable) {
        log.debug("Request to get all DocumentTasks");
        return documentTaskRepository.findAll(pageable)
            .map(documentTaskMapper::toDto);
    }


    /**
     * Get one documentTask by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentTaskDTO> findOne(Long id) {
        log.debug("Request to get DocumentTask : {}", id);
        return documentTaskRepository.findById(id)
            .map(documentTaskMapper::toDto);
    }

    /**
     * Delete the documentTask by id.
     *
     * @param id the id of the entity
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete DocumentTask : {}", id);
        documentTaskRepository.deleteById(id);
    }
}
