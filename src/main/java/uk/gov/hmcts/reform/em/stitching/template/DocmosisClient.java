package uk.gov.hmcts.reform.em.stitching.template;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
            IOUtils.copy(response.body().byteStream(), new FileOutputStream(file));
            return file;
        } else {
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
            IOUtils.copy(response.body().byteStream(), new FileOutputStream(file));

            PDDocument waterMarkDocument = PDDocument.load(file);
            PDPage page = waterMarkDocument.getPage(waterMarkDocument.getNumberOfPages() - 1);
            PDResources resources = page.getResources();

            COSName name = resources.getXObjectNames().iterator().next();
            PDXObject o = resources.getXObject(name);
            File watermarkFile = File.createTempFile("watermark-image", ".png");

            if (o instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject) o;
                ImageIO.write(image.getImage(), "png", watermarkFile);
            }

            return watermarkFile;
        } else {
            throw new DocumentTaskProcessingException(
                    "Could not retrieve Docmosis Template. Error: " + response.body().string());
        }
    }
}
