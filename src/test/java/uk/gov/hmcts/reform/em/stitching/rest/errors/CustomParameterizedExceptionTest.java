package uk.gov.hmcts.reform.em.stitching.rest.errors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test class for the CustomParameterizedException controller advice.
 *
 * @see CustomParameterizedException
 */
@RunWith(SpringRunner.class)
public class CustomParameterizedExceptionTest {


    @Test
    public void testToParamMap() {
        Assert.assertTrue(CustomParameterizedException.toParamMap(null).isEmpty());
    }
}
