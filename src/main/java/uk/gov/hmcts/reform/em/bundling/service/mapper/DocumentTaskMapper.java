package uk.gov.hmcts.reform.em.bundling.service.mapper;

import org.mapstruct.Mapper;
import uk.gov.hmcts.reform.em.bundling.domain.DocumentTask;
import uk.gov.hmcts.reform.em.bundling.service.dto.DocumentTaskDTO;

/**
 * Mapper for the entity DocumentTask and its DTO DocumentTaskDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface DocumentTaskMapper extends EntityMapper<DocumentTaskDTO, DocumentTask> {



    default DocumentTask fromId(Long id) {
        if (id == null) {
            return null;
        }
        DocumentTask documentTask = new DocumentTask();
        documentTask.setId(id);
        return documentTask;
    }
}
