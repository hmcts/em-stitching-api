package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.mapper.BundleMapper;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;
import uk.gov.hmcts.reform.em.stitching.testutil.Env;

public class DocumentTaskScenarios {

    TestUtil testUtil = new TestUtil();

    @Autowired
    BundleMapper bundleMapper;

    @Test
    public void testPostDocumentTaskNotEmptyAnnotationSet() {
        Bundle bundle = testUtil.getTestBundle();
        BundleDTO dto  = bundleMapper.toDto(bundle);
        String newDocId = testUtil.uploadDocument();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("bundle", dto);

        Response response = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, response.getStatusCode());
        Assert.assertEquals( response.getBody().jsonPath().getString("bundle"), newDocId);
        Assert.assertEquals( response.getBody().jsonPath().getString("taskState"), TaskState.NEW.toString());
    }

}
