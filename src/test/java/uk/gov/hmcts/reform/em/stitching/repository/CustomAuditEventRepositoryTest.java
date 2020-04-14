package uk.gov.hmcts.reform.em.stitching.repository;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.config.audit.AuditEventConverter;

import java.time.Instant;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class CustomAuditEventRepositoryTest {

    @MockBean
    PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockBean
    AuditEventConverter auditEventConverter;

    @Test
    public void add() {
        CustomAuditEventRepository repository = new CustomAuditEventRepository(
            persistenceAuditEventRepository,
            auditEventConverter
        );

        AuditEvent event = new AuditEvent("principal", "type", "data");

        repository.add(event);
        List<AuditEvent> result = repository.find("principal", Instant.MIN, "type");
        Assert.assertEquals(0, result.size());
    }
}
