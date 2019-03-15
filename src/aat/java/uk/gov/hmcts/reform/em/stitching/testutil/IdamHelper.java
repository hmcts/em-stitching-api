package uk.gov.hmcts.reform.em.stitching.testutil;

import io.restassured.RestAssured;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;

@Service
public class IdamHelper {

    private static final String USERNAME = "testytesttest@test.net";
    private static final String PASSWORD = "4590fgvhbfgbDdffm3lk4j";

    @Autowired
    private IdamClient client;

    @Value("${idam.api.url}")
    private String idamUrl;

    @Value("${idam.client.secret}")
    private String secret;

    @Value("${idam.client.redirect_uri}")
    private String redirect;

    public String getIdamToken() {
        createUser();

        return client.authenticateUser(USERNAME, PASSWORD);
    }

    private void createUser() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", USERNAME);
        jsonObject.put("password", PASSWORD);
        jsonObject.put("forename", "test");
        jsonObject.put("lastname", "test");

        RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(jsonObject.toString())
                .post(idamUrl + "/testing-support/accounts");
    }
}
