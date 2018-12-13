package uk.gov.hmcts.reform.em.bundling.smoke;

import io.restassured.RestAssured;
import org.junit.Test;
import uk.gov.hmcts.reform.em.bundling.testutil.Env;

public class SmokeTest {

    @Test
    public void testHealthEndpoint() {

        RestAssured.useRelaxedHTTPSValidation();

        RestAssured.given()
            .request("GET", Env.getTestUrl() + "/health")
            .then()
            .statusCode(200);


    }
}
