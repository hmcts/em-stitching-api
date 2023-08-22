package uk.gov.hmcts.reform.em.stitching.repository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IdamRepositoryTest {

    @Mock
    private IdamClient idamClient;

    private IdamRepository idamRepository;

    private static final  String FORE_NAME = "ABC";
    private static final  String SURNAME = "XYZ";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        idamRepository = new IdamRepository(idamClient);
    }

    @Test
    public void getUserDetailsTestSuccess() {

        final UserInfo userInfo = UserInfo.builder()
            .uid("100")
            .givenName(FORE_NAME)
            .familyName(SURNAME)
            .roles(asList("Admin", "CaseWorker"))
            .build();
        Mockito.when(idamClient.getUserInfo(Mockito.anyString())).thenReturn(userInfo);
        String token = random(5, true, false);

        Assert.assertEquals(FORE_NAME,  idamRepository.getUserInfo(token).getGivenName());
        Assert.assertEquals(SURNAME,  idamRepository.getUserInfo(token).getFamilyName());

        verify(idamClient, times(2)).getUserInfo(anyString());
    }

    @Test
    public void getUserDetailsTestFailure() {

        String token = "randomValue";

        Assert.assertNull(FORE_NAME,  idamRepository.getUserInfo(token));
    }

}

