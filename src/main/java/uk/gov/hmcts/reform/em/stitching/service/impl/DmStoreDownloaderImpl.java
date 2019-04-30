package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    public DmStoreDownloaderImpl(OkHttpClient okHttpClient, AuthTokenGenerator authTokenGenerator) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
    }

    @Override
    public Stream<Pair<BundleDocument, File>> downloadFiles(Stream<BundleDocument> bundleDocuments) {
        return bundleDocuments
            .parallel()
            .map(unchecked(this::downloadFile));
    }

    private Pair<BundleDocument, File> downloadFile(BundleDocument bundleDocument)
        throws DocumentTaskProcessingException {

        try {
            Request request = new Request.Builder()
                    .addHeader("user-roles", "caseworker")
                    .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                    .url(documentURIWithBinarySuffix(bundleDocument))
                    .build();

            Response response = okHttpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                File file = copyResponseToFile(response);

                return Pair.of(bundleDocument, file);
            } else {
                throw new DocumentTaskProcessingException(
                    "Could not access the binary. HTTP response: " + response.code()
                );
            }
        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException("Could not access the binary: " + e.getMessage(), e);
        }
    }

    private String documentURIWithBinarySuffix(BundleDocument bundleDocument) {
        return bundleDocument.getDocumentURI().endsWith("/binary") ? bundleDocument.getDocumentURI() : bundleDocument.getDocumentURI() + "/binary";
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
