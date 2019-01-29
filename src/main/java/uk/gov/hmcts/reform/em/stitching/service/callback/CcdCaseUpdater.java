package uk.gov.hmcts.reform.em.stitching.service.callback;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface CcdCaseUpdater {

    ObjectNode updateCase(ObjectNode bundleData);

}
