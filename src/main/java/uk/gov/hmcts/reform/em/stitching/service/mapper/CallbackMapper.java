package uk.gov.hmcts.reform.em.stitching.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.reform.em.stitching.domain.Callback;
import uk.gov.hmcts.reform.em.stitching.service.dto.CallbackDto;

import java.util.List;

/**
 * Mapper for the entity
 */
@Mapper(componentModel = "spring", uses = {BundleMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CallbackMapper extends EntityMapper<CallbackDto, Callback> {

    Callback toEntity(CallbackDto dto);

    List<Callback> toEntity(List<CallbackDto> list);

    CallbackDto toDto(Callback message);

    List<CallbackDto> toDto(List<Callback> list);

}
