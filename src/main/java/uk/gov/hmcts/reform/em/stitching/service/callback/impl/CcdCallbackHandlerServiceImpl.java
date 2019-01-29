package uk.gov.hmcts.reform.em.stitching.service.callback.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.applicationinsights.core.dependencies.gson.JsonObject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.service.callback.CasePropertyFinder;
import uk.gov.hmcts.reform.em.stitching.service.callback.CcdCaseUpdater;
import uk.gov.hmcts.reform.em.stitching.service.callback.CcdCallbackHandlerService;
import uk.gov.hmcts.reform.em.stitching.service.callback.CcdCallbackDto;

import javax.transaction.Transactional;

@Service
@Transactional
public class CcdCallbackHandlerServiceImpl implements CcdCallbackHandlerService {

    private CasePropertyFinder casePropertyFinder;

    public CcdCallbackHandlerServiceImpl(CasePropertyFinder casePropertyFinder) {
        this.casePropertyFinder = casePropertyFinder;
    }

    @Override
    public ObjectNode handleCddCallback(CcdCallbackDto cmd, CcdCaseUpdater ccdCaseUpdater) {
        return casePropertyFinder
            .findCaseProperty(cmd.getCaseData(), cmd.getPropertyName())
                .map( caseProperty -> {
                    ObjectNode updatedCaseProperty = ccdCaseUpdater.updateCase(caseProperty);
                    cmd.getCaseData().replace(cmd.getPropertyName(), updatedCaseProperty);
                    return cmd.getCaseData();
                })
                .orElse(cmd.getCaseData());

    }
}
