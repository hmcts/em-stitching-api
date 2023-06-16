package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


public class DocumentTaskScenarios extends BaseTest {

    private RequestSpecification request;
    private RequestSpecification unAuthenticatedRequest;
    private static final String END_POINT = "/api/document-tasks";
    private static final String CALL_BACK_URL = "https://postman-echo.com/post";
    private static final String TASK_STATE = "taskState";
    private static final String BUNDLE_S_DOC_URI = "bundle.stitchedDocumentURI";

    private final Logger log = LoggerFactory.getLogger(DocumentTaskScenarios.class);

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
    }

    @Test
    public void testPostBundleStitch() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(BUNDLE_S_DOC_URI));
    }

    @Test
    public void testPostBundleStitchWithCaseId() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        String testCaseId = "TestCaseId967";
        documentTask.setCaseId(testCaseId);


        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        assertEquals(201, createTaskResponse.getStatusCode());
        assertEquals(testCaseId, createTaskResponse.getBody().jsonPath().getString("caseId"));
        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(BUNDLE_S_DOC_URI));

    }

    @Test
    public void testPostBundleStitchWithWordDoc() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithWordDoc();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(BUNDLE_S_DOC_URI));
    }

    @Test
    public void testPostBundleStitchWithTextFile() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithTextFile();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(BUNDLE_S_DOC_URI));
    }

    @Test
    public void testPostBundleStitchWithRichTextFile() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithRichTextFile();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(BUNDLE_S_DOC_URI));
    }

    @Test
    public void testPostBundleStitchWithExcelAndPpt() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithExcelAndPptDoc();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(BUNDLE_S_DOC_URI));
    }

    @Test
    public void testPostBundleStitchWithImage() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithImage();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(BUNDLE_S_DOC_URI));
    }

    @Test
    public void testPostBundleStitchWithDocumentWatermarkImage() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithWatermarkImage();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
    }

    @Test
    public void testPostDocumentTask() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();

        documentTask.setBundle(bundle);

        Response response = request.body(convertObjectToJsonBytes(documentTask)).post(END_POINT);

        assertEquals(201, response.getStatusCode());
        assertEquals(response.getBody().jsonPath().getString(TASK_STATE), TaskState.NEW.toString());
    }

    @Test
    public void testStitchTwoIdenticalDocuments() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithDuplicateBundleDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        assertEquals(200, completedResponse.getStatusCode());
        assertNotNull(completedResponse.getBody().jsonPath().getString(BUNDLE_S_DOC_URI));
    }

    @Test
    public void testStitchDocumentsWithSortIndices() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithSortedDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);

        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE).equals("DONE"));

        String stitchedDocumentUri = completedResponse.getBody().jsonPath().getString(BUNDLE_S_DOC_URI);
        File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String parsedText = pdfStripper.getText(stitchedPdDocument);

        stitchedPdDocument.close();

        int indexOfDocument1 = parsedText.indexOf("Document 1");
        int indexOfDocument2 = parsedText.indexOf("Document 2");
        assertTrue(indexOfDocument2 < indexOfDocument1);
    }


    @Test
    public void testPostBundleStitchWithCallback() throws IOException, InterruptedException {

        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl(CALL_BACK_URL);

        documentTask.setCallback(callback);

        Response createTaskResponse =
                request
                        .log().all()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT);
        assertEquals(201, createTaskResponse.getStatusCode());
        assertEquals(CALL_BACK_URL,
                createTaskResponse.getBody().jsonPath().getString("callback.callbackUrl"));

        String taskUrl = END_POINT + "/" + createTaskResponse.getBody().jsonPath().getString("id");
        testUtil.pollUntil(taskUrl, body -> body.getString("callback.callbackState").equals("SUCCESS"));

    }

    @Test
    public void testPostBundleStitchWithCallbackForFailure() throws IOException {
        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl(CALL_BACK_URL);
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
                        .post(END_POINT);

        log.info("body {}", createTaskResponse.getBody());
        assertEquals(400, createTaskResponse.getStatusCode());
        assertTrue(createTaskResponse.body().asString().contains("Error saving Document Task"));
        assertTrue(createTaskResponse.getBody().jsonPath().getString("detail")
                .contains("ERROR: value too long for type character varying(255)"));
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
                        .post(END_POINT);

        createTaskResponse.prettyPrint();
        assertEquals(400, createTaskResponse.getStatusCode());
        assertEquals("callback.callbackUrl",
                createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].field"));
        assertEquals("Connection to the callback URL could not be verified.",
                createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].message"));

    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserPostBundleStitch() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        unAuthenticatedRequest
                .body(convertObjectToJsonBytes(documentTask))
                .post(END_POINT)
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void shouldReturn404WhenGetDocumentTaskWithNonExistentId() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        request
                .body(convertObjectToJsonBytes(documentTask))
                .post(END_POINT)
                .then().log().all()
                .assertThat()
                .statusCode(201);

        final long nonExistentId = Long.MAX_VALUE;
        final String taskUrl = END_POINT + "/" + nonExistentId;
        request
                .get(taskUrl)
                .then().log().all()
                .assertThat()
                .statusCode(404);
    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserGetDocumentTask() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        final String documentTaskId =
                request
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(END_POINT)
                        .then()
                        .assertThat()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        final String taskUrl = END_POINT + "/" + documentTaskId;
        unAuthenticatedRequest
                .get(taskUrl)
                .then().log().all()
                .assertThat()
                .statusCode(401);
    }
}
