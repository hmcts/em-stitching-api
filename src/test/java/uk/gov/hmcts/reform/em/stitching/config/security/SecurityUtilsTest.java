package uk.gov.hmcts.reform.em.stitching.config.security;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.reform.em.stitching.repository.IdamRepository;

import java.util.HashMap;
import java.util.Map;

public class SecurityUtilsTest {

    private IdamRepository idamRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @Mock
    private JwtAuthenticationToken jwtAuthenticationToken;

    @Mock
    private DefaultOidcUser defaultOidcUser;

    private SecurityUtils securityUtils;

    @Before
    public void setup() {

        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        securityUtils = new SecurityUtils(idamRepository);
    }

    @Test
    public void getCurrentUserLoginNoAuthentication() {
        Assert.assertFalse(securityUtils.getCurrentUserLogin().isPresent());
    }

    @Test
    public void getCurrentUserLoginUserDetails() {

        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);
        Mockito.when(userDetails.getUsername()).thenReturn("aabb");

        Assert.assertEquals("aabb",  securityUtils.getCurrentUserLogin().get());
    }

    @Test
    public void getCurrentUserLoginString() {

        Mockito.when(authentication.getPrincipal()).thenReturn("xyz");

        Assert.assertEquals("xyz",  securityUtils.getCurrentUserLogin().get());
    }

    @Test
    public void getCurrentUserLoginDefaultOidcUser() {

        Mockito.when(authentication.getPrincipal()).thenReturn(defaultOidcUser);
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("preferred_username","testUser");
        Mockito.when(defaultOidcUser.getAttributes()).thenReturn(attributes);
        Assert.assertEquals("testUser",  securityUtils.getCurrentUserLogin().get());
    }

}
