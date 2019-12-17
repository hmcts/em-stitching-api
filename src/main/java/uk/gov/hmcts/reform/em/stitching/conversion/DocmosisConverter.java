package uk.gov.hmcts.reform.em.stitching.conversion;

import com.google.common.collect.Lists;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Converts word doc, docx,excel,power point files to PDF using the Docmosis API.
 */
public class DocmosisConverter implements FileToPDFConverter {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private final String docmosisAccessKey;
    private final String docmosisConvertEndpoint;
    private final OkHttpClient httpClient;

    public DocmosisConverter(String docmosisAccessKey, String docmosisConvertEndpoint, OkHttpClient httpClient) {
        this.docmosisAccessKey = docmosisAccessKey;
        this.docmosisConvertEndpoint = docmosisConvertEndpoint;
        this.httpClient = httpClient;
    }


    @Override
    public List<String> accepts() {
        return Lists.newArrayList(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/x-tika-ooxml",
            "application/x-tika-msoffice",
             //changes for story EM-2426
             "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
             "application/vnd.ms-excel",
             "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
             "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
             "application/vnd.openxmlformats-officedocument.presentationml.presentation",
             "application/vnd.ms-powerpoint",
             "application/vnd.openxmlformats-officedocument.presentationml.template",
             "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
        );
    }

    @Override
    public File convert(File file) throws IOException {
        final Request request = this.createRequest(file);
        final Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException(String.format("Docmosis error (%s) converting: %s", response.code(), file.getName()));
        }

        return createConvertedFile(response);
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
