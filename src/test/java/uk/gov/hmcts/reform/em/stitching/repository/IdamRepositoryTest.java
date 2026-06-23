package uk.gov.hmcts.reform.em.stitching.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.AssertionErrors.assertNull;

class IdamRepositoryTest {

    @Mock
    private IdamClient idamClient;

    private IdamRepository idamRepository;

    private static final  String FORE_NAME = "ABC";
    private static final  String SURNAME = "XYZ";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        idamRepository = new IdamRepository(idamClient);
    }

    @Test
    void getUserDetailsTestSuccess() {

        final UserInfo userInfo = UserInfo.builder()
            .uid("100")
            .givenName(FORE_NAME)
            .familyName(SURNAME)
            .roles(asList("Admin", "CaseWorker"))
            .build();
        Mockito.when(idamClient.getUserInfo(anyString())).thenReturn(userInfo);
        String rawToken = secure().next(5, true, false);

        assertEquals(FORE_NAME,  idamRepository.getUserInfo(rawToken).getGivenName());
        assertEquals(SURNAME,  idamRepository.getUserInfo(rawToken).getFamilyName());

        verify(idamClient, times(2)).getUserInfo(eq("Bearer " + rawToken));
    }

    @Test
    void getUserInfoAddsBearerPrefixWhenMissing() {
        String rawToken = "eyJrawToken";
        Mockito.when(idamClient.getUserInfo("Bearer " + rawToken)).thenReturn(UserInfo.builder().uid("1").build());

        idamRepository.getUserInfo(rawToken);

        verify(idamClient).getUserInfo(eq("Bearer " + rawToken));
    }

    @Test
    void getUserInfoDoesNotDoublePrefixWhenBearerAlreadyPresent() {
        String bearerToken = "Bearer eyJrawToken";
        Mockito.when(idamClient.getUserInfo(bearerToken)).thenReturn(UserInfo.builder().uid("1").build());

        idamRepository.getUserInfo(bearerToken);

        verify(idamClient).getUserInfo(eq(bearerToken));
    }

    @Test
    void getUserDetailsTestFailure() {

        String token = "randomValue";

        assertNull(FORE_NAME,  idamRepository.getUserInfo(token));
    }

}

