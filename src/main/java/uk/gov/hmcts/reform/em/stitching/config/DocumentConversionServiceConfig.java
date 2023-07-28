package uk.gov.hmcts.reform.em.stitching.config;

import com.google.common.collect.Lists;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.em.stitching.conversion.DocmosisConverter;
import uk.gov.hmcts.reform.em.stitching.conversion.ImageConverter;
import uk.gov.hmcts.reform.em.stitching.conversion.PDFConverter;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentConversionServiceImpl;

@Configuration
public class DocumentConversionServiceConfig {

    @Bean
    public DocumentConversionServiceImpl getConversionService(
        @Value("${docmosis.accessKey}") String docmosisAccessKey,
        @Value("${docmosis.convert.endpoint}") String docmosisConvertEndpoint,
        @Autowired OkHttpClient httpClient
    ) {
        return new DocumentConversionServiceImpl(
            Lists.newArrayList(
                new PDFConverter(),
                new DocmosisConverter(docmosisAccessKey, docmosisConvertEndpoint, httpClient),
                new ImageConverter()
            )
        );
    }
}
