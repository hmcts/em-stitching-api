package uk.gov.hmcts.reform.em.stitching.rest.errors;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for the ExceptionTranslator controller advice.
 *
 * @see FieldErrorVM
 */
public class FieldErrorVMTest {

    private final String objectName = "Name";

    private final String field = "Field";

    private final String message = "Message";

    private FieldErrorVM fieldErrorVM;

    @Test
    public void testConstructor() {

        fieldErrorVM = new FieldErrorVM(objectName,field,message);

        Assert.assertEquals(objectName, fieldErrorVM.getObjectName());
        Assert.assertEquals(field, fieldErrorVM.getField());
        Assert.assertEquals(message, fieldErrorVM.getMessage());

    }

}
