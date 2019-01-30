package uk.gov.hmcts.reform.em.stitching.service.callback;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;

import java.util.List;

public interface CcdBundleMapper {

    BundleDTO jsonToBundle(JsonNode ccdBundleData);

    JsonNode bundleToDto(Bundle bundle);

}
