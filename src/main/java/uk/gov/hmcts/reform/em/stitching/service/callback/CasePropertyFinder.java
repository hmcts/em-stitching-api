package uk.gov.hmcts.reform.em.stitching.service.callback;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public interface CasePropertyFinder {

    Optional<ObjectNode> findCaseProperty(ObjectNode caseData, String propertyName);

}
