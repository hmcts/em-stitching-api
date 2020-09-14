package uk.gov.hmcts.reform.em.stitching.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.CallbackState;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.io.IOException;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class DocumentTaskCallbackProcessor implements ItemProcessor<DocumentTask, DocumentTask> {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskCallbackProcessor.class);

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private final DocumentTaskMapper documentTaskMapper;

    private final ObjectMapper objectMapper;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Value("${stitching-complete.callback.delay-milliseconds}")
    long callBackDelayMilliseconds;

    public DocumentTaskCallbackProcessor(OkHttpClient okHttpClient, AuthTokenGenerator authTokenGenerator,
                                         DocumentTaskMapper documentTaskMapper, ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
        this.documentTaskMapper = documentTaskMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public DocumentTask process(DocumentTask documentTask) throws InterruptedException {
        try {

            Thread.sleep(callBackDelayMilliseconds);
            Request request = new Request.Builder()
                    .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                    .addHeader("Authorization", documentTask.getJwt())
                    .url(documentTask.getCallback().getCallbackUrl())
                    .post(RequestBody.create(JSON,
                            objectMapper.writeValueAsString(documentTaskMapper.toDto(documentTask))))
                    .build();

            Response response = okHttpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                documentTask.getCallback().setCallbackState(CallbackState.SUCCESS);
                log.info(String.format("Document Task#%d successfully executed callback#%d",
                        documentTask.getId(),
                        documentTask.getCallback().getId()));
            } else {
                documentTask.getCallback().setCallbackState(CallbackState.FAILURE);
                String errorMessage = StringUtils.truncate(String.format("HTTP Callback failed.\nStatus: %d.\nResponse Body: %s",
                        response.code(),
                        response.body().string()), 5000);
                documentTask.getCallback().setFailureDescription(errorMessage);
                log.error(errorMessage);
            }

        } catch (IOException e) {
            documentTask.getCallback().setCallbackState(CallbackState.FAILURE);
            String errorMessage = String.format("IO Exception: %s", e.getMessage());
            documentTask.getCallback().setFailureDescription(errorMessage);
            log.error(errorMessage, e);
        }
        return documentTask;
    }

}
