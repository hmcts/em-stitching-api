package uk.gov.hmcts.reform.em.stitching.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class DocumentTaskTest {

    @Test
    public void failureDescription() {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setFailureDescription("x");
        assertEquals("x", documentTask.getFailureDescription());
    }

}
