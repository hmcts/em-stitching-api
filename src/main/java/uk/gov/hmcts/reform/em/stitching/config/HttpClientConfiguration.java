package uk.gov.hmcts.reform.em.stitching.config;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import okhttp3.OkHttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class HttpClientConfiguration {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build();
    }

    @Bean
    public Client getFeignHttpClient(
        @Value("${feign.httpClient.connectTimeout}") final int connectTimeout,
        @Value("${feign.httpClient.socketTimeout}") final int socketTimeout
    ) {
        return new ApacheHttpClient(getHttpClient(connectTimeout, socketTimeout));
    }

    private CloseableHttpClient getHttpClient(int connectTimeout, int socketTimeout) {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setConnectionRequestTimeout(connectTimeout)
            .setSocketTimeout(socketTimeout)
            .build();

        return HttpClientBuilder
            .create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .build();
    }

}
