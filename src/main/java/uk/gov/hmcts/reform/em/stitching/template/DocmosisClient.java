package uk.gov.hmcts.reform.em.stitching.template;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;
import javax.imageio.ImageIO;

import static uk.gov.hmcts.reform.em.stitching.service.CloseableCloser.close;

@Component
public class DocmosisClient {

    private final Logger logger = LoggerFactory.getLogger(DocmosisClient.class);

    @Value("${docmosis.render.endpoint}")
    private String docmosisRenderEndpoint;

    @Value("${docmosis.accessKey}")
    private String docmosisAccessKey;

    private final OkHttpClient client;

    @Autowired
    public DocmosisClient(@Autowired OkHttpClient client) {
        this.client = client;
    }

    public File renderDocmosisTemplate(
            String templateId,
            JsonNode payload)
            throws IOException, DocumentTaskProcessingException {
        Response response = null;
        FileOutputStream fileOutputStream = null;
        try {
            String tempFileName = String.format("%s%s",
                                                UUID.randomUUID().toString(), ".pdf"
            );

            MultipartBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("templateName", templateId)
                .addFormDataPart("accessKey", docmosisAccessKey)
                .addFormDataPart("outputName", tempFileName)
                .addFormDataPart("data", String.valueOf(payload))
                .build();

            Request request = new Request.Builder()
                .url(docmosisRenderEndpoint)
                .method("POST", requestBody)
                .build();

            response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                File file = Files.createTempFile("docmosis-rendition", ".pdf").toFile();
                file.deleteOnExit();
                fileOutputStream = new FileOutputStream(file);
                copyStream(response.body().byteStream(), fileOutputStream);
                return file;
            } else {
                String responseMsg = String.format(
                    "Could not render Cover Page template with Id : %s . Error: %s "
                        + "with response msg: %s ",
                    templateId,
                    response.code(),
                    response.body().string()
                );
                logger.error(responseMsg);
                throw new DocumentTaskProcessingException(responseMsg);
            }
        } finally {
            close(fileOutputStream);
            close(response);
        }
    }

    public File getDocmosisImage(String assetId) throws IOException, DocumentTaskProcessingException {
        Response response = null;
        try {
            String tempFileName = String.format("%s%s", UUID.randomUUID().toString(), ".pdf");
            MultipartBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("templateName", assetId)
                .addFormDataPart("accessKey", docmosisAccessKey)
                .addFormDataPart("outputName", tempFileName)
                .build();

            Request request = new Request.Builder()
                .url(docmosisRenderEndpoint)
                .method("POST", requestBody)
                .build();

            response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                return createWatermarkFile(response);
            } else {
                throw new DocumentTaskProcessingException(
                    "Could not retrieve Watermark Image from Docmosis. Error: " + response.body().string());
            }
        } finally {
            close(response);
        }
    }

    private File createWatermarkFile(Response response) throws IOException {
        File file = Files.createTempFile("watermark-page", ".pdf").toFile();
        File watermarkFile = Files.createTempFile("watermark-image", ".png").toFile();
        file.deleteOnExit();
        watermarkFile.deleteOnExit();


        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            copyStream(response.body().byteStream(), fileOutputStream);

            PDDocument waterMarkDocument = PDDocument.load(file);
            PDPage page = waterMarkDocument.getPage(waterMarkDocument.getNumberOfPages() - 1);
            PDResources resources = page.getResources();

            COSName name = resources.getXObjectNames().iterator().next();
            PDXObject documentObject = resources.getXObject(name);

            if (documentObject instanceof PDImageXObject documentImage) {
                ImageIO.write(documentImage.getImage(), "png", watermarkFile);
            }
            waterMarkDocument.close();
        }

        return watermarkFile;
    }

    private void copyStream(InputStream in, OutputStream out) {
        try {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            logger.error("Could not close the resource : {}", e.getMessage());
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }
}
