package uk.gov.hmcts.reform.em.stitching.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;

@Service
public class DocumentConversionServiceImpl implements DocumentConversionService {

    private final String docmosisAccessKey;
    private final String docmosisConvertEndpoint;
    private final OkHttpClient httpClient;

    public DocumentConversionServiceImpl(
        @Value("${docmosis.accessKey}") String docmosisAccessKey,
        @Value("${docmosis.convert.endpoint}") String docmosisConvertEndpoint,
        OkHttpClient httpClient
    ) {
        this.docmosisAccessKey = docmosisAccessKey;
        this.docmosisConvertEndpoint = docmosisConvertEndpoint;
        this.httpClient = httpClient;
    }

    @Override
    public File convert(File originalFile) throws IOException {
        String contentType = Files.probeContentType(originalFile.toPath());

        if (contentType.equals("application/pdf")) {
            return originalFile;
        }

        final Request request = this.createRequest(originalFile);
        final Response response = httpClient.newCall(request).execute();

        if (response.isSuccessful()) {
            return this.createConvertedFile(response);
        }

        throw new IOException("Docmosis error converting " + originalFile.getName() + ":" + response.body().string());
    }

    private Request createRequest(final File file) {
        final String originalFileName = file.getName();
        final String convertedFileName = originalFileName + ".pdf";

        MultipartBody requestBody = new MultipartBody
            .Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("accessKey", docmosisAccessKey)
            .addFormDataPart("outputName", convertedFileName)
            .addFormDataPart("file", originalFileName, RequestBody.create(MediaType.get("application/pdf"), file))
            .build();

        return new Request.Builder()
            .header("Accept", "application/pdf")
            .url(docmosisConvertEndpoint)
            .method("POST", requestBody)
            .build();
    }

    private File createConvertedFile(Response response) throws IOException {
        final File convertedFile = File.createTempFile("stitch-conversion", ".pdf");

        Files.write(convertedFile.toPath(), response.body().bytes());

        return convertedFile;
    }
}

