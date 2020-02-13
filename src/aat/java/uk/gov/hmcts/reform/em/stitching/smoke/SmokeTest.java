package uk.gov.hmcts.reform.em.stitching.smoke;

import io.restassured.RestAssured;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@PropertySource(value = "classpath:application.yml")
@RunWith(SpringRunner.class)
public class SmokeTest {

    @Autowired
    private TestUtil testUtil;

    @Test
    public void testHealthEndpoint() {

        RestAssured.useRelaxedHTTPSValidation();

        RestAssured.given()
                .request("GET", testUtil.getTestUrl() + "/health")
                .then()
                .statusCode(200);


    }
}
