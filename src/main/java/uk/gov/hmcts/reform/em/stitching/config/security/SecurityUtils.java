package uk.gov.hmcts.reform.em.stitching.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.repository.IdamRepository;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Optional;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;

/**
 * Utility class for Spring Security.
 */
@Service
public class SecurityUtils {

    public static final String TOKEN_NAME = "tokenName";

    private final IdamRepository idamRepository;

    @Autowired
    public SecurityUtils(final IdamRepository idamRepository) {
        this.idamRepository = idamRepository;
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user
     */
    public Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return Optional.ofNullable(securityContext.getAuthentication())
            .map(authentication -> switch (authentication) {
                case Authentication auth when auth.getPrincipal() instanceof UserDetails userDetails ->
                    userDetails.getUsername();

                case Authentication auth when auth.getPrincipal() instanceof String s -> s;

                case JwtAuthenticationToken jwtAuth -> {
                    Jwt jwt = jwtAuth.getToken();
                    if (jwt.hasClaim(TOKEN_NAME)
                        && jwt.getClaim(TOKEN_NAME).equals(ACCESS_TOKEN)) {
                        UserInfo userInfo = idamRepository.getUserInfo(jwt.getTokenValue());
                        yield userInfo.getUid();
                    }
                    yield null;
                }

                case Authentication auth when auth.getPrincipal() instanceof DefaultOidcUser oidcUser ->
                    (String) oidcUser.getAttributes().get("preferred_username");

                default -> null;
            });
    }

}
