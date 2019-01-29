package uk.gov.hmcts.reform.em.stitching.service.callback;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface CcdCallbackHandlerService {

    ObjectNode handleCddCallback(CcdCallbackDto caseData, CcdCaseUpdater ccdCaseUpdater);

}
