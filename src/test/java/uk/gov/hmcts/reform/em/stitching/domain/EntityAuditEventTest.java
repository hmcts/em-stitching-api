package uk.gov.hmcts.reform.em.stitching.domain;

import org.junit.Test;

import java.time.Instant;
import java.util.Objects;

import static org.junit.Assert.*;

public class EntityAuditEventTest {

    @Test
    public void testSettersAndEqualsHash() {
        EntityAuditEvent entityAuditEvent = new EntityAuditEvent();
        entityAuditEvent.setId(1L);
        entityAuditEvent.setEntityId(1L);
        entityAuditEvent.setEntityType("T");
        entityAuditEvent.setCommitVersion(1);
        entityAuditEvent.setModifiedBy("u");
        entityAuditEvent.setModifiedDate(Instant.MIN);
        entityAuditEvent.setAction("a");

        assertEquals(1L, entityAuditEvent.getId().longValue());
        assertEquals(1L, entityAuditEvent.getEntityId().longValue());
        assertEquals("T", entityAuditEvent.getEntityType());
        assertEquals(1, entityAuditEvent.getCommitVersion().intValue());
        assertEquals("u", entityAuditEvent.getModifiedBy());
        assertEquals(Instant.MIN, entityAuditEvent.getModifiedDate());
        assertEquals("a", entityAuditEvent.getAction());
        assertEquals(Objects.hashCode(1L), entityAuditEvent.hashCode());

        EntityAuditEvent entityAuditEvent2 = new EntityAuditEvent();
        entityAuditEvent2.setId(1L);
        assertEquals(entityAuditEvent, entityAuditEvent2);

        EntityAuditEvent entityAuditEvent3 = new EntityAuditEvent();
        entityAuditEvent3.setId(2L);
        assertNotEquals(entityAuditEvent, entityAuditEvent3);

        assertNotEquals(entityAuditEvent, null);

        assertEquals(entityAuditEvent, entityAuditEvent);

        assertNotEquals(new Callback(), new Callback());

        assertNotEquals(null, entityAuditEvent3);

        assertNotEquals(new Callback(), new Object());

        assertNotEquals(new Callback(), entityAuditEvent);
        assertNotEquals(entityAuditEvent, new Callback());
    }

}
