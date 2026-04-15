package uk.gov.hmcts.reform.em.stitching.functional;

import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryExtension;

@SpringBootTest(classes = {TestUtil.class})
@TestPropertySource(value = "classpath:application.yml")
@ExtendWith(SerenityJUnit5Extension.class)
@WithTags({@WithTag("testType:Functional")})
@SuppressWarnings("java:S5960")
public abstract class BaseTest {

    protected final TestUtil testUtil;

    @RegisterExtension
    RetryExtension retryExtension = new RetryExtension(3);

    protected BaseTest(TestUtil testUtil) {
        this.testUtil = testUtil;
    }
}