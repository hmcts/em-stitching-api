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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@Component
public class DocmosisClient {

    @Value("${docmosis.render.endpoint}")
    private String docmosisRenderEndpoint;

    @Value("${docmosis.accessKey}")
    private String docmosisAccessKey;

    private final OkHttpClient client;

    @Autowired
    public DocmosisClient(@Autowired OkHttpClient client) {
        this.client = client;
    }

    public File renderDocmosisTemplate(String templateId, JsonNode payload) throws IOException, DocumentTaskProcessingException {
        String tempFileName = String.format("%s%s",
                UUID.randomUUID().toString(), ".pdf");

        MultipartBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "templateName",
                        templateId)
                .addFormDataPart(
                        "accessKey",
                        docmosisAccessKey)
                .addFormDataPart(
                        "outputName",
                        tempFileName)
                .addFormDataPart(
                        "data",
                        String.valueOf(payload))
                .build();

        Request request = new Request.Builder()
                .url(docmosisRenderEndpoint)
                .method("POST", requestBody)
                .build();

        Response response =  client.newCall(request).execute();

        if (response.isSuccessful()) {
            File file = File.createTempFile(
                    "docmosis-rendition",
                    ".pdf");

            copyAndClose(response.body().byteStream(), new FileOutputStream(file), response);

            return file;
        } else {
            response.close();
            throw new DocumentTaskProcessingException(
                    "Could not render Cover Page template. Error: " + response.body().string());
        }
    }

    public File getDocmosisImage(String assetId) throws IOException, DocumentTaskProcessingException {
        String tempFileName = String.format("%s%s",
                UUID.randomUUID().toString(), ".pdf");

        MultipartBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "templateName",
                        assetId)
                .addFormDataPart(
                        "accessKey",
                        docmosisAccessKey)
                .addFormDataPart(
                        "outputName",
                        tempFileName)
                .build();

        Request request = new Request.Builder()
                .url(docmosisRenderEndpoint)
                .method("POST", requestBody)
                .build();

        Response response =  client.newCall(request).execute();

        if (response.isSuccessful()) {
            File file = File.createTempFile(
                    "watermark-page", ".pdf");

            copyAndClose(response.body().byteStream(), new FileOutputStream(file), response);

            PDDocument waterMarkDocument = PDDocument.load(file);
            PDPage page = waterMarkDocument.getPage(waterMarkDocument.getNumberOfPages() - 1);
            PDResources resources = page.getResources();

            COSName name = resources.getXObjectNames().iterator().next();
            PDXObject documentObject = resources.getXObject(name);
            File watermarkFile = File.createTempFile("watermark-image", ".png");

            if (documentObject instanceof PDImageXObject) {
                PDImageXObject documentImage = (PDImageXObject) documentObject;
                ImageIO.write(documentImage.getImage(), "png", watermarkFile);
            }

            return watermarkFile;
        } else {
            response.close();
            throw new DocumentTaskProcessingException(
                    "Could not retrieve Watermark Image from Docmosis. Error: " + response.body().string());
        }
    }

    private void copyAndClose(InputStream in, OutputStream out, Response response) throws IOException {
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
        response.close();
    }
}
