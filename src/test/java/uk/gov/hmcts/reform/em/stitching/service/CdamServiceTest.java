package uk.gov.hmcts.reform.em.stitching.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;
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
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private List<Document> documents;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private Document document;

    private static final UUID docStoreUUID = UUID.randomUUID();

    @BeforeEach
    public void setup() {
        cdamService = new CdamService(caseDocumentClientApi, authTokenGenerator);
        document = Document.builder()
            .originalDocumentName("one-page.pdf")
            .mimeType("application/pdf")
            .build();
    }

    @Test
    void downloadFiles() {

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("http://localhost:samplefile/" + docStoreUUID);
        bundleDocument.setId(1234566L);
        bundleDocument.setDocDescription("TestBundleDescription");
        bundleDocument.setDocTitle("BundleTitle");
        DocumentTask documentTask = populateRequestBody();
        documentTask.getBundle().setDocuments(List.of(bundleDocument));
        documentTask.setServiceAuth("serviceAuth");
        Stream<Pair<BundleDocument, FileAndMediaType>> pairStream = cdamService.downloadFiles(documentTask);

        assertNotNull(pairStream);
    }

    @Test
    void downloadFile() throws Exception {

        File mockFile = new File("src/test/resources/one-page.pdf");
        InputStream inputStream = new FileInputStream(mockFile);

        when(caseDocumentClientApi.getMetadataForDocument("xxx", "serviceAuth", docStoreUUID))
            .thenReturn(document);
        ResponseEntity responseEntity = ResponseEntity.accepted().body(byteArrayResource);
        when(byteArrayResource.getInputStream()).thenReturn(inputStream);
        when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID))
                .thenReturn(responseEntity);

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("http://localhost:samplefile/" + docStoreUUID);
        Pair<BundleDocument, FileAndMediaType> resultPair =
                cdamService.downloadFile("xxx", "serviceAuth", bundleDocument);


        verify(caseDocumentClientApi, Mockito.atLeast(1))
                .getDocumentBinary("xxx", "serviceAuth", docStoreUUID);
        verify(caseDocumentClientApi, Mockito.atLeast(1))
                .getMetadataForDocument("xxx", "serviceAuth", docStoreUUID);

        assertNotNull(resultPair.getSecond().getFile());
        var cdamFile = resultPair.getSecond().getFile();
        assertEquals("one-page.pdf", cdamFile.getName());
    }

    @Test
    void testDownloadFileNullResponseBodyThrowsDocumentTaskProcessingException()  {

        ResponseEntity responseEntity = ResponseEntity.accepted().body(null);
        when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID))
                .thenReturn(responseEntity);
        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("http://localhost:samplefile/" + docStoreUUID);

        assertThrows(DocumentTaskProcessingException.class,
                () -> cdamService.downloadFile("xxx", "serviceAuth", bundleDocument));
    }

    @Test
    void testUploadDocuments() throws DocumentTaskProcessingException {
        Document testDoc = Document.builder().originalDocumentName("template1.docx")
            .hashToken("token")
            .links(getLinks())
            .build();

        when(caseDocumentClientApi.uploadDocuments(
                any(),
                any(),
                any(DocumentUploadRequest.class)))
                .thenReturn(uploadResponse
            );
        when(uploadResponse.getDocuments()).thenReturn(documents);
        when(uploadResponse.getDocuments().get(0)).thenReturn(testDoc);

        File mockFile = new File("src/test/resources/wordDocument2.docx");
        DocumentTask documentTask = populateRequestBody();

        cdamService.uploadDocuments(mockFile, documentTask);

        verify(caseDocumentClientApi, Mockito.atLeast(1))
                .uploadDocuments(any(), any(), any(DocumentUploadRequest.class));

        assertNotNull(documentTask.getBundle().getHashToken());
        assertNotNull(documentTask.getBundle().getStitchedDocumentURI());
    }

    private DocumentTask populateRequestBody() {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setJurisdictionId("PUBLICLAW");
        documentTask.setCaseTypeId("XYZ");

        documentTask.setBundle(new Bundle());

        return documentTask;
    }

    static Document.Links getLinks() {
        Document.Links links = new Document.Links();

        Document.Link self = new Document.Link();
        Document.Link binary = new Document.Link();

        var selfLink = "http://localhost:samplefile/" + docStoreUUID;
        var binaryLink = "http://localhost:samplefile/" + docStoreUUID + "/binary";

        self.href = selfLink;
        binary.href = binaryLink;

        links.self = self;
        links.binary = binary;

        return links;
    }
}

