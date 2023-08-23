package uk.gov.hmcts.reform.em.stitching.conversion;

import com.google.common.collect.Lists;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static uk.gov.hmcts.reform.em.stitching.service.HttpOkResponseCloser.closeResponse;

/**
 * Converts word doc,docx,excel,power point files to PDF using the Docmosis API.
 */
public class DocmosisConverter implements FileToPDFConverter {

    private final Logger logger = LoggerFactory.getLogger(DocmosisConverter.class);

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
             "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
             "application/vnd.ms-excel",
             "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
             "application/vnd.openxmlformats-officedocument.presentationml.presentation",
             "application/vnd.ms-powerpoint",
             "application/vnd.openxmlformats-officedocument.presentationml.template",
             "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
             "application/octet-stream",
             "text/plain",
             "application/rtf"
        );
    }

    @Override
    public File convert(File file) throws IOException {
        Response response = null;
        try {
            final Request request = this.createRequest(file);
            response = httpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                String responseMsg = String.format(
                        "Docmosis error code : (%s) for converting: %s with response msg: %s ",
                                                   response.code(),
                                                   file.getName(),
                                                   response.body().string()
                );
                logger.error(responseMsg);
                throw new IOException(responseMsg);
            }

            return createConvertedFile(response);
        } finally {
            closeResponse(response);
        }
    }

    private Request createRequest(final File file) {
        final String originalFileName = file.getName();
        final String convertedFileName = originalFileName + ".pdf";

        MultipartBody requestBody = new MultipartBody
            .Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("accessKey", docmosisAccessKey)
            .addFormDataPart("outputName", convertedFileName)
                .addFormDataPart("file", originalFileName, RequestBody.create(file, MediaType.get(PDF_CONTENT_TYPE)))
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
