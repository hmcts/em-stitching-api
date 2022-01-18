package uk.gov.hmcts.reform.em.stitching.smoke;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

import java.util.Random;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Smoke")})
public class SmokeTest {

    private static final String MESSAGE = "Welcome to Stitching API!";

    @Autowired
    private TestUtil testUtil;

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

        Assert.assertEquals(MESSAGE, response);
    }

    @Test
    public void invokeJava17RandomClass() {
        Random random = new Random();
        int number = random.nextInt(10);
    }
}
