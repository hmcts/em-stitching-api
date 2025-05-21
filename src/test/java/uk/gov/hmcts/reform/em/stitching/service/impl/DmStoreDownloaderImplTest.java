package uk.gov.hmcts.reform.em.stitching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import pl.touk.throwing.exception.WrappedException;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DmStoreDownloaderImplTest {

    private static final String PDF_FILENAME = "test-files/annotationTemplate.pdf";
    private static final String DEFAULT_METADATA_SUCCESS_BODY_TEMPLATE = """
        {
          "mimeType": "application/pdf",
          "_links": {
            "binary": {
              "href": "%s/documents/some-doc/binary"
            }
          }
        }""";
    private static final String DUMMY_DM_STORE_BASE_URL = "http://dm-store-dummy-host";

    private DmStoreDownloader dmStoreDownloader;

    @Mock
    private DmStoreUriFormatter dmStoreUriFormatter;

    private int metadataResponseCode;
    private int binaryResponseCode;
    private String metadataResponseBody;
    private boolean throwIOExceptionForMetadata;
    private boolean throwIOExceptionForBinary;
    private boolean useEmptyBinaryResponseBody;

    @BeforeEach
    void setup() {
        this.metadataResponseCode = 200;
        this.binaryResponseCode = 200;
        this.metadataResponseBody = String.format(DEFAULT_METADATA_SUCCESS_BODY_TEMPLATE, DUMMY_DM_STORE_BASE_URL);
        this.throwIOExceptionForMetadata = false;
        this.throwIOExceptionForBinary = false;
        this.useEmptyBinaryResponseBody = false;

        OkHttpClient http = new OkHttpClient
            .Builder()
            .addInterceptor(this::intercept)
            .build();

        when(dmStoreUriFormatter.formatDmStoreUri(nullable(String.class))).thenAnswer(invocation -> {
            String originalUri = invocation.getArgument(0);
            if (originalUri == null) {
                throw new NullPointerException("Original URI cannot be null for DmStoreUriFormatter");
            }
            if (originalUri.startsWith("http://") || originalUri.startsWith("https://")) {
                return originalUri;
            }
            return DUMMY_DM_STORE_BASE_URL + (originalUri.startsWith("/") ? originalUri : "/" + originalUri);
        });

        dmStoreDownloader = new DmStoreDownloaderImpl(http, () -> "auth", dmStoreUriFormatter, new ObjectMapper());
    }

    private Response intercept(Interceptor.Chain chain) throws IOException {
        String requestUrl = chain.request().url().toString();

        if (requestUrl.startsWith(DUMMY_DM_STORE_BASE_URL) && requestUrl.endsWith("/binary")) {
            if (this.throwIOExceptionForBinary) {
                throw new IOException("Simulated IOException for binary request");
            }
            if (this.binaryResponseCode != 200) {
                return new Response.Builder()
                    .body(ResponseBody.create("", MediaType.get("application/octet-stream")))
                    .request(chain.request())
                    .message("Binary Error")
                    .code(this.binaryResponseCode)
                    .protocol(Protocol.HTTP_2)
                    .build();
            }
            InputStream fileStream = DmStoreDownloaderImplTest.class.getClassLoader().getResourceAsStream(PDF_FILENAME);
            assertNotNull(fileStream, "PDF test file not found: " + PDF_FILENAME);
            byte[] responseBodyBytes = useEmptyBinaryResponseBody ? new byte[0] : IOUtils.toByteArray(fileStream);
            fileStream.close();
            return new Response.Builder()
                .body(ResponseBody.create(responseBodyBytes, Objects.requireNonNull(MediaType.get("application/pdf"))))
                .request(chain.request())
                .message("Binary OK")
                .code(200)
                .protocol(Protocol.HTTP_2)
                .build();
        } else {
            if (this.throwIOExceptionForMetadata) {
                throw new IOException("Simulated IOException for metadata request");
            }
            return new Response.Builder()
                .body(ResponseBody.create(
                    this.metadataResponseBody,
                    Objects.requireNonNull(MediaType.get("application/json"))))
                .request(chain.request())
                .message(this.metadataResponseCode == 200 ? "Metadata OK" : "Metadata Error")
                .code(this.metadataResponseCode)
                .protocol(Protocol.HTTP_2)
                .build();
        }
    }

    private DocumentTaskProcessingException assertAndGetCause(Runnable executable) {
        WrappedException wrappedEx = assertThrows(
            WrappedException.class,
            executable::run
        );
        Throwable cause = wrappedEx.getCause();
        assertNotNull(cause, "WrappedException's cause should not be null");
        assertInstanceOf(DocumentTaskProcessingException.class, cause);
        return (DocumentTaskProcessingException) cause;
    }

    @Test
    void downloadFiles() {
        BundleDocument mockBundleDocument1 = new BundleDocument();
        BundleDocument mockBundleDocument2 = new BundleDocument();
        mockBundleDocument1.setDocumentURI("/AAAA");
        mockBundleDocument2.setDocumentURI("/BBBB");

        Stream<Pair<BundleDocument, FileAndMediaType>> resultsStream =
            dmStoreDownloader.downloadFiles(Stream.of(mockBundleDocument1, mockBundleDocument2));
        assertNotNull(resultsStream);
        assertEquals(2, resultsStream.toList().size());
    }

    @Test
    void downloadFileCreatesTempFileAndReturnsCorrectMediaType() throws IOException {
        BundleDocument mockBundleDocument1 = new BundleDocument();
        mockBundleDocument1.setDocumentURI("/AAAA");

        Stream<Pair<BundleDocument, FileAndMediaType>> resultsStream =
            dmStoreDownloader.downloadFiles(Stream.of(mockBundleDocument1));
        Pair<BundleDocument, FileAndMediaType> result = resultsStream.toList().getFirst();

        assertEquals(mockBundleDocument1, result.getFirst());
        File tempFile = result.getSecond().getFile();
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
        assertEquals(MediaType.get("application/pdf"), result.getSecond().getMediaType());
        Files.deleteIfExists(tempFile.toPath());
    }

    @Test
    void downloadFileWhenDocumentUriIsNull() {
        BundleDocument docWithNullUri = new BundleDocument();
        docWithNullUri.setDocumentURI(null);

        DocumentTaskProcessingException actualException = assertAndGetCause(
            () -> dmStoreDownloader.downloadFiles(Stream.of(docWithNullUri)).toList()
        );
        assertEquals("Could not access the binary: Original URI cannot be null for DmStoreUriFormatter",
            actualException.getMessage());
        assertNotNull(actualException.getCause());
        assertInstanceOf(NullPointerException.class, actualException.getCause());
        assertEquals("Original URI cannot be null for DmStoreUriFormatter",
            actualException.getCause().getMessage());
    }

    @Test
    void downloadFileWhenMetadataRequestFails() {
        this.metadataResponseCode = 500;
        BundleDocument doc = new BundleDocument();
        doc.setDocumentURI("/docPath1");

        DocumentTaskProcessingException actualException = assertAndGetCause(
            () -> dmStoreDownloader.downloadFiles(Stream.of(doc)).toList()
        );
        assertTrue(actualException.getMessage().contains("Could not access the meta-data. HTTP response: 500"));
    }

    @Test
    void downloadFileWhenBinaryRequestFails() {
        this.binaryResponseCode = 404;
        BundleDocument doc = new BundleDocument();
        doc.setDocumentURI("/docPath2");

        DocumentTaskProcessingException actualException = assertAndGetCause(
            () -> dmStoreDownloader.downloadFiles(Stream.of(doc)).toList()
        );
        assertTrue(actualException.getMessage().contains("Could not access the binary. HTTP response: 404"));
    }

    @Test
    void downloadFileWhenMetadataIsMalformed() {
        this.metadataResponseBody = "this is not valid json";
        BundleDocument doc = new BundleDocument();
        doc.setDocumentURI("/docPath3");

        DocumentTaskProcessingException actualException = assertAndGetCause(
            () -> dmStoreDownloader.downloadFiles(Stream.of(doc)).toList()
        );
        assertTrue(actualException.getMessage().startsWith("Could not access the binary: Unrecognized token 'this'"));
        assertNotNull(actualException.getCause());
        assertInstanceOf(IOException.class, actualException.getCause());
    }

    @Test
    void downloadFileWhenMetadataIsIncomplete() {
        this.metadataResponseBody =
            "{ \"mimeType\": \"application/pdf\", \"_links\": { \"self\": { \"href\": \"self_link\"} } }";
        BundleDocument doc = new BundleDocument();
        doc.setDocumentURI("/docPath4");

        DocumentTaskProcessingException actualException = assertAndGetCause(
            () -> dmStoreDownloader.downloadFiles(Stream.of(doc)).toList()
        );
        String expectedNpeMessage = "Cannot invoke \"com.fasterxml.jackson.databind.JsonNode.get(String)\" "
            + "because the return value of \"com.fasterxml.jackson.databind.JsonNode.get(String)\" is null";
        String expectedFullMessage = "Could not access the binary: " + expectedNpeMessage;
        assertEquals(expectedFullMessage, actualException.getMessage());
        assertNotNull(actualException.getCause());
        assertInstanceOf(NullPointerException.class, actualException.getCause());
        assertEquals(expectedNpeMessage, actualException.getCause().getMessage());
    }

    @Test
    void downloadFileWhenMetadataIsIncompleteMissingHrefNode() {
        this.metadataResponseBody = String.format("{ \"mimeType\": \"application/pdf\", \"_links\": "
                + "{ \"binary\" : { \"not_href_field\": \"%s/documents/some-doc/binary\" } } }",
            DUMMY_DM_STORE_BASE_URL
        );
        BundleDocument doc = new BundleDocument();
        doc.setDocumentURI("/docPath5");

        DocumentTaskProcessingException actualException = assertAndGetCause(
            () -> dmStoreDownloader.downloadFiles(Stream.of(doc)).toList()
        );
        String expectedNpeMessage = "Cannot invoke \"com.fasterxml.jackson.databind.JsonNode.asText()\" "
            + "because the return value of \"com.fasterxml.jackson.databind.JsonNode.get(String)\" is null";
        String expectedFullMessage = "Could not access the binary: " + expectedNpeMessage;
        assertEquals(expectedFullMessage, actualException.getMessage());
        assertNotNull(actualException.getCause());
        assertInstanceOf(NullPointerException.class, actualException.getCause());
        assertEquals(expectedNpeMessage, actualException.getCause().getMessage());
    }

    @Test
    void downloadFileWhenMetadataCallThrowsIOException() {
        this.throwIOExceptionForMetadata = true;
        BundleDocument doc = new BundleDocument();
        doc.setDocumentURI("/docPath6");

        DocumentTaskProcessingException actualException = assertAndGetCause(
            () -> dmStoreDownloader.downloadFiles(Stream.of(doc)).toList()
        );
        assertTrue(actualException.getMessage()
            .contains("Could not access the binary: Simulated IOException for metadata request"));
        assertNotNull(actualException.getCause());
        assertInstanceOf(IOException.class, actualException.getCause());
        assertEquals("Simulated IOException for metadata request", actualException.getCause().getMessage());
    }

    @Test
    void downloadFileWhenBinaryCallThrowsIOException() {
        this.throwIOExceptionForBinary = true;
        BundleDocument doc = new BundleDocument();
        doc.setDocumentURI("/docPath7");

        DocumentTaskProcessingException actualException = assertAndGetCause(
            () -> dmStoreDownloader.downloadFiles(Stream.of(doc)).toList()
        );
        assertTrue(actualException.getMessage()
            .contains("Could not access the binary: Simulated IOException for binary request"));
        assertNotNull(actualException.getCause());
        assertInstanceOf(IOException.class, actualException.getCause());
        assertEquals("Simulated IOException for binary request", actualException.getCause().getMessage());
    }
}