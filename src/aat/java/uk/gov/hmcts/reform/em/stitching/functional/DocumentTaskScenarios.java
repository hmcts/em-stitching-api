package uk.gov.hmcts.reform.em.stitching.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;
import uk.gov.hmcts.reform.em.stitching.testutil.Env;

import java.io.File;
import java.io.IOException;

public class DocumentTaskScenarios {

    TestUtil testUtil = new TestUtil();

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testPostBundleStitch() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        System.out.println("JJJ - testPostBundleStitch - request body");
        System.out.println(mapper.writeValueAsString(documentTask));

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(mapper.writeValueAsString(documentTask))
            .request("POST", Env.getTestUrl() + "/api/document-tasks");

        System.out.println("JJJ - testPostBundleStitch - responsebody - taskState = New");
        System.out.println(createTaskResponse.getBody().jsonPath().prettyPrint());

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        System.out.println("JJJ - testPostBundleStitch - responsebody - taskState = Done");
        System.out.println(getTaskResponse.getBody().jsonPath().prettyPrint());
        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithWordDoc() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithWordDoc();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        System.out.println("JJJ - testPostBundleStitchWithWordDoc - requestbody");
        System.out.println(mapper.writeValueAsString(documentTask));

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(mapper.writeValueAsString(documentTask))
            .request("POST", Env.getTestUrl() + "/api/document-tasks");

        System.out.println("JJJ - testPostBundleStitchWithWordDoc - responseBody - taskState = New");
        System.out.println(createTaskResponse.getBody().jsonPath().prettyPrint());
        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        System.out.println("JJJ - testPostBundleStitchWithWordDoc - responseBody - taskState = Done");
        System.out.println(getTaskResponse.getBody().jsonPath().prettyPrint());
        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithImage() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithImage();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        System.out.println("JJJ - testPostBundleStitchWithImage - request body");
        System.out.println(mapper.writeValueAsString(documentTask));

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(mapper.writeValueAsString(documentTask))
            .request("POST", Env.getTestUrl() + "/api/document-tasks");

        System.out.println("JJJ - testPostBundleStitchWithImage - responseBody - taskState = New");
        System.out.println(createTaskResponse.getBody().jsonPath().prettyPrint());
        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        System.out.println("JJJ - testPostBundleStitchWithImage - responseBody - taskState = Done");
        System.out.println(getTaskResponse.getBody().jsonPath().prettyPrint());
        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostDocumentTask() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();

        documentTask.setBundle(bundle);
        System.out.println("JJJ - testPostDocumentTask - request body");
        System.out.println(mapper.writeValueAsString(documentTask));

        Response response = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        System.out.println("JJJ - testPostDocumentTask - responseBody");
        System.out.println(response.getBody().jsonPath().prettyPrint());
        Assert.assertEquals(201, response.getStatusCode());
        Assert.assertEquals(response.getBody().jsonPath().getString("taskState"), TaskState.NEW.toString());
    }

    @Test
    public void testStitchTwoIdenticalDocuments() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithDuplicateBundleDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        System.out.println("JJJ - testStitchTwoIdenticalDocuments - request body");
        System.out.println(mapper.writeValueAsString(documentTask));

        Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        System.out.println("JJJ - testStitchTwoIdenticalDocuments - response body - taskstate = New");
        System.out.println(createTaskResponse.getBody().jsonPath().prettyPrint());
        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        System.out.println("JJJ - testStitchTwoIdenticalDocuments- response body - taskState - Done");
        System.out.println(completedResponse.getBody().jsonPath().prettyPrint());
        Assert.assertEquals(200, completedResponse.getStatusCode());
        Assert.assertNotNull(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testStitchDocumentsWithSortIndices() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithSortedDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        System.out.println("JJJ - testStitchDocumentsWithSortIndices - request body");
        System.out.println(mapper.writeValueAsString(documentTask));

        Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        System.out.println("JJJ - testStitchDocumentsWithSortIndices - responsebody - taskstate = NEW");
        System.out.println(createTaskResponse.getBody().jsonPath().prettyPrint());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        System.out.println("JJJ - testStitchDocumentsWithSortIndices - responsebody - taskstate = DONE");
        System.out.println(completedResponse.getBody().jsonPath().prettyPrint());
        BundleDocument stitchedDocument = new BundleDocument();
        stitchedDocument.setDocumentURI(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        File stitchedFile = testUtil.downloadDocument(stitchedDocument.getDocumentURI());

        PDDocument stitchedDocumentText = PDDocument.load(stitchedFile);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String parsedText = pdfStripper.getText(stitchedDocumentText);

        int indexOfDocument1 = parsedText.indexOf("Document 1");
        int indexOfDocument2 = parsedText.indexOf("Document 2");
        Assert.assertTrue(indexOfDocument2 < indexOfDocument1);
    }



}
