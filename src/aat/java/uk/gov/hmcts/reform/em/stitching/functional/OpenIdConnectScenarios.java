package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenIdConnectScenarios  extends BaseTest  {

    @Rule
    public ExpectedException exceptionThrown = ExpectedException.none();



    @Test
    public void testValidAuthenticationAndAuthorisation() throws IOException, InterruptedException {
        DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        assertEquals(201, createTaskResponse.getStatusCode());
    }

    @Test // Invalid S2SAuth
    public void testInvalidS2SAuth() throws IOException, InterruptedException {
        DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Response createTaskResponse = testUtil.invalidS2SAuth()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        assertEquals(401, createTaskResponse.getStatusCode());
    }

    //Invalid  IdamAuth
    @Test
    public void testWithInvalidIdamAuth() throws IOException, InterruptedException {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        Response createTaskResponse = testUtil.invalidIdamAuthrequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        assertEquals(401, createTaskResponse.getStatusCode());

    }

    //Empty S2SAuth
    @Test
    public void testWithEmptyS2SAuth() throws IOException, InterruptedException {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        exceptionThrown.expect(IllegalArgumentException.class);

        Response createTaskResponse = testUtil.validAuthRequestWithEmptyS2SAuth()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

    }

    // Empty IdamAuth and Valid S2S Auth
    @Test
    public void testWithEmptyIdamAuthAndValidS2SAuth() throws IOException, InterruptedException {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();
        exceptionThrown.expect(IllegalArgumentException.class);

        final Response createTaskResponse = testUtil.validS2SAuthWithEmptyIdamAuth()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", testUtil.getTestUrl() + "/api/document-tasks");

        exceptionThrown.expectMessage("Header value cannot be null");

    }

    // Empty IdamAuth and Empty S2SAuth
    @Test
    public void testIdamAuthAndS2SAuthAreEmpty() throws IOException, InterruptedException {
        final DocumentTaskDTO documentTask = createDocumentTaskDTO();

        exceptionThrown.expect(IllegalArgumentException.class);

        final Response createTaskResponse = testUtil.emptyIdamAuthAndEmptyS2SAuth()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", testUtil.getTestUrl() + "/api/document-tasks");
    }

    private DocumentTaskDTO createDocumentTaskDTO() {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);
        return documentTask;
    }

}
