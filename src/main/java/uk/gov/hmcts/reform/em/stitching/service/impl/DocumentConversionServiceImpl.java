package uk.gov.hmcts.reform.em.stitching.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
        if (originalFile.getName().toLowerCase().endsWith(".pdf")) {
            return originalFile;
        }

        final Request request = this.createRequest(originalFile);
        final Response response = httpClient.newCall(request).execute();
        final String body = response.body().string();

        if (response.isSuccessful()) {
            final File convertedFile = File.createTempFile("stitch-conversion", ".pdf");
            final FileWriter writer = new FileWriter(convertedFile);

            writer.write(body);
            writer.close();

            return convertedFile;
        }

        throw new IOException("Docmosis error converting " + originalFile.getName() + ":" + body);
    }

    private Request createRequest(final File file) {
        final String originalFileName = file.getName();
        final String convertedFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.')) + ".pdf";

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
}

