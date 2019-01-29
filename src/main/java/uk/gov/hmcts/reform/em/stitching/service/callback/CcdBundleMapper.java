package uk.gov.hmcts.reform.em.stitching.service.callback;

import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;

public interface CcdBundleMapper {

    Bundle map(ObjectNode ccdBundleData);

}
