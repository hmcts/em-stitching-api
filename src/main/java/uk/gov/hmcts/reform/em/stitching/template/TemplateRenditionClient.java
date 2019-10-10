package uk.gov.hmcts.reform.em.stitching.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;
import uk.gov.hmcts.reform.em.stitching.service.impl.FileAndMediaType;

import java.io.IOException;

@Component
public class TemplateRenditionClient {

    private final OkHttpClient client;
    private ObjectMapper mapper;

    @Value("doc-assembly-app.base-url")
    private String documentAssemblyUrl;

    private final DmStoreDownloader dmStoreDownloader;
    private static final String ENDPOINT = "/template-renditions";

    @Autowired
    public TemplateRenditionClient(OkHttpClient client,
                                   ObjectMapper objectMapper,
                                   DmStoreDownloader dmStoreDownloader) {
        this.client = client;
        this.mapper = objectMapper;
        this.dmStoreDownloader = dmStoreDownloader;
    }

    public FileAndMediaType renderTemplate(JsonNode payload, String jwtToken)
            throws IOException, DocumentTaskProcessingException {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        final ObjectNode node = factory.objectNode();
        node.set("formPayload", payload);
        node.put("outputType", "PDF");

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), node.toString());

        Request request = new Request.Builder()
                .addHeader("Authorisation", jwtToken)
                .addHeader("ContentType", "application/json")
                .url(documentAssemblyUrl + ENDPOINT)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            JsonNode responseBody = mapper.readTree(response.body().string());
            String templateLocation = responseBody.get("renditionOutputLocation").asText();
            return dmStoreDownloader.downloadFile(templateLocation);
        } else {
            throw new DocumentTaskProcessingException(
                "Could not render Cover Page template. HTTP response: " + response.code());
        }
    }
}
