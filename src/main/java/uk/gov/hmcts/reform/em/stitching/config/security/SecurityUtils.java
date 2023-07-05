package uk.gov.hmcts.reform.em.stitching.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.repository.IdamRepository;

import java.util.Map;
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
                .map(authentication -> {
                    if (authentication.getPrincipal() instanceof UserDetails) {
                        UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
                        return springSecurityUser.getUsername();
                    } else if (authentication.getPrincipal() instanceof String) {
                        return (String) authentication.getPrincipal();
                    } else if (authentication instanceof JwtAuthenticationToken) {
                        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
                        if (ACCESS_TOKEN.equals(jwt.getClaim(TOKEN_NAME))) {
                            uk.gov.hmcts.reform.idam.client.models.UserInfo userInfo = idamRepository.getUserInfo(jwt.getTokenValue());
                            return userInfo.getUid();
                        }
                    } else if (authentication.getPrincipal() instanceof DefaultOidcUser) {
                        Map<String, Object> attributes = ((DefaultOidcUser) authentication.getPrincipal()).getAttributes();
                        if (attributes.containsKey("preferred_username")) {
                            return (String) attributes.get("preferred_username");
                        }
                    }
                    return null;
                });
    }

}
