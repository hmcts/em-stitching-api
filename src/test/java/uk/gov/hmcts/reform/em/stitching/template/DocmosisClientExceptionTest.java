package uk.gov.hmcts.reform.em.stitching.template;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull; // Ensure this is imported
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class DocmosisClientExceptionTest {

    private DocmosisClient clientForHttpErrorTests;
    private static final String MOCK_TEMPLATE_ID = "some-template-id";
    private static final String MOCK_ASSET_ID = "some-asset-id";

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        OkHttpClient okHttpClientForHttpErrors = new OkHttpClient
            .Builder()
            .addInterceptor(DocmosisClientExceptionTest::interceptHttpError)
            .build();

        clientForHttpErrorTests = new DocmosisClient(okHttpClientForHttpErrors);
        ReflectionTestUtils.setField(clientForHttpErrorTests,
            "docmosisRenderEndpoint", "http://example.org");
        ReflectionTestUtils.setField(clientForHttpErrorTests, "docmosisAccessKey", "key");

        Logger docmosisClientLogger = (Logger) LoggerFactory.getLogger(DocmosisClient.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        docmosisClientLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        Logger docmosisClientLogger = (Logger) LoggerFactory.getLogger(DocmosisClient.class);
        if (listAppender != null) {
            docmosisClientLogger.detachAppender(listAppender);
            listAppender.stop();
            listAppender.list.clear();
        }
    }

    private static Response interceptHttpError(Interceptor.Chain chain) {
        return new Response.Builder()
            .body(ResponseBody.create(new byte[0], MediaType.get("application/pdf")))
            .request(chain.request())
            .message("Error!")
            .code(400)
            .protocol(Protocol.HTTP_2)
            .build();
    }

    private static Response interceptWithSimulatedStreamError(Interceptor.Chain chain) throws IOException {
        InputStream mockInputStream = mock(InputStream.class);
        IOException simulatedException = new IOException("Simulated stream read error");

        when(mockInputStream.read()).thenThrow(simulatedException);
        when(mockInputStream.read(any(byte[].class))).thenThrow(simulatedException);
        when(mockInputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(simulatedException);

        ResponseBody realResponseBody = ResponseBody.create(new byte[0], MediaType.get("application/pdf"));
        ResponseBody spyResponseBody = spy(realResponseBody);
        doReturn(mockInputStream).when(spyResponseBody).byteStream();

        return new Response.Builder()
            .body(spyResponseBody)
            .request(chain.request())
            .message("OK")
            .code(200)
            .protocol(Protocol.HTTP_2)
            .build();
    }

    @Test
    void renderTemplateShouldThrowDTPExceptionOnHttpError() {
        assertThrows(DocumentTaskProcessingException.class, () ->
            clientForHttpErrorTests.renderDocmosisTemplate(MOCK_TEMPLATE_ID,
                JsonNodeFactory.instance.objectNode().put("caseNo", "12345")));
    }

    @Test
    void getDocmosisImageShouldThrowDTPExceptionOnHttpError() {
        assertThrows(DocumentTaskProcessingException.class, () ->
            clientForHttpErrorTests.getDocmosisImage(MOCK_ASSET_ID));
    }

    @Test
    void renderDocmosisTemplateShouldLogWhenIOExceptionOccursInCopyStreamAndNotThrow() {
        OkHttpClient specificOkHttpClient = new OkHttpClient.Builder()
            .addInterceptor(DocmosisClientExceptionTest::interceptWithSimulatedStreamError)
            .build();
        DocmosisClient specificClient = new DocmosisClient(specificOkHttpClient);
        ReflectionTestUtils.setField(specificClient, "docmosisRenderEndpoint", "http://example.org");
        ReflectionTestUtils.setField(specificClient, "docmosisAccessKey", "key");

        assertDoesNotThrow(() -> specificClient.renderDocmosisTemplate(
                MOCK_TEMPLATE_ID,
                JsonNodeFactory.instance.objectNode().put("caseNo", "12345")
            )
        );

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR
                    && event.getFormattedMessage().equals("Could not close the resource : Simulated stream read error"))
        );
    }

    @Test
    void getDocmosisImageShouldLogAndThrowIOExceptionWhenCopyStreamFailsAndPdfProcessingFails() {
        OkHttpClient specificOkHttpClient = new OkHttpClient.Builder()
            .addInterceptor(DocmosisClientExceptionTest::interceptWithSimulatedStreamError)
            .build();
        DocmosisClient specificClient = new DocmosisClient(specificOkHttpClient);
        ReflectionTestUtils.setField(specificClient, "docmosisRenderEndpoint", "http://example.org");
        ReflectionTestUtils.setField(specificClient, "docmosisAccessKey", "key");

        IOException thrownException = assertThrows(IOException.class, () ->
            specificClient.getDocmosisImage(MOCK_ASSET_ID)
        );

        assertNotNull(thrownException.getMessage(), "Thrown IOException message should not be null.");
        assertTrue(thrownException.getMessage().startsWith("Error:")
        );

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR
                    && event.getFormattedMessage().equals("Could not close the resource : Simulated stream read error"))
        );
    }
}