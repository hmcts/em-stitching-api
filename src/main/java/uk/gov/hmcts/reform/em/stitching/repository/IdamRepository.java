package uk.gov.hmcts.reform.em.stitching.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

@Component
public class IdamRepository {

    @Autowired
    private IdamClient idamClient;

    @Autowired
    public IdamRepository(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    public UserInfo getUserInfo(String jwtToken) {
        return idamClient.getUserInfo("Bearer " + jwtToken);
    }
}
