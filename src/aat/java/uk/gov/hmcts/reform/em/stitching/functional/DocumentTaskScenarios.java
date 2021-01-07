package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.CallbackDto;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.convertObjectToJsonBytes;


public class DocumentTaskScenarios extends BaseTest {

    private RequestSpecification request;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Before
    public void setupRequestSpecification() {
        request = testUtil
                .authRequest()
                .baseUri(testUtil.getTestUrl())
                .contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    public void testPostBundleStitch() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

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

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithTextFile() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithTextFile();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithRichTextFile() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithRichTextFile();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

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

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

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

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithDocumentWatermarkImage() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithWatermarkImage();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
    }

    @Test
    public void testPostDocumentTask() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();

        documentTask.setBundle(bundle);

        Response response =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        Assert.assertEquals(201, response.getStatusCode());
        Assert.assertEquals(response.getBody().jsonPath().getString("taskState"), TaskState.NEW.toString());
    }

    @Test
    public void testStitchTwoIdenticalDocuments() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithDuplicateBundleDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

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

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

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

        Response createTaskResponse =
                request
                        .log().all()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");
        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        Assert.assertEquals("https://postman-echo.com/post",
                createTaskResponse.getBody().jsonPath().getString("callback.callbackUrl"));

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        testUtil.pollUntil(taskUrl, body -> body.getString("callback.callbackState").equals("SUCCESS"));

    }

    @Test
    public void testPostBundleStitchWithCallbackForFailure() throws IOException {
        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl("https://postman-echo.com/post");
        callback.setCreatedBy("callback_dummy1");
        callback.setCreatedDate(Instant.now());
        callback.setLastModifiedBy("callback_dummmy2");
        callback.setLastModifiedDate(Instant.now());

        BundleDTO bundle = testUtil.getTestBundleforFailure();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        documentTask.setCallback(callback);
        documentTask.setCreatedBy("documentTask_Dummy1");
        documentTask.setLastModifiedBy("documentTask_Dummy2");
        documentTask.setCreatedDate(Instant.now());
        documentTask.setLastModifiedDate(Instant.now());

        Response createTaskResponse =
                request
                        .log().all()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");
        Assert.assertEquals(400, createTaskResponse.getStatusCode());
        Assert.assertTrue(createTaskResponse.body().asString().contains("Error saving Document Task"));
        Assert.assertTrue(createTaskResponse.body().asString().contains("Caused by ConstraintViolationException"));

    }

    @Test
    public void testPostBundleStitchWithCallbackUrlNotAccessible() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl("http://localhost:80899/my/callback/resource");

        documentTask.setCallback(callback);

        Response createTaskResponse =
                request
                        .log().all()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        createTaskResponse.prettyPrint();
        Assert.assertEquals(400, createTaskResponse.getStatusCode());
        Assert.assertEquals("callback.callbackUrl",
                createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].field"));
        Assert.assertEquals("Connection to the callback URL could not be verified.",
                createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].message"));

    }
}
