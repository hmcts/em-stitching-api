package uk.gov.hmcts.reform.em.stitching.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Guards the CDAM client Jackson 3 contract used by Feign upload decoding.
 * Requires ccd-case-document-am-client 2.0.0+ ({@code Document} no-arg constructor).
 */
class CdamDocumentJackson3CompatibilityTest {

    private static final String UPLOAD_RESPONSE_JSON = """
            {
              "documents": [
                {
                  "classification": "PUBLIC",
                  "size": 123,
                  "mimeType": "application/pdf",
                  "originalDocumentName": "out.pdf",
                  "hashToken": "abc",
                  "_links": {
                    "self": { "href": "http://cdam/documents/111" },
                    "binary": { "href": "http://cdam/documents/111/binary" }
                  }
                }
              ]
            }
            """;

    @Test
    void jackson3DeserializesCdamUploadResponseAndDocumentLinks() {
        JsonMapper mapper = JsonMapper.builder().build();

        UploadResponse response = mapper.readValue(UPLOAD_RESPONSE_JSON, UploadResponse.class);
        Document document = response.getDocuments().get(0);

        assertEquals("abc", document.hashToken);
        assertNotNull(document.links);
        assertEquals("http://cdam/documents/111", document.links.self.href);
        assertEquals("http://cdam/documents/111/binary", document.links.binary.href);
    }
}
