package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.info.BuildInfo;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.util.Optional;

/**
 * Service Implementation for managing DocumentTask.
 */
@Service
public class DocumentTaskServiceImpl implements DocumentTaskService {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskServiceImpl.class);
    private final DocumentTaskRepository documentTaskRepository;
    private final DocumentTaskMapper documentTaskMapper;
    private final BuildInfo buildInfo;

    public DocumentTaskServiceImpl(DocumentTaskRepository documentTaskRepository,
                                   DocumentTaskMapper documentTaskMapper,
                                   BuildInfo buildInfo) {
        this.documentTaskRepository = documentTaskRepository;
        this.documentTaskMapper = documentTaskMapper;
        this.buildInfo = buildInfo;
    }

    /**
     * Save a documentTask. Saving the task also sets the version - bear in mind that if a task is loaded as one version it may
     * be saved back as a different (more recent) version as the model will be updated.
     *
     * @param documentTaskDto the entity to save
     * @return the persisted entity
     */
    @Override
    @Transactional
    public DocumentTaskDTO save(DocumentTaskDTO documentTaskDto) {
        log.debug("Request to save DocumentTask : {}", documentTaskDto);
        DocumentTask documentTask = documentTaskMapper.toEntity(documentTaskDto);
        documentTask.setVersion(buildInfo.getBuildNumber());
        documentTask = documentTaskRepository.save(documentTask);

        return documentTaskMapper.toDto(documentTask);
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

}
