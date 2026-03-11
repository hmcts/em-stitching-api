package uk.gov.hmcts.reform.em.stitching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
class DmStoreDownloaderIntegrationTest {

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    private DmStoreDownloaderImpl dmStoreDownloader;
    private MockWebServer mockWebServer;
    private String mockServerBaseUrl;
    private final OkHttpClient okHttpClient = new OkHttpClient();

    private static final String JSON_TYPE = "application/json";
    private static final String PDF_TYPE = "application/pdf";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockServerBaseUrl = "http://localhost:" + mockWebServer.getPort();

        DmStoreUriFormatter formatter = new DmStoreUriFormatter(mockServerBaseUrl);
        dmStoreDownloader = new DmStoreDownloaderImpl(okHttpClient, authTokenGenerator, formatter, objectMapper);

        when(authTokenGenerator.generate()).thenReturn("Bearer test-token");
    }

    @AfterEach
    void tearDown() {
        mockWebServer.close();
    }

    @Nested
    @DisplayName("URL Reconstruction Tests")
    class UrlLogic {

        @Test
        @DisplayName("Should wash external domains and ports out of input URIs")
        void shouldReconstructCleanUrl() throws Exception {
            UUID docId = UUID.randomUUID();
            BundleDocument doc = createBundleDoc("https://external-system.com:8443/documents/" + docId);

            enqueueDoc(docId);
            dmStoreDownloader.downloadFiles(Stream.of(doc)).toList();

            assertRequestMatches(mockWebServer.takeRequest(), "/documents/" + docId);
        }

        @Test
        @DisplayName("Should handle multiple input formats in parallel")
        void shouldHandleMixedFormatsInParallel() throws Exception {
            UUID docId = UUID.randomUUID();
            mockWebServer.setDispatcher(createUuidDispatcher(docId));

            Stream<BundleDocument> docs = Stream.of(
                createBundleDoc("https://ext.com/documents/" + docId),
                createBundleDoc("/documents/" + docId),
                createBundleDoc(docId.toString())
            );

            dmStoreDownloader.downloadFiles(docs).toList();

            for (int i = 0; i < 6; i++) {
                assertThat(mockWebServer.takeRequest().getUrl().toString()).contains(docId.toString());
            }
        }

        @Test
        @DisplayName("Should sanitize the binary link returned inside the metadata JSON")
        void shouldSanitizeInternalMetadataLinks() throws Exception {
            UUID docId = UUID.randomUUID();
            String dirtyMetadata = createMetadataJson("https://malicious.com/documents/" + docId + "/binary");

            mockWebServer.enqueue(jsonResponse(dirtyMetadata));
            mockWebServer.enqueue(binaryResponse());

            dmStoreDownloader.downloadFiles(Stream.of(createBundleDoc(docId.toString()))).toList();

            mockWebServer.takeRequest(); // Skip metadata
            RecordedRequest binaryReq = mockWebServer.takeRequest();

            assertRequestMatches(binaryReq, "/documents/" + docId + "/binary");
            assertThat(binaryReq.getUrl().host()).isEqualTo("localhost");
        }
    }

    @Nested
    @DisplayName("Success & Content Tests")
    class SuccessTests {

        @Test
        @DisplayName("Should successfully download file and return correct Pair metadata")
        void shouldDownloadAndReturnFileWithMediaType() throws Exception {
            UUID docId = UUID.randomUUID();
            enqueueDoc(docId);

            List<Pair<BundleDocument, FileAndMediaType>> results =
                dmStoreDownloader.downloadFiles(Stream.of(createBundleDoc(docId.toString()))).toList();

            assertThat(results).hasSize(1);
            Pair<BundleDocument, FileAndMediaType> result = results.get(0);

            assertThat(result.getSecond().getMediaType().toString()).isEqualTo(PDF_TYPE);
            assertThat(result.getSecond().getFile()).exists().canRead();
            assertThat(result.getSecond().getFile().getName()).startsWith("dm-store");
        }
    }

    @Nested
    @DisplayName("Security & Validation Rejection Tests")
    class SecurityRejection {

        @ParameterizedTest(name = "Should reject {0}")
        @ValueSource(strings = {
            "/documents/../../../etc/passwd",                   // Path traversal
            "/documents/valid-uuid?query=param",                // Query param injection
            "/documents/test?redirect=http://invalid.com",      // Redirect attempt
            "/documents/UUID/extra/path",                       // Strict pattern violation
            "/documents/invalid/delete",                        // Malicious path
            "/documents/<tag></tag>",                           // XSS/Tag injection
            "not-a-uuid"                                        // Garbage input
        })
        void shouldRejectInvalidInputs(String input) {
            // Replace 'valid-uuid' placeholder with actual UUID for that specific case
            String finalInput = input.replace("valid-uuid", UUID.randomUUID().toString())
                .replace("UUID", UUID.randomUUID().toString());

            BundleDocument doc = createBundleDoc(finalInput);

            assertThatThrownBy(() -> dmStoreDownloader.downloadFiles(Stream.of(doc)).toList())
                .hasRootCauseInstanceOf(IllegalArgumentException.class);

            assertThat(mockWebServer.getRequestCount()).isZero();
        }
    }

    // --- Helpers ---

    private void enqueueDoc(UUID docId) throws Exception {
        mockWebServer.enqueue(jsonResponse(createMetadataJson("/documents/" + docId + "/binary")));
        mockWebServer.enqueue(binaryResponse());
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse.Builder().code(200).body(body).addHeader("Content-Type", JSON_TYPE).build();
    }

    private MockResponse binaryResponse() {
        return new MockResponse.Builder()
            .code(200)
            .body("%PDF-1.4 content")
            .addHeader("Content-Type", PDF_TYPE)
            .build();
    }

    private String createMetadataJson(String binaryUrl) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "_links", Map.of("binary", Map.of("href", binaryUrl)),
            "mimeType", PDF_TYPE
        ));
    }

    private BundleDocument createBundleDoc(String uri) {
        BundleDocument doc = new BundleDocument();
        doc.setDocumentURI(uri);
        return doc;
    }

    private void assertRequestMatches(RecordedRequest request, String expectedPath) {
        assertThat(request.getUrl().encodedPath()).isEqualTo(expectedPath);
    }

    private Dispatcher createUuidDispatcher(UUID validId) {
        return new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                String path = request.getUrl().encodedPath();
                try {
                    if (path.endsWith("/binary")) {
                        return binaryResponse();
                    }
                    if (path.contains(validId.toString())) {
                        return jsonResponse(createMetadataJson("/documents/" + validId + "/binary"));
                    }
                } catch (Exception e) {
                    return new MockResponse.Builder().code(500).build();
                }
                return new MockResponse.Builder().code(404).build();
            }
        };
    }
}