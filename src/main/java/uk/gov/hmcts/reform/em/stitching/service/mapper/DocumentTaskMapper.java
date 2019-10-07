package uk.gov.hmcts.reform.em.stitching.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;

import java.util.List;

/**
 * Mapper for the entity DocumentTask and its DTO DocumentTaskDTO.
 */
@Mapper(componentModel = "spring", uses = {BundleMapper.class, CallbackMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentTaskMapper extends EntityMapper<DocumentTaskDTO, DocumentTask> {

    DocumentTask toEntity(DocumentTaskDTO messageDto);

    List<DocumentTask> toEntity(List<DocumentTaskDTO> list);

    DocumentTaskDTO toDto(DocumentTask message);

    List<DocumentTaskDTO> toDto(List<DocumentTask> list);

}
