package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.convertObjectToJsonBytes;


public class SecureDocumentTaskScenarios extends BaseTest {

    private RequestSpecification request;
    private RequestSpecification unAuthenticatedRequest;
    private DocumentTaskDTO documentTask;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Before
    public void setupRequestSpecification() {
        request = testUtil
                .authRequest()
                .baseUri(testUtil.getTestUrl())
                .contentType(APPLICATION_JSON_VALUE);

        unAuthenticatedRequest = testUtil
                .unauthenticatedRequest()
                .baseUri(testUtil.getTestUrl())
                .contentType(APPLICATION_JSON_VALUE);

        documentTask = new DocumentTaskDTO();
        documentTask.setCaseTypeId(testUtil.getEnvCcdCaseTypeId());
        documentTask.setJurisdictionId("PUBLICLAW");
        documentTask.setServiceAuth(testUtil.getServiceAuth());
    }

    @Test
    public void testPostBundleStitch() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.hashToken"));
    }

    @Test
    public void testPostBundleStitchWithWordDoc() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundleWithWordDoc();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.hashToken"));
    }

    @Test
    public void testPostBundleStitchWithTextFile() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundleWithTextFile();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.hashToken"));
    }

    @Test
    public void testPostBundleStitchWithRichTextFile() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundleWithRichTextFile();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.hashToken"));
    }

    @Test
    public void testPostBundleStitchWithExcelAndPpt() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundleWithExcelAndPptDoc();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.hashToken"));
    }

    @Test
    public void testPostBundleStitchWithImage() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundleWithImage();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.hashToken"));
    }

    @Test
    public void testPostBundleStitchWithDocumentWatermarkImage() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundleWithWatermarkImage();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
    }

    @Test
    public void testPostDocumentTask() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundle();

        documentTask.setBundle(bundle);

        Response response =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, response.getStatusCode());
        assertEquals(response.getBody().jsonPath().getString("taskState"), TaskState.NEW.toString());
    }

    @Test
    public void testStitchTwoIdenticalDocuments() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundleWithDuplicateBundleDocuments();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        assertEquals(200, completedResponse.getStatusCode());
        assertNotNull(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        assertNotNull(completedResponse.getBody().jsonPath().getString("bundle.hashToken"));
    }

    @Test
    public void testStitchDocumentsWithSortIndices() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundleWithSortedDocuments();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        String stitchedDocumentUri = completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI");

        //We need to donwload the Stitched Document via Dm-Store and not via CDAM. As at this stage the document is
        // not yet associated to the case through CCD callBack.
        File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String parsedText = pdfStripper.getText(stitchedPdDocument);

        int indexOfDocument1 = parsedText.indexOf("Document 1");
        int indexOfDocument2 = parsedText.indexOf("Document 2");
        assertTrue(indexOfDocument2 < indexOfDocument1);
    }


    @Test
    public void testPostBundleStitchWithCallback() throws Exception {

        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);

        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl("https://postman-echo.com/post");

        documentTask.setCallback(callback);

        Response createTaskResponse =
                request
                        .log().all()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");
        assertEquals(201, createTaskResponse.getStatusCode());
        assertEquals("https://postman-echo.com/post",
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
        assertEquals(400, createTaskResponse.getStatusCode());
        assertTrue(createTaskResponse.body().asString().contains("Error saving Document Task"));
        assertTrue(createTaskResponse.getBody().jsonPath().getString("detail")
            .contains("ERROR: value too long for type character varying(255)"));

    }

    @Test
    public void testPostBundleStitchWithCallbackUrlNotAccessible() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundle();
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
        assertEquals(400, createTaskResponse.getStatusCode());
        assertEquals("callback.callbackUrl",
                createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].field"));
        assertEquals("Connection to the callback URL could not be verified.",
                createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].message"));

    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserPostBundleStitch() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);

        unAuthenticatedRequest
                .body(convertObjectToJsonBytes(documentTask))
                .post("/api/document-tasks")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void shouldReturn404WhenGetDocumentTaskWithNonExistentId() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);
        request
                .body(convertObjectToJsonBytes(documentTask))
                .post("/api/document-tasks")
                .then().log().all()
                .assertThat()
                .statusCode(201);

        final long nonExistentId = Long.MAX_VALUE;
        final String taskUrl = "/api/document-tasks/" + nonExistentId;
        request
                .get(taskUrl)
                .then().log().all()
                .assertThat()
                .statusCode(404);
    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserGetDocumentTask() throws Exception {
        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);

        final String documentTaskId =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks")
                        .then()
                        .assertThat()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        final String taskUrl = "/api/document-tasks/" + documentTaskId;
        unAuthenticatedRequest
                .get(taskUrl)
                .then().log().all()
                .assertThat()
                .statusCode(401);
    }
}
