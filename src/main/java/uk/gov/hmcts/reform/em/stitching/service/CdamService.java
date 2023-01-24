package uk.gov.hmcts.reform.em.stitching.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.dto.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;
import uk.gov.hmcts.reform.em.stitching.service.impl.FileAndMediaType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static pl.touk.throwing.ThrowingFunction.unchecked;

@Service
public class CdamService {
    private final Logger log = LoggerFactory.getLogger(CdamService.class);

    @Autowired
    private CaseDocumentClientApi caseDocumentClientApi;

    public Stream<Pair<BundleDocument, FileAndMediaType>> downloadFiles(DocumentTask documentTask) {
        return documentTask.getBundle().getSortedDocuments()
            .parallel()
            .map(unchecked(bundleDocument -> downloadFile(documentTask.getJwt(), documentTask.getServiceAuth(), bundleDocument)));
    }

    public Pair<BundleDocument, FileAndMediaType> downloadFile(String auth, String serviceAuth, BundleDocument bundleDocument) throws
            DocumentTaskProcessingException {
        String docId = bundleDocument.getDocumentURI().substring(bundleDocument.getDocumentURI().lastIndexOf('/') + 1);
        UUID documentId = UUID.fromString(docId);
        ResponseEntity<Resource> response =  caseDocumentClientApi.getDocumentBinary(auth, serviceAuth, documentId);
        HttpStatus status = null;

        try {
            if (Objects.nonNull(response)) {
                status = response.getStatusCode();
                var byteArrayResource = (ByteArrayResource) response.getBody();
                if (Objects.nonNull(byteArrayResource)) {
                    try (var inputStream = byteArrayResource.getInputStream()) {
                        Document document = caseDocumentClientApi.getMetadataForDocument(auth, serviceAuth, documentId);
                        var originalDocumentName = document.originalDocumentName;
                        var fileType = FilenameUtils.getExtension(originalDocumentName);
                        var fileName = "document." + fileType;
                        File file = copyResponseToFile(inputStream, fileName);
                        return Pair.of(bundleDocument,
                            new FileAndMediaType(file, okhttp3.MediaType.get(document.mimeType)));
                    }
                }
            }
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not download the file from CDAM", e);
        } catch (Exception e) {
            throw new DocumentTaskProcessingException(e.getMessage(), e);
        }

        throw new DocumentTaskProcessingException(String.format("Could not access the binary. HTTP response: %s",
                status));
    }

    private File copyResponseToFile(InputStream inputStream, String fileName) throws DocumentTaskProcessingException {
        try {

            var tempDir = Files.createTempDirectory("pg",
                PosixFilePermissions.asFileAttribute(EnumSet.allOf(PosixFilePermission.class)));
            var tempFile = new File(tempDir.toAbsolutePath().toFile(), fileName);

            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not copy the file to a temp location", e);
        }
    }

    public void uploadDocuments(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {

        try {
            ByteArrayMultipartFile multipartFile =
                ByteArrayMultipartFile.builder()
                    .content(FileUtils.readFileToByteArray(file))
                    .name(file.getName() + ".pdf")
                    .contentType(MediaType.valueOf("application/pdf"))
                .build();

            DocumentUploadRequest documentUploadRequest = new DocumentUploadRequest(Classification.PUBLIC.toString(),
                documentTask.getCaseTypeId(), documentTask.getJurisdictionId(),
                Arrays.asList(multipartFile));

            UploadResponse uploadResponse = caseDocumentClientApi.uploadDocuments(documentTask.getJwt(),
                documentTask.getServiceAuth(),
                documentUploadRequest);
            Document document = uploadResponse.getDocuments().get(0);

            documentTask.getBundle().setHashToken(document.hashToken);
            documentTask.getBundle().setStitchedDocumentURI(document.links.self.href);
            log.info("uploaded doc ref {},name {}", document.originalDocumentName, document.links.self.href);
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not upload the file to CDAM", e);
        } catch (Exception e) {
            throw new DocumentTaskProcessingException(e.getMessage(), e);
        }
    }
}
