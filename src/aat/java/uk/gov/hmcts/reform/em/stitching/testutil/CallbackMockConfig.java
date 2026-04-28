package uk.gov.hmcts.reform.em.stitching.testutil;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@TestConfiguration
public class CallbackMockConfig {

    private static final Logger log = LoggerFactory.getLogger(CallbackMockConfig.class);

    @Bean(destroyMethod = "stop")
    @ConditionalOnProperty(name = "Ï.host", havingValue = "localhost", matchIfMissing = true)
    public WireMockServer callbackWireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(
                WireMockConfiguration.options()
                        .notifier(new ConsoleNotifier(true))
        );

        try {
            wireMockServer.start();
            wireMockServer.stubFor(post(urlPathMatching("/api/stitching-complete-callback.*"))
                    .willReturn(aResponse().withStatus(200)));
            log.info("WireMock successfully started for local testing.");
        } catch (FatalStartupException e) {
            log.warn("WireMock could not bind to port. Degrading gracefully. Reason: {}", e.getMessage());
        }

        return wireMockServer;
    }
}