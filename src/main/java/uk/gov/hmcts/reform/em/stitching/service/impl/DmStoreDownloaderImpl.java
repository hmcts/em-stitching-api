package uk.gov.hmcts.reform.em.stitching.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static pl.touk.throwing.ThrowingFunction.unchecked;
import static uk.gov.hmcts.reform.em.stitching.service.CloseableCloser.close;

@Service
public class DmStoreDownloaderImpl implements DmStoreDownloader {

    private final Logger log = LoggerFactory.getLogger(DmStoreDownloaderImpl.class);

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private final DmStoreUriFormatter dmStoreUriFormatter;

    private final ObjectMapper objectMapper;

    private final IdamClient idamClient;

    public DmStoreDownloaderImpl(OkHttpClient okHttpClient,
                                 AuthTokenGenerator authTokenGenerator,
                                 DmStoreUriFormatter dmStoreUriFormatter,
                                 ObjectMapper objectMapper,
                                 IdamClient idamClient) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
        this.dmStoreUriFormatter = dmStoreUriFormatter;
        this.objectMapper = objectMapper;
        this.idamClient = idamClient;
    }

    @Override
    public Stream<Pair<BundleDocument, FileAndMediaType>> downloadFiles(Stream<BundleDocument> bundleDocuments,
                                                                        String jwt) {
        UserInfo userInfo = idamClient.getUserInfo(jwt);
        String userId = userInfo.getUid();
        String userRoles = String.join(",", userInfo.getRoles());
        return bundleDocuments
            .parallel()
            .map(unchecked(bundleDocument -> downloadFile(bundleDocument, userId, userRoles)));
    }

    private Pair<BundleDocument, FileAndMediaType> downloadFile(BundleDocument bundleDocument,
                                                                String userId, String userRoles)
            throws DocumentTaskProcessingException {
        Response getDocumentMetaDataResponse = null;
        Response getDocumentContentResponse =  null;
        try {

            getDocumentMetaDataResponse = getDocumentStoreResponse(bundleDocument.getDocumentURI(), userId, userRoles);

            if (getDocumentMetaDataResponse.isSuccessful()) {

                JsonNode documentMetaData = objectMapper.readTree(getDocumentMetaDataResponse.body().byteStream());

                String documentBinaryUrl = documentMetaData.get("_links").get("binary").get("href").asText();

                log.debug("Accessing documentBinaryUrl: {}", documentBinaryUrl);

                getDocumentContentResponse = getDocumentStoreResponse(documentBinaryUrl, userId, userRoles);

                if (getDocumentContentResponse.isSuccessful()) {
                    return Pair.of(bundleDocument,
                            new FileAndMediaType(copyResponseToFile(getDocumentContentResponse, documentBinaryUrl),
                                MediaType.get(documentMetaData.get("mimeType").asText())));
                } else {
                    throw new DocumentTaskProcessingException(
                            "Could not access the binary. HTTP response: " + getDocumentContentResponse.code());
                }

            } else {
                throw new DocumentTaskProcessingException(
                        "Could not access the meta-data. HTTP response: " + getDocumentMetaDataResponse.code());
            }

        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException("Could not access the binary: " + e.getMessage(), e);
        } finally {
            close(getDocumentMetaDataResponse);
            close(getDocumentContentResponse);
        }
    }

    private Response getDocumentStoreResponse(String documentUri, String userId, String userRoles)
            throws IOException {

        String fixedUrl = dmStoreUriFormatter.formatDmStoreUri(documentUri);

        log.debug("getDocumentStoreResponse - URL: {}", fixedUrl);

        return okHttpClient.newCall(new Request.Builder()
                .addHeader("user-id", userId)
                .addHeader("user-roles", userRoles)
                .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                .url(fixedUrl)
                .build()).execute();
    }

    private File copyResponseToFile(Response response, String documentBinaryUrl)
            throws DocumentTaskProcessingException {
        try {
            File tempFile = File.createTempFile("dm-store", ".tmp");
            Files.copy(response.body().byteStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Downloaded file from {} to temp location {}", documentBinaryUrl, tempFile.getName());
            return tempFile;
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not copy the file to a temp location", e);
        }
    }

}
