package uk.gov.hmcts.reform.em.stitching.config.security;

import feign.Client;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import static uk.gov.hmcts.reform.em.stitching.config.security.SecurityUtils.extractAuthorityFromClaims;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;
import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthorityExtractor extends JwtAuthenticationConverter {

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final Client httpClient;

    public static final String TOKEN_NAME = "tokenName";

    public JwtAuthorityExtractor(ClientRegistrationRepository clientRegistrationRepository, Client httpClient) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.httpClient = httpClient;
    }

    @Override
    protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (jwt.containsClaim("tokenName")) {
            if (jwt.getClaim("tokenName").equals(ACCESS_TOKEN)) {
                authorities = extractAuthorityFromClaims(getUserInfo(jwt.getTokenValue()));
            } else if (jwt.getClaim("tokenName").equals(ID_TOKEN)) {
                authorities = extractAuthorityFromClaims(jwt.getClaims());
            }
        }
        return authorities;
    }

    public Map<String, Object> getUserInfo(String authorization) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("oidc");
        String userInfoEndpointUri = registration.getProviderDetails()
                .getUserInfoEndpoint().getUri();
        return buildFeignClient(userInfoEndpointUri.replace("/userinfo", ""))
                .userInfo("Bearer " + authorization, null);
    }

    private UserInfoClient buildFeignClient(String target) {
        return Feign.builder()
                .client(httpClient)
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(UserInfoClient.class))
                .target(UserInfoClient.class, target);
    }
}
