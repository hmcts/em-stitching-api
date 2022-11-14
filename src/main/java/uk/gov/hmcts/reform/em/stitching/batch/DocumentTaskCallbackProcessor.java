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

import static uk.gov.hmcts.reform.em.stitching.service.HttpOkResponseCloser.closeResponse;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class DocumentTaskCallbackProcessor implements ItemProcessor<DocumentTask, DocumentTask> {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskCallbackProcessor.class);

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private final DocumentTaskMapper documentTaskMapper;

    private final ObjectMapper objectMapper;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Value("${stitching-complete.callback.max-attempts}")
    int callBackMaxAttempts;

    public DocumentTaskCallbackProcessor(OkHttpClient okHttpClient, AuthTokenGenerator authTokenGenerator,
                                         DocumentTaskMapper documentTaskMapper, ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
        this.documentTaskMapper = documentTaskMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public DocumentTask process(DocumentTask documentTask) {
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                    .addHeader("Authorization", documentTask.getJwt())
                    .url(documentTask.getCallback().getCallbackUrl())
                    .post(RequestBody.create(JSON,
                            objectMapper.writeValueAsString(documentTaskMapper.toDto(documentTask))))
                    .build();
            log.info("request url {}", request.url());
            log.info("request documentTask {}", documentTask);
            log.info("request body {} ", objectMapper.writeValueAsString(documentTaskMapper.toDto(documentTask)));

            response = okHttpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                documentTask.getCallback().setCallbackState(CallbackState.SUCCESS);
                log.info(String.format("Document Task#%d successfully executed callback#%d with Bundle-Id : %d",
                    documentTask.getId(),
                    documentTask.getCallback().getId(),
                    documentTask.getBundle().getId()));
                return documentTask;

            } else {

                int callBackAttempts = documentTask.getCallback().getAttempts();
                callBackAttempts++;

                var warnMessage = StringUtils.truncate(String.format("HTTP Callback failed.\nStatus: %d"
                        + ".\nBundle-Id : %d\nResponse Body: %s.",
                    response.code(),documentTask.getBundle().getId(),
                    response.body().toString()),5000);
                documentTask.getCallback().setFailureDescription(warnMessage);
                log.warn(warnMessage);
                documentTask.getCallback().setAttempts(callBackAttempts);

                if (callBackAttempts >= callBackMaxAttempts) {
                    var failedBundleDetails = String.format("Failed callback for Bundle-Id : %d"
                        + ". Document Task-Id : %d ", documentTask.getBundle().getId(), documentTask.getId());
                    log.error(failedBundleDetails);
                    documentTask.getCallback().setCallbackState(CallbackState.FAILURE);
                }

                return documentTask;

            }

        } catch (IOException e) {
            documentTask.getCallback().setCallbackState(CallbackState.FAILURE);
            log.error("IO Exception:", e);
        } catch (Exception ex){
            log.error("Exception:", ex);
            throw ex;

        } finally {
            closeResponse(response);
        }
        return documentTask;
    }

}
