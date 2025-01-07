package uk.gov.hmcts.reform.em.stitching.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.config.audit.AuditEventConverter;
import uk.gov.hmcts.reform.em.stitching.rest.TestSecurityConfiguration;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class CustomAuditEventRepositoryTest {

    @MockBean
    PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockBean
    AuditEventConverter auditEventConverter;

    @Test
    void add() {
        CustomAuditEventRepository repository = new CustomAuditEventRepository(
            persistenceAuditEventRepository,
            auditEventConverter
        );

        AuditEvent event = new AuditEvent("principal", "type", "data");

        repository.add(event);
        List<AuditEvent> result = repository.find("principal", Instant.MIN, "type");
        assertEquals(0, result.size());
    }
}
