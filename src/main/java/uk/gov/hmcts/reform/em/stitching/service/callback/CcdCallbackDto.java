package uk.gov.hmcts.reform.em.stitching.service.callback;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class CcdCallbackDto {

    private String propertyName;

    private ObjectNode caseData;

    private String jwt;

    public ObjectNode getCaseData() {
        return caseData;
    }

    public String getJwt() {
        return jwt;
    }

    public void setCaseData(ObjectNode caseData) {
        this.caseData = caseData;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
}
