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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static pl.touk.throwing.ThrowingFunction.unchecked;

@Service
public class DmStoreDownloaderImpl implements DmStoreDownloader {

    private final Logger log = LoggerFactory.getLogger(DmStoreDownloaderImpl.class);

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private final DmStoreUriFormatter dmStoreUriFormatter;

    private final ObjectMapper objectMapper;

    public DmStoreDownloaderImpl(OkHttpClient okHttpClient,
                                 AuthTokenGenerator authTokenGenerator,
                                 DmStoreUriFormatter dmStoreUriFormatter,
                                 ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
        this.dmStoreUriFormatter = dmStoreUriFormatter;
        this.objectMapper = objectMapper;
    }

    @Override
    public Stream<Pair<BundleDocument, FileAndMediaType>> downloadFiles(Stream<BundleDocument> bundleDocuments) {
        return bundleDocuments
            .parallel()
            .map(unchecked(this::downloadFile));
    }

    private Pair<BundleDocument, FileAndMediaType> downloadFile(BundleDocument bundleDocument)
            throws DocumentTaskProcessingException {
        return Pair.of(bundleDocument, downloadFile(bundleDocument.getDocumentURI()));
    }

    @Override
    public FileAndMediaType downloadFile(String documentURI)
        throws DocumentTaskProcessingException {

        try {

            Response getDocumentMetaDataResponse = getDocumentStoreResponse(documentURI);

            if (getDocumentMetaDataResponse.isSuccessful()) {

                JsonNode documentMetaData = objectMapper.readTree(getDocumentMetaDataResponse.body().byteStream());

                log.info("Accessing binary of the DM document: {}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documentMetaData));

                String documentBinaryUrl = documentMetaData.get("_links").get("binary").get("href").asText();

                log.info("Accessing documentBinaryUrl: {}", documentBinaryUrl);

                Response getDocumentContentResponse = getDocumentStoreResponse(documentBinaryUrl);

                if (getDocumentContentResponse.isSuccessful()) {
                    return new FileAndMediaType(copyResponseToFile(getDocumentContentResponse),
                            MediaType.get(documentMetaData.get("mimeType").asText()));
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
        }
    }

    private Response getDocumentStoreResponse(String documentUri) throws IOException {

        String fixedUrl = dmStoreUriFormatter.formatDmStoreUri(documentUri);

        log.info("getDocumentStoreResponse - URL: {}", fixedUrl);

        return okHttpClient.newCall(new Request.Builder()
                .addHeader("user-roles", "caseworker")
                .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                .url(fixedUrl)
                .build()).execute();
    }

    private File copyResponseToFile(Response response) throws DocumentTaskProcessingException {
        try {
            File tempFile = File.createTempFile("dm-store", ".tmp");
            Files.copy(response.body().byteStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not copy the file to a temp location", e);
        }
    }

}
