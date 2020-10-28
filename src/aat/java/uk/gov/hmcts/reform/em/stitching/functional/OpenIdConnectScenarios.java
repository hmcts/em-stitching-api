package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.em.stitching.testutil.TestUtil.convertObjectToJsonBytes;

public class OpenIdConnectScenarios extends BaseTest {

    @Test
    public void testValidAuthenticationAndAuthorisation() throws IOException {
        DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Response createTaskResponse =
                testUtil
                        .authRequest()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
    }

    @Test // Invalid S2SAuth
    public void testInvalidS2SAuth() throws IOException {
        DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Response createTaskResponse =
                testUtil
                        .invalidS2SAuth()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(401, createTaskResponse.getStatusCode());
    }

    //Invalid  IdamAuth
    @Test
    public void testWithInvalidIdamAuth() throws IOException {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Response createTaskResponse =
                testUtil
                        .invalidIdamAuthrequest()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        assertEquals(401, createTaskResponse.getStatusCode());

    }

    //Empty S2SAuth
    @Test
    public void testWithEmptyS2SAuth() {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        assertThrows(NullPointerException.class, () -> testUtil
                .validAuthRequestWithEmptyS2SAuth()
                .body(convertObjectToJsonBytes(documentTask))
                .post("/api/document-tasks"));

    }

    // Empty IdamAuth and Valid S2S Auth
    @Test
    public void testWithEmptyIdamAuthAndValidS2SAuth() {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Throwable exceptionThrown =
                assertThrows(NullPointerException.class, () -> testUtil
                        .validS2SAuthWithEmptyIdamAuth()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks"));

        assertEquals("Header value", exceptionThrown.getMessage());
    }

    // Empty IdamAuth and Empty S2SAuth
    @Test
    public void testIdamAuthAndS2SAuthAreEmpty() {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        assertThrows(NullPointerException.class, () -> testUtil
                .emptyIdamAuthAndEmptyS2SAuth()
                .body(convertObjectToJsonBytes(documentTask))
                .post("/api/document-tasks"));
    }

    private DocumentTaskDTO createDocumentTaskDTO() {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        return documentTask;
    }

}
