package uk.gov.hmcts.reform.em.stitching.domain;

import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.*;

public class CallbackTest {

    @Test
    public void testSettersAndEqualsHash() {
        Callback callback = new Callback();
        callback.setId(1l);
        callback.setVersion(1);
        callback.setFailureDescription("x");

        assertEquals(new Long(1), callback.getId());
        assertEquals(1, callback.getVersion());
        assertEquals("x", callback.getFailureDescription());
        assertEquals(Objects.hashCode(1l), callback.hashCode());

        callback.failureDescription("t");
        assertEquals("t", callback.getFailureDescription());

        Callback callback2 = new Callback();
        callback2.setId(1l);
        callback2.setVersion(1);
        callback2.setFailureDescription("x");
        assertEquals(callback, callback2);

        Callback callback3 = new Callback();
        callback3.setId(2l);
        callback3.setVersion(1);
        callback3.setFailureDescription("x");
        assertNotEquals(callback, callback3);

        assertNotEquals(callback, null);

        assertEquals(callback, callback);

        assertNotEquals(new Callback(), new Callback());

        assertNotEquals(null, callback3);

        assertNotEquals(new Callback(), new Object());

        assertNotEquals(new Callback(), callback);
        assertNotEquals(callback, new Callback());
    }

}
