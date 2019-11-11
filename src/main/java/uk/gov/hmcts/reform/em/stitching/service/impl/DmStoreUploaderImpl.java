package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;

import java.io.File;
import java.io.IOException;

import static uk.gov.hmcts.reform.em.stitching.service.StringFormattingUtils.ensureStringEndsWithSuffix;

@Service
public class DmStoreUploaderImpl implements DmStoreUploader {

    private final Logger log = LoggerFactory.getLogger(DmStoreUploaderImpl.class);

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private final SubjectResolver<User> userResolver;

    private final String dmStoreAppBaseUrl;

    private static final String ENDPOINT = "/documents";

    public DmStoreUploaderImpl(OkHttpClient okHttpClient,
                               AuthTokenGenerator authTokenGenerator,
                               @Value("${dm-store-app.base-url}") String dmStoreAppBaseUrl,
                               SubjectResolver<User> userResolver) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
        this.dmStoreAppBaseUrl = dmStoreAppBaseUrl;
        this.userResolver = userResolver;
    }

    @Override
    public void uploadFile(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {
        if (documentTask.getBundle().getStitchedDocumentURI() != null) {
            uploadNewDocumentVersion(file, documentTask);
        } else {
            uploadNewDocument(file, documentTask);
        }
    }

    private void uploadNewDocument(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {
        try {

            log.info("Uploading new document '{}' for {}", file.getName(), documentTask.toString());

            MultipartBody requestBody = new MultipartBody
                    .Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("classification", "PUBLIC")
                    .addFormDataPart(
                        "files",
                        ensureStringEndsWithSuffix(file.getName(), ".pdf"),
                        RequestBody.create(MediaType.get("application/pdf"), file))
                    .build();

            Request request = new Request.Builder()
                    .addHeader("user-id", getUserId(documentTask))
                    .addHeader("user-roles", "caseworker")
                    .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                    .url(dmStoreAppBaseUrl + ENDPOINT)
                    .method("POST", requestBody)
                    .build();

            Response response = okHttpClient.newCall(request).execute();

            if (response.isSuccessful()) {

                JSONObject jsonObject = new JSONObject(response.body().string());
                String documentUri = jsonObject
                        .getJSONObject("_embedded")
                        .getJSONArray("documents")
                        .getJSONObject(0)
                        .getJSONObject("_links")
                        .getJSONObject("self")
                        .getString("href");

                documentTask.getBundle().setStitchedDocumentURI(documentUri);
            } else {
                throw new DocumentTaskProcessingException("Upload failed. Response code: " + response.code());
            }

        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException(String.format("Upload failed:  %s", e.getMessage()), e);
        }
    }

    private void uploadNewDocumentVersion(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {
        try {

            log.info("Uploading new document version '{}' for {}", file.getName(), documentTask.toString());

            MultipartBody requestBody = new MultipartBody
                    .Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.get("application/pdf"), file))
                    .build();

            Request request = new Request.Builder()
                    .addHeader("user-id", getUserId(documentTask))
                    .addHeader("user-roles", "caseworker")
                    .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                    .url(documentTask.getBundle().getStitchedDocumentURI())
                    .method("POST", requestBody)
                    .build();

            Response response = okHttpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new DocumentTaskProcessingException("Upload failed. Response code: " + response.code());
            }

        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException("Upload failed", e);
        }
    }

    private String getUserId(DocumentTask documentTask) {
        User user = userResolver.getTokenDetails(documentTask.getJwt());
        return user.getPrincipal();
    }

}
