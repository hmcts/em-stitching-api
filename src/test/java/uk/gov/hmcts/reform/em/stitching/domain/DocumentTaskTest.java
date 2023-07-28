package uk.gov.hmcts.reform.em.stitching.domain;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DocumentTaskTest {

    @Test
    public void failureDescription() {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setFailureDescription("x");
        assertEquals("x", documentTask.getFailureDescription());
    }


    @Test
    public void toStringTest() {
        DocumentTask documentTask = new DocumentTask();
        String toString = documentTask.toString();
        assertEquals(
                "DocumentTask(id=null, "
                        + "bundle=null, "
                        + "taskState=NEW, "
                        + "failureDescription=null, "
                        + "jwt=null, "
                        + "callback=null, "
                        + "version=0)",
                toString
        );
    }
}
