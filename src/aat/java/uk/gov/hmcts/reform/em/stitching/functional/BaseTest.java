package uk.gov.hmcts.reform.em.stitching.functional;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;


@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
public abstract class BaseTest {

    @Value("${test.retryCount}")
    private int retryCount;

    @Autowired
    TestUtil testUtil;


    @Rule
    public RetryRule retryRule = new RetryRule(retryCount);
}
