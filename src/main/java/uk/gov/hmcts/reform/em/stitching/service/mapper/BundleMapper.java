package uk.gov.hmcts.reform.em.stitching.service.mapper;

import org.mapstruct.Mapper;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DocumentTaskMapper.class})
public interface BundleMapper extends EntityMapper<BundleDTO, Bundle> {

    Bundle toEntity(BundleDTO messageDTO);

    BundleDTO toDto(Bundle message);

    List<Bundle> toEntity(List<BundleDTO> list);
    List<BundleDTO> toDto(List<Bundle> list);

}
