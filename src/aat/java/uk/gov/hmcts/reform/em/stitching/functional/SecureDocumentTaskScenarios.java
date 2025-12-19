package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.CallbackDto;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.convertObjectToJsonBytes;


public class SecureDocumentTaskScenarios extends BaseTest {

    private static final String API_DOCUMENT_TASKS = "/api/document-tasks";
    private static final String TASK_ID_PATH = "id";
    private static final String TASK_STATE_PATH = "taskState";
    private static final String STITCHED_DOC_URI_PATH = "bundle.stitchedDocumentURI";
    private static final String HASH_TOKEN_PATH = "bundle.hashToken";
    private static final String CALLBACK_URL_PATH = "callback.callbackUrl";
    private static final String CALLBACK_STATE_PATH = "callback.callbackState";
    private static final String POSTMAN_ECHO_URL = "https://postman-echo.com/post";


    private RequestSpecification request;
    private RequestSpecification unAuthenticatedRequest;
    private DocumentTaskDTO documentTask;

    @Autowired
    protected SecureDocumentTaskScenarios(TestUtil testUtil) {
        super(testUtil);
    }

    @BeforeEach
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
    void testPostBundleStitch() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE_PATH).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(STITCHED_DOC_URI_PATH));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(HASH_TOKEN_PATH));
    }

    @Test
    void testPostBundleStitchWithWordDoc() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getCdamTestBundleWithWordDoc();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE_PATH).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(STITCHED_DOC_URI_PATH));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(HASH_TOKEN_PATH));
    }

    @Test
    void testPostBundleStitchWithTextFile() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getCdamTestBundleWithTextFile();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE_PATH).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(STITCHED_DOC_URI_PATH));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(HASH_TOKEN_PATH));
    }

    @Test
    void testPostBundleStitchWithRichTextFile() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getCdamTestBundleWithRichTextFile();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE_PATH).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(STITCHED_DOC_URI_PATH));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(HASH_TOKEN_PATH));
    }

    @Disabled("Seems to be intermittently failing. Seems to be an issue with the test file or the conversion service.")
    @Test
    void testPostBundleStitchWithExcelAndPpt() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getCdamTestBundleWithExcelAndPptDoc();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE_PATH).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(STITCHED_DOC_URI_PATH));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(HASH_TOKEN_PATH));
    }

    @Test
    void testPostBundleStitchWithImage() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getCdamTestBundleWithImage();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE_PATH).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(STITCHED_DOC_URI_PATH));
        assertNotNull(getTaskResponse.getBody().jsonPath().getString(HASH_TOKEN_PATH));
    }

    @Test
    void testPostBundleStitchWithDocumentWatermarkImage() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getCdamTestBundleWithWatermarkImage();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString(TASK_STATE_PATH).equals("DONE"));

        assertEquals(200, getTaskResponse.getStatusCode());
    }

    @Test
    void testPostDocumentTask() throws IOException {
        BundleDTO bundle = testUtil.getCdamTestBundle();

        documentTask.setBundle(bundle);

        Response response =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        assertEquals(201, response.getStatusCode());
        assertEquals(response.getBody().jsonPath().getString(TASK_STATE_PATH), TaskState.NEW.toString());
    }

    @Test
    void testStitchTwoIdenticalDocuments() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getCdamTestBundleWithDuplicateBundleDocuments();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        Response completedResponse = testUtil.pollUntil(taskUrl, body ->
            body.getString(TASK_STATE_PATH).equals("DONE"));

        assertEquals(200, completedResponse.getStatusCode());
        assertNotNull(completedResponse.getBody().jsonPath().getString(STITCHED_DOC_URI_PATH));
        assertNotNull(completedResponse.getBody().jsonPath().getString(HASH_TOKEN_PATH));
    }

    @Test
    void testStitchDocumentsWithSortIndices() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getCdamTestBundleWithSortedDocuments();
        documentTask.setBundle(bundle);

        Response createTaskResponse =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        Response completedResponse = testUtil.pollUntil(taskUrl, body ->
            body.getString(TASK_STATE_PATH).equals("DONE"));

        String stitchedDocumentUri = completedResponse.getBody().jsonPath().getString(STITCHED_DOC_URI_PATH);

        //We need to donwload the Stitched Document via Dm-Store and not via CDAM. As at this stage the document is
        // not yet associated to the case through CCD callBack.
        File stitchedFile = testUtil.downloadDocument(stitchedDocumentUri);

        PDDocument stitchedPdDocument = Loader.loadPDF(stitchedFile);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String parsedText = pdfStripper.getText(stitchedPdDocument);

        stitchedPdDocument.close();

        int indexOfDocument1 = parsedText.indexOf("Document 1");
        int indexOfDocument2 = parsedText.indexOf("Document 2");
        assertTrue(indexOfDocument2 < indexOfDocument1);
    }


    @Test
    void testPostBundleStitchWithCallback() throws IOException, InterruptedException {

        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);

        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl(POSTMAN_ECHO_URL);

        documentTask.setCallback(callback);

        Response createTaskResponse =
            request
                .log().all()
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);
        assertEquals(201, createTaskResponse.getStatusCode());
        assertEquals(POSTMAN_ECHO_URL,
            createTaskResponse.getBody().jsonPath().getString(CALLBACK_URL_PATH));

        String taskUrl = API_DOCUMENT_TASKS + "/" + createTaskResponse.getBody().jsonPath().getString(TASK_ID_PATH);
        testUtil.pollUntil(taskUrl, body -> body.getString(CALLBACK_STATE_PATH).equals("SUCCESS"));

    }

    @Test
    void testPostBundleStitchWithCallbackForFailure() throws IOException {
        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl(POSTMAN_ECHO_URL);
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
                .post(API_DOCUMENT_TASKS);
        assertEquals(400, createTaskResponse.getStatusCode());
        assertTrue(createTaskResponse.body().asString().contains("Error saving Document Task"));
        assertTrue(createTaskResponse.getBody().jsonPath().getString("detail")
            .contains("Bundle Title can not be more than 255 Chars"));

    }

    @Test
    void testPostBundleStitchWithCallbackUrlNotAccessible() throws IOException {
        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);

        CallbackDto callback = new CallbackDto();
        callback.setCallbackUrl("http://localhost:80899/my/callback/resource");

        documentTask.setCallback(callback);

        Response createTaskResponse =
            request
                .log().all()
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS);

        createTaskResponse.prettyPrint();
        assertEquals(400, createTaskResponse.getStatusCode());
        assertEquals(CALLBACK_URL_PATH,
            createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].field"));
        assertEquals("Connection to the callback URL could not be verified.",
            createTaskResponse.getBody().jsonPath().getString("fieldErrors[0].message"));

    }

    @Test
    void shouldReturn401WhenUnAuthenticatedUserPostBundleStitch() throws IOException {
        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);

        unAuthenticatedRequest
            .body(convertObjectToJsonBytes(documentTask))
            .post(API_DOCUMENT_TASKS)
            .then()
            .assertThat()
            .statusCode(401);
    }

    @Test
    void shouldReturn404WhenGetDocumentTaskWithNonExistentId() throws IOException {
        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);
        request
            .body(convertObjectToJsonBytes(documentTask))
            .post(API_DOCUMENT_TASKS)
            .then().log().all()
            .assertThat()
            .statusCode(201);

        final long nonExistentId = Long.MAX_VALUE;
        final String taskUrl = API_DOCUMENT_TASKS + "/" + nonExistentId;
        request
            .get(taskUrl)
            .then().log().all()
            .assertThat()
            .statusCode(404);
    }

    @Test
    void shouldReturn401WhenUnAuthenticatedUserGetDocumentTask() throws IOException {
        BundleDTO bundle = testUtil.getCdamTestBundle();
        documentTask.setBundle(bundle);

        final String documentTaskId =
            request
                .body(convertObjectToJsonBytes(documentTask))
                .post(API_DOCUMENT_TASKS)
                .then()
                .assertThat()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString(TASK_ID_PATH);

        final String taskUrl = API_DOCUMENT_TASKS + "/" + documentTaskId;
        unAuthenticatedRequest
            .get(taskUrl)
            .then().log().all()
            .assertThat()
            .statusCode(401);
    }
}
