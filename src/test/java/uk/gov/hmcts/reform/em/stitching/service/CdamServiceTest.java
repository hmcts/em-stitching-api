package uk.gov.hmcts.reform.em.stitching.service;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;
import uk.gov.hmcts.reform.em.stitching.service.impl.FileAndMediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdamServiceTest {
    private CdamService cdamService;

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;

    @Mock
    private ByteArrayResource byteArrayResource;

    @Mock
    private UploadResponse uploadResponse;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private Document document;
    private File mockFile;

    private static final UUID DOC_STORE_UUID = UUID.randomUUID();
    private static final String AUTH_TOKEN = "xxx";
    private static final String SERVICE_AUTH_TOKEN = "serviceAuth";
    private static final String DOC_URI_PREFIX = "http://localhost:samplefile/";
    private static final String VALID_DOC_URI = DOC_URI_PREFIX + DOC_STORE_UUID;
    private static final String MOCK_FILE_NAME = "one-page.pdf";
    private static final String MOCK_FILE_MIME_TYPE = "application/pdf";

    @BeforeEach
    void setup() throws IOException {
        cdamService = new CdamService(caseDocumentClientApi, authTokenGenerator);
        document = Document.builder()
            .originalDocumentName(MOCK_FILE_NAME)
            .mimeType(MOCK_FILE_MIME_TYPE)
            .build();

        mockFile = File.createTempFile("testUpload", ".pdf");
        FileUtils.writeStringToFile(mockFile, "mock content", "UTF-8");
    }

    @AfterEach
    void cleanup() {
        FileUtils.deleteQuietly(mockFile);
    }

    private DocumentTask populateDocumentTask() {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setJwt(AUTH_TOKEN);
        documentTask.setServiceAuth(SERVICE_AUTH_TOKEN);
        documentTask.setJurisdictionId("PUBLICLAW");
        documentTask.setCaseTypeId("XYZ");
        Bundle bundle = new Bundle();
        bundle.setFileName("stitchedBundle.pdf");
        documentTask.setBundle(bundle);
        return documentTask;
    }

    private BundleDocument createBundleDocument(String docUri) {
        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI(docUri);
        bundleDocument.setId(123L);
        bundleDocument.setDocDescription("Test Desc");
        bundleDocument.setDocTitle("Test Title");
        return bundleDocument;
    }

    @Test
    void downloadFilesShouldDownloadAllDocumentsInTask() throws IOException {
        DocumentTask documentTask = populateDocumentTask();
        BundleDocument bundleDoc1 = createBundleDocument(DOC_URI_PREFIX + UUID.randomUUID());
        BundleDocument bundleDoc2 = createBundleDocument(DOC_URI_PREFIX + UUID.randomUUID());
        documentTask.getBundle().setDocuments(List.of(bundleDoc1, bundleDoc2));

        ResponseEntity<Resource> responseEntity = ResponseEntity.ok(byteArrayResource);
        InputStream mockInputStream1 = new FileInputStream("src/test/resources/one-page.pdf");
        InputStream mockInputStream2 = new FileInputStream("src/test/resources/wordDocument2.docx");

        UUID uuid1 = UUID.fromString(bundleDoc1.getDocumentURI().substring(bundleDoc1.getDocumentURI().lastIndexOf('/') + 1));
        UUID uuid2 = UUID.fromString(bundleDoc2.getDocumentURI().substring(bundleDoc2.getDocumentURI().lastIndexOf('/') + 1));

        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, uuid1)).thenReturn(responseEntity);
        when(caseDocumentClientApi.getMetadataForDocument(AUTH_TOKEN, SERVICE_AUTH_TOKEN, uuid1)).thenReturn(document);
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, uuid2)).thenReturn(responseEntity);
        when(caseDocumentClientApi.getMetadataForDocument(AUTH_TOKEN, SERVICE_AUTH_TOKEN, uuid2)).thenReturn(
            Document.builder().originalDocumentName("wordDocument.docx").mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document").build()
        );

        when(byteArrayResource.getInputStream()).thenReturn(mockInputStream1).thenReturn(mockInputStream2);

        Stream<Pair<BundleDocument, FileAndMediaType>> pairStream = cdamService.downloadFiles(documentTask);
        List<Pair<BundleDocument, FileAndMediaType>> results = pairStream.toList();

        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(pair -> {
            assertNotNull(pair.getFirst());
            assertNotNull(pair.getSecond());
            assertNotNull(pair.getSecond().getFile());
            assertTrue(pair.getSecond().getFile().exists());
            assertTrue(pair.getSecond().getFile().length() > 0);
            pair.getSecond().getFile().delete();
        });

        verify(caseDocumentClientApi, times(1)).getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, uuid1);
        verify(caseDocumentClientApi, times(1)).getMetadataForDocument(AUTH_TOKEN, SERVICE_AUTH_TOKEN, uuid1);
        verify(caseDocumentClientApi, times(1)).getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, uuid2);
        verify(caseDocumentClientApi, times(1)).getMetadataForDocument(AUTH_TOKEN, SERVICE_AUTH_TOKEN, uuid2);
        mockInputStream1.close();
        mockInputStream2.close();
    }

    @Test
    void downloadFilesWithEmptyDocumentListReturnsEmptyStream() {
        DocumentTask documentTask = populateDocumentTask();
        documentTask.getBundle().setDocuments(Collections.emptyList());

        Stream<Pair<BundleDocument, FileAndMediaType>> pairStream = cdamService.downloadFiles(documentTask);
        List<Pair<BundleDocument, FileAndMediaType>> results = pairStream.toList();

        assertTrue(results.isEmpty());
        verify(caseDocumentClientApi, never()).getDocumentBinary(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void downloadFileSuccessful() throws Exception {
        BundleDocument bundleDocument = createBundleDocument(VALID_DOC_URI);
        InputStream inputStream = new FileInputStream("src/test/resources/one-page.pdf");

        when(caseDocumentClientApi.getMetadataForDocument(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID))
            .thenReturn(document);
        ResponseEntity<Resource> responseEntity = ResponseEntity.ok(byteArrayResource);
        when(byteArrayResource.getInputStream()).thenReturn(inputStream);
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID))
            .thenReturn(responseEntity);

        Pair<BundleDocument, FileAndMediaType> resultPair =
            cdamService.downloadFile(AUTH_TOKEN, SERVICE_AUTH_TOKEN, bundleDocument);

        verify(caseDocumentClientApi).getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID);
        verify(caseDocumentClientApi).getMetadataForDocument(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID);

        assertNotNull(resultPair.getSecond().getFile());
        File cdamFile = resultPair.getSecond().getFile();
        assertEquals(MOCK_FILE_NAME, cdamFile.getName());
        assertTrue(cdamFile.exists());
        assertTrue(cdamFile.length() > 0);
        assertEquals(okhttp3.MediaType.get(MOCK_FILE_MIME_TYPE), resultPair.getSecond().getMediaType());
        cdamFile.delete();
        inputStream.close();
    }

    @Test
    void downloadFileWhenCdamReturnsNullResponseEntityThrowsException() {
        BundleDocument bundleDocument = createBundleDocument(VALID_DOC_URI);
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID))
            .thenReturn(null);

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class,
            () -> cdamService.downloadFile(AUTH_TOKEN, SERVICE_AUTH_TOKEN, bundleDocument));
        assertEquals("Could not access the binary. HTTP response: null", exception.getMessage());
    }

    @Test
    void downloadFileWhenCdamReturnsNonOkStatusThrowsException() {
        BundleDocument bundleDocument = createBundleDocument(VALID_DOC_URI);
        ResponseEntity<Resource> errorResponse = ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID))
            .thenReturn(errorResponse);

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class,
            () -> cdamService.downloadFile(AUTH_TOKEN, SERVICE_AUTH_TOKEN, bundleDocument));
        assertEquals("Could not access the binary. HTTP response: 403 FORBIDDEN", exception.getMessage());
    }

    @Test
    void downloadFileWhenResponseBodyIsNullThrowsException() {
        BundleDocument bundleDocument = createBundleDocument(VALID_DOC_URI);
        ResponseEntity<Resource> responseEntityWithNullBody = ResponseEntity.ok(null);
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID))
            .thenReturn(responseEntityWithNullBody);

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class,
            () -> cdamService.downloadFile(AUTH_TOKEN, SERVICE_AUTH_TOKEN, bundleDocument));
        assertEquals("Could not access the binary. HTTP response: 200 OK", exception.getMessage());
    }

    @Test
    void downloadFileWhenInputStreamThrowsIoExceptionThrowsException() throws IOException {
        BundleDocument bundleDocument = createBundleDocument(VALID_DOC_URI);
        ResponseEntity<Resource> responseEntity = ResponseEntity.ok(byteArrayResource);

        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID))
            .thenReturn(responseEntity);
        when(byteArrayResource.getInputStream()).thenThrow(new IOException("test stream error"));

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class,
            () -> cdamService.downloadFile(AUTH_TOKEN, SERVICE_AUTH_TOKEN, bundleDocument));
        assertEquals("Could not download the file from CDAM", exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void downloadFileWhenGetMetadataThrowsExceptionThrowsException() throws IOException {
        BundleDocument bundleDocument = createBundleDocument(VALID_DOC_URI);
        InputStream mockInputStream = mock(InputStream.class);
        ResponseEntity<Resource> responseEntity = ResponseEntity.ok(byteArrayResource);

        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID))
            .thenReturn(responseEntity);
        when(byteArrayResource.getInputStream()).thenReturn(mockInputStream);
        when(caseDocumentClientApi.getMetadataForDocument(AUTH_TOKEN, SERVICE_AUTH_TOKEN, DOC_STORE_UUID))
            .thenThrow(new RuntimeException("metadata feign error"));

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class,
            () -> cdamService.downloadFile(AUTH_TOKEN, SERVICE_AUTH_TOKEN, bundleDocument));
        assertEquals("metadata feign error", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }

    @Test
    void uploadDocumentsSuccessful() throws DocumentTaskProcessingException {
        DocumentTask documentTask = populateDocumentTask();
        Document testDoc = Document.builder().originalDocumentName("template1.docx")
            .hashToken("token")
            .links(getLinks())
            .build();

        when(authTokenGenerator.generate()).thenReturn("s2s-token");
        when(caseDocumentClientApi.uploadDocuments(
            anyString(),
            anyString(),
            any(DocumentUploadRequest.class)))
            .thenReturn(uploadResponse);
        when(uploadResponse.getDocuments()).thenReturn(List.of(testDoc));

        cdamService.uploadDocuments(mockFile, documentTask);

        verify(authTokenGenerator).generate();
        verify(caseDocumentClientApi).uploadDocuments(
            Mockito.eq(documentTask.getJwt()),
            Mockito.eq("s2s-token"),
            any(DocumentUploadRequest.class));

        assertNotNull(documentTask.getBundle().getHashToken());
        assertEquals("token", documentTask.getBundle().getHashToken());
        assertNotNull(documentTask.getBundle().getStitchedDocumentURI());
        assertEquals(getLinks().self.href, documentTask.getBundle().getStitchedDocumentURI());
    }

    @Test
    void uploadDocumentsWhenReadFileToByteArrayThrowsIoExceptionThrowsException() throws IOException {
        DocumentTask documentTask = populateDocumentTask();
        File tempDirForTest = Files.createTempDirectory("testDirForUploadFail").toFile();
        tempDirForTest.deleteOnExit();

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class,
            () -> cdamService.uploadDocuments(tempDirForTest, documentTask));

        assertEquals("Could not upload the file to CDAM", exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void uploadDocumentsWhenAuthTokenGeneratorThrowsExceptionThrowsException() {
        DocumentTask documentTask = populateDocumentTask();
        when(authTokenGenerator.generate()).thenThrow(new RuntimeException("s2s generation error"));

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class,
            () -> cdamService.uploadDocuments(mockFile, documentTask));

        assertEquals("s2s generation error", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
        verify(caseDocumentClientApi, never()).uploadDocuments(any(), any(), any());
    }

    @Test
    void uploadDocumentsWhenCdamUploadThrowsExceptionThrowsException() {
        DocumentTask documentTask = populateDocumentTask();
        when(authTokenGenerator.generate()).thenReturn("s2s-token");
        when(caseDocumentClientApi.uploadDocuments(anyString(), anyString(), any(DocumentUploadRequest.class)))
            .thenThrow(new RuntimeException("cdam upload feign error"));

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class,
            () -> cdamService.uploadDocuments(mockFile, documentTask));

        assertEquals("cdam upload feign error", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }

    static Document.Links getLinks() {
        Document.Links links = new Document.Links();
        Document.Link self = new Document.Link();
        Document.Link binary = new Document.Link();

        var selfLink = "http://localhost:samplefile/" + DOC_STORE_UUID;
        var binaryLink = "http://localhost:samplefile/" + DOC_STORE_UUID + "/binary";

        self.href = selfLink;
        binary.href = binaryLink;

        links.self = self;
        links.binary = binary;

        return links;
    }
}

