package uk.gov.hmcts.reform.em.stitching.repository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

public class IdamRepositoryTest {

    @Mock
    private IdamClient idamClient;

    private IdamRepository idamRepository;

    private static final  String FORE_NAME = "ABC";
    private static final  String SURNAME = "XYZ";
    private static final  String EMAIL = "user@test.com";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        idamRepository = new IdamRepository(idamClient);
    }

    @Test
    public void getUserDetailsTestSuccess() {

        UserDetails userDetails = UserDetails.builder()
                .forename(FORE_NAME)
                .surname(SURNAME)
                .email(EMAIL)
                .build();
        Mockito.when(idamClient.getUserDetails(Mockito.anyString())).thenReturn(userDetails);
        String token = "randomValue";

        Assert.assertEquals(FORE_NAME,  idamRepository.getUserDetails(token).getForename());
        Assert.assertEquals(SURNAME,  idamRepository.getUserDetails(token).getSurname().get());
        Assert.assertEquals(EMAIL,  idamRepository.getUserDetails(token).getEmail());
    }

    @Test
    public void getUserDetailsTestFailure() {

        String token = "randomValue";

        Assert.assertNull(FORE_NAME,  idamRepository.getUserDetails(token));
    }

}

