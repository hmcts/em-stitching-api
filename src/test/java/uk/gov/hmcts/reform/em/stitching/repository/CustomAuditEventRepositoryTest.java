package uk.gov.hmcts.reform.em.stitching.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.audit.AuditEvent;
import uk.gov.hmcts.reform.em.stitching.config.Constants;
import uk.gov.hmcts.reform.em.stitching.config.audit.AuditEventConverter;
import uk.gov.hmcts.reform.em.stitching.domain.PersistentAuditEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAuditEventRepositoryTest {

    @Mock
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Mock
    private AuditEventConverter auditEventConverter;

    @InjectMocks
    private CustomAuditEventRepository customAuditEventRepository;

    @Captor
    private ArgumentCaptor<PersistentAuditEvent> persistentEventCaptor;

    @Test
    void addEventWithLongDataShouldTruncateValue() {
        String longValue = "a".repeat(300);
        Map<String, Object> data = new HashMap<>();
        data.put("testKey", longValue);

        Map<String, String> stringData = new HashMap<>();
        stringData.put("testKey", longValue);

        AuditEvent event = new AuditEvent("principal", "type", data);

        when(auditEventConverter.convertDataToStrings(data)).thenReturn(stringData);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository).save(persistentEventCaptor.capture());
        PersistentAuditEvent savedEvent = persistentEventCaptor.getValue();

        assertNotNull(savedEvent);
        assertEquals(1, savedEvent.getData().size());

        String savedValue = savedEvent.getData().get("testKey");
        assertEquals(255, savedValue.length());
        assertEquals(longValue.substring(0, 255), savedValue);
    }

    @Test
    void addEventWithNormalDataShouldNotTruncate() {
        String normalValue = "Short value";
        Map<String, Object> data = new HashMap<>();
        data.put("key", normalValue);

        Map<String, String> stringData = new HashMap<>();
        stringData.put("key", normalValue);

        AuditEvent event = new AuditEvent("principal", "type", data);

        when(auditEventConverter.convertDataToStrings(data)).thenReturn(stringData);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository).save(persistentEventCaptor.capture());
        PersistentAuditEvent savedEvent = persistentEventCaptor.getValue();

        assertEquals(normalValue, savedEvent.getData().get("key"));
    }

    @Test
    void addEventShouldHandleNullValuesGracefully() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", null);

        Map<String, String> stringData = new HashMap<>();
        stringData.put("key", null);

        AuditEvent event = new AuditEvent("principal", "type", data);

        when(auditEventConverter.convertDataToStrings(data)).thenReturn(stringData);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository).save(persistentEventCaptor.capture());
        PersistentAuditEvent savedEvent = persistentEventCaptor.getValue();

        assertNull(savedEvent.getData().get("key"));
    }

    @Test
    void addShouldIgnoreAuthorizationFailureEvents() {
        AuditEvent event = new AuditEvent("principal", "AUTHORIZATION_FAILURE", Collections.emptyMap());

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository, never()).save(any());
    }

    @Test
    void addShouldIgnoreAnonymousUserEvents() {
        AuditEvent event = new AuditEvent(Constants.ANONYMOUS_USER, "SOME_TYPE", Collections.emptyMap());

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository, never()).save(any());
    }

    @Test
    void findShouldDelegateToRepositoryAndConverter() {
        String principal = "user";
        String type = "LOGIN";
        Instant after = Instant.now();
        PersistentAuditEvent persistentEvent = new PersistentAuditEvent();
        List<PersistentAuditEvent> persistentList = List.of(persistentEvent);
        List<AuditEvent> expectedList = List.of(new AuditEvent(principal, type, Collections.emptyMap()));

        when(persistenceAuditEventRepository
            .findByPrincipalAndAuditEventDateAfterAndAuditEventType(principal, after, type))
            .thenReturn(persistentList);
        when(auditEventConverter.convertToAuditEvent(persistentList)).thenReturn(expectedList);

        List<AuditEvent> result = customAuditEventRepository.find(principal, after, type);

        assertEquals(expectedList, result);
        verify(persistenceAuditEventRepository)
            .findByPrincipalAndAuditEventDateAfterAndAuditEventType(principal, after, type);
        verify(auditEventConverter).convertToAuditEvent(persistentList);
    }
}