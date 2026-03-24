package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestDataConstants.AUTH_HEADER;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestDataConstants.SERVICE_AUTH_HEADER;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestDataConstants.TASK_STATE_FIELD;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.convertObjectToJsonBytes;

class DocumentTaskAuthorizationScenarios extends BaseTest {

    private static final String DOCUMENT_TASKS_ENDPOINT = "/api/document-tasks";
    private static final List<String> NON_CASEWORKER_ROLES = List.of("ccd-import");

    private final IdamHelper idamHelper;
    private final String nonCaseworkerEmail =
        "stitching.citizen." + UUID.randomUUID() + "@test.com";

    private RequestSpecification nonCaseworkerRequest;

    @Autowired
    DocumentTaskAuthorizationScenarios(TestUtil testUtil, IdamHelper idamHelper) {
        super(testUtil);
        this.idamHelper = idamHelper;
    }

    @BeforeEach
    void setupNonCaseworkerUser() {
        idamHelper.createUser(nonCaseworkerEmail, NON_CASEWORKER_ROLES);
        String nonCaseworkerJwt = idamHelper.authenticateUser(nonCaseworkerEmail);

        nonCaseworkerRequest = SerenityRest
            .given()
            .baseUri(testUtil.getTestUrl())
            .contentType(APPLICATION_JSON_VALUE)
            .header(AUTH_HEADER, nonCaseworkerJwt)
            .header(SERVICE_AUTH_HEADER, testUtil.getS2sAuth());
    }

    @Test
    void shouldReturnNotFoundWhenDifferentUserAttemptsToGetAnotherUsersTask() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
            .body(convertObjectToJsonBytes(documentTask))
            .post(DOCUMENT_TASKS_ENDPOINT);

        assertEquals(201, createTaskResponse.getStatusCode(),
            "Primary user should be able to create a task.");
        String taskId = createTaskResponse.getBody().jsonPath().getString("id");

        Response getTaskResponse = nonCaseworkerRequest
            .get(DOCUMENT_TASKS_ENDPOINT + "/" + taskId);

        assertEquals(404, getTaskResponse.getStatusCode(),
            "A different authenticated user must not be able to retrieve another user's task (IDOR).");
    }

    @Test
    void shouldReturnTaskWhenOwnerRequestsTheirOwnTask() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
            .body(convertObjectToJsonBytes(documentTask))
            .post(DOCUMENT_TASKS_ENDPOINT);

        assertEquals(201, createTaskResponse.getStatusCode(),
            "Primary user should be able to create a task.");
        String taskId = createTaskResponse.getBody().jsonPath().getString("id");

        Response getTaskResponse = testUtil.authRequest()
            .get(DOCUMENT_TASKS_ENDPOINT + "/" + taskId);

        assertEquals(200, getTaskResponse.getStatusCode(),
            "The task owner must be able to retrieve their own task.");
    }

    @Test
    void shouldFailTaskWhenUserHasNoCaseworkerRole() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = nonCaseworkerRequest
            .body(convertObjectToJsonBytes(documentTask))
            .post(DOCUMENT_TASKS_ENDPOINT);

        assertEquals(201, createTaskResponse.getStatusCode(),
            "Task creation should be accepted regardless of role; "
                + "authorisation is enforced when downloading from DM Store.");

        String taskUrl = DOCUMENT_TASKS_ENDPOINT + "/"
            + createTaskResponse.getBody().jsonPath().getString("id");

        Response taskResponse = testUtil.pollUntil(taskUrl, body -> {
            String state = body.getString(TASK_STATE_FIELD);
            return "FAILED".equals(state) || "DONE".equals(state);
        });

        assertEquals("FAILED", taskResponse.getBody().jsonPath().getString(TASK_STATE_FIELD),
            "Task should have FAILED because DM Store rejects downloads for non-caseworker roles. "
                + "If DONE, the hardcoded 'caseworker' role has been reintroduced.");
    }
}
