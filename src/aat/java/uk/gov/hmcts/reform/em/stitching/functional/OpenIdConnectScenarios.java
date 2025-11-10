package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.convertObjectToJsonBytes;

class OpenIdConnectScenarios extends BaseTest {

    private static final String DOCUMENT_TASKS_ENDPOINT = "/api/document-tasks";

    @Autowired
    protected OpenIdConnectScenarios(TestUtil testUtil) {
        super(testUtil);
    }

    @Test
    void testValidAuthenticationAndAuthorisation() throws IOException {
        DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Response createTaskResponse =
                testUtil
                        .authRequest()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(DOCUMENT_TASKS_ENDPOINT);

        assertEquals(201, createTaskResponse.getStatusCode());
    }

    @Test
    void testInvalidS2SAuth() throws IOException {
        DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Response createTaskResponse =
                testUtil
                        .invalidS2SAuth()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(DOCUMENT_TASKS_ENDPOINT);

        assertEquals(401, createTaskResponse.getStatusCode());
    }

    @Test
    void testWithInvalidIdamAuth() throws IOException {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Response createTaskResponse =
                testUtil
                        .invalidIdamAuthrequest()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(DOCUMENT_TASKS_ENDPOINT);

        assertEquals(401, createTaskResponse.getStatusCode());

    }

    @Test
    void testWithEmptyS2SAuth() {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        assertThrows(NullPointerException.class, () -> testUtil
                .validAuthRequestWithEmptyS2SAuth()
                .body(convertObjectToJsonBytes(documentTask))
                .post(DOCUMENT_TASKS_ENDPOINT));

    }

    @Test
    void testWithEmptyIdamAuthAndValidS2SAuth() {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Throwable exceptionThrown =
                assertThrows(NullPointerException.class, () -> testUtil
                        .validS2SAuthWithEmptyIdamAuth()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post(DOCUMENT_TASKS_ENDPOINT));

        assertEquals("Header value", exceptionThrown.getMessage());
    }

    @Test
    void testIdamAuthAndS2SAuthAreEmpty() {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        assertThrows(NullPointerException.class, () -> testUtil
                .emptyIdamAuthAndEmptyS2SAuth()
                .body(convertObjectToJsonBytes(documentTask))
                .post(DOCUMENT_TASKS_ENDPOINT));
    }

    private DocumentTaskDTO createDocumentTaskDTO() {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        return documentTask;
    }
}
