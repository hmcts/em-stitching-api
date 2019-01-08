package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;
import uk.gov.hmcts.reform.em.stitching.testutil.Env;

import static org.hamcrest.CoreMatchers.*;

import java.io.File;
import java.util.UUID;

public class DocumentTaskScenarios {

    TestUtil testUtil = new TestUtil();

    @Test
    public void testGetDocumentTasks() throws Exception {

        testUtil.authRequest()
            .request("GET", Env.getTestUrl() + "/api/document-tasks")
        .then()
            .statusCode(200);

    }

    @Test
    public void testPostDocumentTaskDocumentNotFound() throws Exception {

        UUID nonExistentDocumentId = UUID.randomUUID();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", nonExistentDocumentId);

        testUtil
            .authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", Env.getTestUrl() + "/api/document-tasks")
            .then()
                .statusCode(201)
                .body("inputDocumentId", equalTo(nonExistentDocumentId.toString()))
                .body("taskState", equalTo(TaskState.FAILED.toString()))
                .body("failureDescription", equalTo("Could not access the binary. HTTP response: 404"));

    }

    @Test
    public void testPostDocumentTaskAnnotationSetNotFound() throws Exception {

        String newDocId = testUtil.uploadDocument();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", newDocId);

        testUtil
            .authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", Env.getTestUrl() + "/api/document-tasks")
            .then()
                .statusCode(201)
                .body("inputDocumentId", equalTo(newDocId))
                .body("taskState", equalTo(TaskState.FAILED.toString()))
                .body("failureDescription", startsWith("Could not access the annotation set."));

    }

    @Test
    public void testPostDocumentTaskEmptyAnnotationSet() throws Exception {

        String newDocId = testUtil.uploadDocument();

        testUtil.createAnnotationSetForDocumentId(newDocId);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", newDocId);

        testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonObject)
            .request("POST", Env.getTestUrl() + "/api/document-tasks")
            .then()
            .statusCode(201)
            .body("inputDocumentId", equalTo(newDocId))
            .body("taskState", equalTo(TaskState.DONE.toString()));

    }

    @Test
    public void testPostDocumentTaskNotEmptyAnnotationSet() throws Exception {

        String newDocId = testUtil.uploadDocument();

        String annotationSetId = testUtil.createAnnotationSetForDocumentId(newDocId);

        testUtil.saveAnnotation(annotationSetId);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", newDocId);

        Response response = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, response.getStatusCode());
        Assert.assertEquals( response.getBody().jsonPath().getString("inputDocumentId"), newDocId);
        Assert.assertEquals( response.getBody().jsonPath().getString("taskState"), TaskState.DONE.toString());

        File file = testUtil.getDocumentBinary(response.getBody().jsonPath().getString("outputDocumentId"));

        PDDocument pdDocument = PDDocument.load(file);

        PDPage page = pdDocument.getPage(0);

        Assert.assertNotNull(page.getAnnotations());

    }

}
