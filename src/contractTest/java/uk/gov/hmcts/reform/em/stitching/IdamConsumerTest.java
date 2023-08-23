package uk.gov.hmcts.reform.em.stitching;


import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.Maps;
import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
public class IdamConsumerTest {

    private static final String IDAM_DETAILS_URL = "/details";
    private static final String IDAM_OPENID_TOKEN_URL = "/o/token";
    private static final String ACCESS_TOKEN = "111";

    @Pact(provider = "Idam_api", consumer = "em_stitching_api")
    public RequestResponsePact executeGetUserDetailsAndGet200(PactDslWithProvider builder) {

        Map<String, String> headers = Maps.newHashMap();
        headers.put(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN);

        return builder
                .given("Idam successfully returns user details")
                .uponReceiving("Provider receives a GET /details request from the Stitching API")
                .path(IDAM_DETAILS_URL)
                .method(HttpMethod.GET.toString())
                .headers(headers)
                .willRespondWith()
                .status(HttpStatus.OK.value())
                .body(createUserDetailsResponse())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetUserDetailsAndGet200")
    public void should_get_user_details_with_access_token(MockServer mockServer) throws JSONException {

        Map<String, String> headers = Maps.newHashMap();
        headers.put(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN);

        String actualResponseBody =
                SerenityRest
                        .given()
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .when()
                        .get(mockServer.getUrl() + IDAM_DETAILS_URL)
                        .then()
                        .statusCode(200)
                        .and()
                        .extract()
                        .body()
                        .asString();

        JSONObject response = new JSONObject(actualResponseBody);

        assertThat(actualResponseBody).isNotNull();
        assertThat(response).hasNoNullFieldsOrProperties();
        assertThat(response.getString("id")).isNotBlank();
        assertThat(response.getString("forename")).isNotBlank();
        assertThat(response.getString("surname")).isNotBlank();

        JSONArray rolesArr = new JSONArray(response.getString("roles"));

        assertThat(rolesArr).isNotNull();
        assertThat(rolesArr.length()).isNotZero();
        assertThat(rolesArr.get(0).toString()).isNotBlank();

    }

    @Pact(provider = "Idam_api", consumer = "em_stitching_api")
    public RequestResponsePact executeGetIdamAccessTokenAndGet200(PactDslWithProvider builder) {

        String[] rolesArray = new String[1];
        rolesArray[0] = "citizen";
        Map<String, String> requestheaders = Maps.newHashMap();
        requestheaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> responseheaders = Maps.newHashMap();
        responseheaders.put("Content-Type", "application/json");

        Map<String, Object> params = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        params.put("email", "emCaseOfficer@email.net");
        params.put("password", "Password123");
        params.put("forename", "emCaseOfficer");
        params.put("surname", "jar123");
        params.put("roles", rolesArray);

        return builder
                .given("a user exists", params)
                .uponReceiving("Provider takes user/pwd and returns Access Token to Stitching API")
                .path(IDAM_OPENID_TOKEN_URL)
                .method(HttpMethod.POST.toString())
                .body("redirect_uri=http%3A%2F%2Fwww.dummy-pact-service.com%2Fcallback"
                        + "&client_id=pact&grant_type=password&username=emCaseOfficer%40email.net&password=Password123"
                        + "&client_secret=pactsecret&scope=openid profile roles", "application/x-www-form-urlencoded")
                .willRespondWith()
                .status(HttpStatus.OK.value())
                .headers(responseheaders)
                .body(createAuthResponse())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetIdamAccessTokenAndGet200")
    public void should_post_to_token_endpoint_and_receive_access_token_with_200_response(
            MockServer mockServer
    ) throws JSONException {

        String actualResponseBody =
                SerenityRest
                        .given()
                        .contentType(ContentType.URLENC)
                        .formParam("redirect_uri", "http://www.dummy-pact-service.com/callback")
                        .formParam("client_id", "pact")
                        .formParam("grant_type", "password")
                        .formParam("username", "emCaseOfficer@email.net")
                        .formParam("password", "Password123")
                        .formParam("client_secret", "pactsecret")
                        .formParam("scope", "openid profile roles")
                        .post(mockServer.getUrl() + IDAM_OPENID_TOKEN_URL)
                        .then()
                        .log().all().extract().asString();

        JSONObject response = new JSONObject(actualResponseBody);

        assertThat(response).isNotNull();
        assertThat(response.getString("access_token")).isNotBlank();
    }

    private PactDslJsonBody createAuthResponse() {

        return new PactDslJsonBody()
                .stringType("access_token", "some-long-value")
                .stringType("refresh_token", "another-long-value")
                .stringType("scope", "openid roles profile")
                .stringType("id_token", "some-value")
                .stringType("token_type", "Bearer")
                .stringType("expires_in", "12345");

    }

    private PactDslJsonBody createUserDetailsResponse() {
        PactDslJsonArray array = new PactDslJsonArray().stringValue("caseofficer-em");

        return new PactDslJsonBody()
                .stringType("id", "123")
                .stringType("email", "em-caseofficer@fake.hmcts.net")
                .stringType("forename", "Case")
                .stringType("surname", "Officer")
                .stringType("roles", array.toString());

    }

    private static String createRequestBody() {

        return "{\"grant_type\": \"password\","
                + " \"client_id\": \"em\","
                + " \"client_secret\": \"some_client_secret\","
                + " \"redirect_uri\": \"/oauth2redirect\","
                + " \"scope\": \"openid roles profile\","
                + " \"username\": \"stitchingusername\","
                + " \"password\": \"stitchingpwd\"\n"
                + " }";
    }
}
