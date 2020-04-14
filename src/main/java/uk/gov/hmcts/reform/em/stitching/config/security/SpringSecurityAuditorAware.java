package uk.gov.hmcts.reform.em.stitching.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.stitching.config.Constants;

import java.util.Optional;

/**
 * Implementation of AuditorAware based on Spring Security.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    private final SecurityUtils securityUtils;

    @Autowired
    public SpringSecurityAuditorAware(final SecurityUtils securityUtils){
        this.securityUtils = securityUtils;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(securityUtils.getCurrentUserLogin().orElse(Constants.SYSTEM_ACCOUNT));
    }
}
