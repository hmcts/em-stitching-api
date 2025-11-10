package uk.gov.hmcts.reform.em.stitching.smoke;

import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@ExtendWith(SerenityJUnit5Extension.class)
@WithTags({@WithTag("testType:Smoke")})
public class SmokeTest {

    private static final String MESSAGE = "{\"message\":\"Welcome to Stitching API!\"}";

    private final TestUtil testUtil;

    @Autowired
    public SmokeTest(TestUtil testUtil) {
        this.testUtil = testUtil;
    }

    @Test
    public void testHealthEndpoint() {

        SerenityRest.useRelaxedHTTPSValidation();

        String response =
                SerenityRest
                        .given()
                        .baseUri(testUtil.getTestUrl())
                        .when()
                        .get("/")
                        .then()
                        .statusCode(200).extract().body().asString();

        assertEquals(MESSAGE, response);
    }
}
