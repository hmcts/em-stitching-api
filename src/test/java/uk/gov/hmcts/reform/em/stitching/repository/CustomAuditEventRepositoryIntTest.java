package uk.gov.hmcts.reform.em.stitching.repository;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.config.Constants;
import uk.gov.hmcts.reform.em.stitching.config.audit.AuditEventConverter;
import uk.gov.hmcts.reform.em.stitching.domain.PersistentAuditEvent;
import uk.gov.hmcts.reform.em.stitching.rest.TestSecurityConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.em.stitching.repository.CustomAuditEventRepository.EVENT_DATA_COLUMN_MAX_LENGTH;

/**
 * Test class for the CustomAuditEventRepository class.
 *
 * @see CustomAuditEventRepository
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class CustomAuditEventRepositoryIntTest {

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private AuditEventConverter auditEventConverter;

    private CustomAuditEventRepository customAuditEventRepository;

    private PersistentAuditEvent testUserEvent;

    private PersistentAuditEvent testOtherUserEvent;

    private PersistentAuditEvent testOldUserEvent;

    @BeforeEach
    void setup() {
        customAuditEventRepository = new CustomAuditEventRepository(
                persistenceAuditEventRepository,
                auditEventConverter);
        persistenceAuditEventRepository.deleteAll();
        testUserEvent = new PersistentAuditEvent();
        testUserEvent.setPrincipal("test-user");
        testUserEvent.setAuditEventType("test-type");
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        testUserEvent.setAuditEventDate(oneHourAgo);
        Map<String, String> data = new HashMap<>();
        data.put("test-key", "test-value");
        testUserEvent.setData(data);

        testOldUserEvent = new PersistentAuditEvent();
        testOldUserEvent.setPrincipal("test-user");
        testOldUserEvent.setAuditEventType("test-type");
        testOldUserEvent.setAuditEventDate(oneHourAgo.minusSeconds(10000));

        testOtherUserEvent = new PersistentAuditEvent();
        testOtherUserEvent.setPrincipal("other-test-user");
        testOtherUserEvent.setAuditEventType("test-type");
        testOtherUserEvent.setAuditEventDate(oneHourAgo);
    }

    @Test
    void addAuditEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(1,(persistentAuditEvents).size());
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();
        assertEquals(persistentAuditEvent.getPrincipal(), event.getPrincipal());
        assertEquals(persistentAuditEvent.getAuditEventType(), event.getType());
        assertTrue(persistentAuditEvent.getData().containsKey("test-key"));
        assertEquals("test-value", persistentAuditEvent.getData().get("test-key"));
        assertEquals(persistentAuditEvent.getAuditEventDate().truncatedTo(ChronoUnit.DAYS),
                event.getTimestamp().truncatedTo(ChronoUnit.DAYS));
        assertEquals(persistentAuditEvent.getAuditEventDate().truncatedTo(ChronoUnit.MINUTES),
                event.getTimestamp().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(persistentAuditEvent.getAuditEventDate().truncatedTo(ChronoUnit.SECONDS),
                event.getTimestamp().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void addAuditEventTruncateLargeData() {
        Map<String, Object> data = new HashMap<>();
        StringBuilder largeData = new StringBuilder();
        largeData.append("a".repeat(EVENT_DATA_COLUMN_MAX_LENGTH + 10));
        data.put("test-key", largeData);
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(1,(persistentAuditEvents).size());
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();
        assertEquals(persistentAuditEvent.getPrincipal(), event.getPrincipal());
        assertEquals(persistentAuditEvent.getAuditEventType(), event.getType());
        assertTrue(persistentAuditEvent.getData().containsKey("test-key"));
        String actualData = persistentAuditEvent.getData().get("test-key");
        assertEquals(EVENT_DATA_COLUMN_MAX_LENGTH, actualData.length());
        assertTrue(largeData.toString().contains(actualData));
        assertEquals(persistentAuditEvent.getAuditEventDate().truncatedTo(ChronoUnit.DAYS),
                event.getTimestamp().truncatedTo(ChronoUnit.DAYS));
        assertEquals(persistentAuditEvent.getAuditEventDate().truncatedTo(ChronoUnit.MINUTES),
                event.getTimestamp().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(persistentAuditEvent.getAuditEventDate().truncatedTo(ChronoUnit.SECONDS),
                event.getTimestamp().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void testAddEventWithWebAuthenticationDetails() {
        HttpSession session = new MockHttpSession(null, "test-session-id");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.setRemoteAddr("1.2.3.4");
        WebAuthenticationDetails details = new WebAuthenticationDetails(request);
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", details);
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(1,(persistentAuditEvents).size());
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();
        assertTrue(persistentAuditEvent.getData().containsKey("remoteAddress"));
        assertEquals("1.2.3.4", persistentAuditEvent.getData().get("remoteAddress"));
        assertTrue(persistentAuditEvent.getData().containsKey("sessionId"));
        assertEquals("test-session-id", persistentAuditEvent.getData().get("sessionId"));
    }

    @Test
    void testAddEventWithNullData() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", null);
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(1,(persistentAuditEvents).size());
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();
        assertTrue(persistentAuditEvent.getData().containsKey("test-key"));
        assertEquals("null", persistentAuditEvent.getData().get("test-key"));
    }

    @Test
    void addAuditEventWithAnonymousUser() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent(Constants.ANONYMOUS_USER, "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertTrue((persistentAuditEvents).isEmpty());
    }

    @Test
    void addAuditEventWithAuthorizationFailureType() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent("test-user", "AUTHORIZATION_FAILURE", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertTrue((persistentAuditEvents).isEmpty());
    }

}
