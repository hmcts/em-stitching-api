package uk.gov.hmcts.reform.em.stitching.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

@Component
public class IdamRepository {

    @Autowired
    private IdamClient idamClient;

    @Autowired
    public IdamRepository(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    public UserDetails getUserDetails(String jwtToken) {
        return idamClient.getUserDetails("Bearer " + jwtToken);
    }
}
