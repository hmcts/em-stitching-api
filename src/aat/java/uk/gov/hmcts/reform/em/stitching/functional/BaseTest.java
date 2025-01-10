package uk.gov.hmcts.reform.em.stitching.functional;

import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

@SpringBootTest(classes = {TestUtil.class})
@TestPropertySource(value = "classpath:application.yml")
@ExtendWith(SerenityJUnit5Extension.class)
@WithTags({@WithTag("testType:Functional")})
public abstract class BaseTest {

    @Autowired
    TestUtil testUtil;

}
