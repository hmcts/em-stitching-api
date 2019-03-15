package uk.gov.hmcts.reform.em.stitching.testutil;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class S2sHelper {

    @Value("${idam.s2s-auth.url}")
    private String s2sUrl;

    @Value("${idam.s2s-auth.totp-secret}")
    private String totpSecret;

    @Value("${idam.s2s-auth.microservice}")
    private String microservice;

    public String getS2sToken() {
        String otp = String.valueOf(new GoogleAuthenticator().getTotpPassword(totpSecret));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("microservice", microservice);
        jsonObject.put("oneTimePassword", otp);

        return "Bearer " + RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(jsonObject
                .toString())
                .post(s2sUrl + "/lease")
                .getBody()
                .asString();
    }
}
