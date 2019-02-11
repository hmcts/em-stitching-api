package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.*;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


@Service
@Transactional
public class DocumentConversionServiceImpl implements DocumentConversionService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
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
    public Pair<BundleDocument, File> convert(Pair<BundleDocument, File> pair) throws IOException {
        Tika tika = new Tika();
        String mimeType = tika.detect(pair.getSecond());

        if (mimeType.equals(PDF_CONTENT_TYPE)) {
            return pair;
        }

        final Request request = this.createRequest(pair.getSecond());
        final Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException(String.format("Docmosis error (%s) converting: %s", response.code(), pair.getFirst().getDocTitle()));
        }

        return Pair.of(pair.getFirst(), this.createConvertedFile(response));
    }

    private Request createRequest(final File file) {
        final String originalFileName = file.getName();
        final String convertedFileName = originalFileName + ".pdf";

        MultipartBody requestBody = new MultipartBody
            .Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("accessKey", docmosisAccessKey)
            .addFormDataPart("outputName", convertedFileName)
            .addFormDataPart("file", originalFileName, RequestBody.create(MediaType.get(PDF_CONTENT_TYPE), file))
            .build();

        return new Request.Builder()
            .header("Accept", PDF_CONTENT_TYPE)
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

