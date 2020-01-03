package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.CallbackDto;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.io.File;
import java.io.IOException;


public class DocumentTaskScenarios extends BaseTest {

    @Test
    public void testPostBundleStitch() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithWordDoc() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithWordDoc();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithExcelAndPpt() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithExcelAndPptDoc();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
                .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithImage() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithImage();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostDocumentTask() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();

        documentTask.setBundle(bundle);

        Response response = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
                .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, response.getStatusCode());
        Assert.assertEquals(response.getBody().jsonPath().getString("taskState"), TaskState.NEW.toString());
    }

    @Test
    public void testStitchTwoIdenticalDocuments() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithDuplicateBundleDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
                .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, completedResponse.getStatusCode());
        Assert.assertNotNull(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testStitchDocumentsWithSortIndices() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithSortedDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
                .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        String stitchedDocumentUri = completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI");
        File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String parsedText = pdfStripper.getText(stitchedPdDocument);

        int indexOfDocument1 = parsedText.indexOf("Document 1");
        int indexOfDocument2 = parsedText.indexOf("Document 2");
        Assert.assertTrue(indexOfDocument2 < indexOfDocument1);
    }


    @Test
    public void testPostBundleStitchWithCallback() throws IOException, InterruptedException {

        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl("https://postman-echo.com/post");

        documentTask.setCallback(callback);

        Response createTaskResponse = testUtil.authRequest()
                .log().all()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
                .request("POST", testUtil.getTestUrl() + "/api/document-tasks");
        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        Assert.assertEquals("https://postman-echo.com/post",
                createTaskResponse.getBody().jsonPath().getString("callback.callbackUrl"));

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        testUtil.pollUntil(taskUrl, body -> body.getString("callback.callbackState").equals("SUCCESS"));

    }

    @Test
    public void testPostBundleStitchWithCallbackUrlNotAccessible() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl("http://localhost:80899/my/callback/resource");

        documentTask.setCallback(callback);

        Response createTaskResponse = testUtil.authRequest()
                .log().all()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
                .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        createTaskResponse.prettyPrint();
        Assert.assertEquals(400, createTaskResponse.getStatusCode());
        Assert.assertEquals("callback.callbackUrl",
                createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].field"));
        Assert.assertEquals("Connection to the callback URL could not be verified.",
                createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].message"));

    }
}
