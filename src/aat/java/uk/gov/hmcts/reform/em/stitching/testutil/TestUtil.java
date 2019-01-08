package uk.gov.hmcts.reform.em.stitching.testutil;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.http.MediaType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class TestUtil {

    private String s2sToken;
    private String idamToken;
    private String documentId;
    private String annotationSetId;

    public String getAnnotationSetId() {
        return this.annotationSetId;
    }

    public String getDocumentId() {
        return this.documentId;
    }

    public File getDocumentBinary(String documentId) throws Exception {
        Response response = s2sAuthRequest()
                .header("user-roles", "caseworker")
                .request("GET", Env.getDmApiUrl() + "/documents/" + documentId + "/binary");

        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir") + "/" + documentId + "-test.pdf");

        Files.copy(response.getBody().asInputStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);

        return tempPath.toFile();
    }

    public String saveAnnotation(String annotationSetId, Integer pageNum) throws Exception {
        UUID annotationId = UUID.randomUUID();
        JSONObject createAnnotations = new JSONObject();
        createAnnotations.put("annotationSetId", annotationSetId);
        createAnnotations.put("id", annotationId);
        createAnnotations.put("annotationType", "highlight");
        createAnnotations.put("page", pageNum);
        createAnnotations.put("color", "d1d1d1");

        JSONArray comments = new JSONArray();
        JSONObject comment = new JSONObject();
        comment.put("content", "text");
        comment.put("annotationId", annotationId);
        comment.put("id", UUID.randomUUID().toString());
        comments.put(0, comment);
        createAnnotations.put("comments", comments);

        JSONArray rectangles = new JSONArray();
        JSONObject rectangle = new JSONObject();
        rectangle.put("id", UUID.randomUUID().toString());
        rectangle.put("annotationId", annotationId);
        rectangle.put("x", 0f);
        rectangle.put("y", 0f);
        rectangle.put("width", 10f);
        rectangle.put("height", 10f);
        rectangles.put(0, rectangle);
        createAnnotations.put("rectangles", rectangles);

        Response response = authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(createAnnotations)
                .request("POST", Env.getAnnotationApiUrl() + "/api/annotations");

        Assert.assertEquals(201, response.getStatusCode());

        return annotationId.toString();
    }

    public String saveAnnotation(String annotationSetId) throws Exception {
        return saveAnnotation(annotationSetId, 1);
    }

    public String createAnnotationSetForDocumentId(String documentId) throws Exception {
        UUID annotationSetId = UUID.randomUUID();
        JSONObject createAnnotationSet = new JSONObject();
        createAnnotationSet.put("documentId", documentId);
        createAnnotationSet.put("id", annotationSetId);

        Response response = authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(createAnnotationSet)
                .request("POST", Env.getAnnotationApiUrl() + "/api/annotation-sets");

        Assert.assertEquals(201, response.getStatusCode());

        this.annotationSetId = annotationSetId.toString();
        return this.annotationSetId;
    }

    public String uploadDocument(String pdfName) throws Exception {
        String newDocUrl = s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("files", "test.pdf",  ClassLoader.getSystemResourceAsStream(pdfName), "application/pdf")
                .multiPart("classification", "PUBLIC")
                .request("POST", Env.getDmApiUrl() + "/documents")
                .getBody()
                .jsonPath()
                .get("_embedded.documents[0]._links.self.href");

        this.documentId = newDocUrl.substring(newDocUrl.lastIndexOf("/") + 1);
        return this.documentId;
    }

    public String uploadDocument() throws Exception {
        return uploadDocument("annotationTemplate.pdf");
    }

    public RequestSpecification authRequest() throws Exception {
        return s2sAuthRequest()
            .header("Authorization", "Bearer " + getIdamToken("test@test.com"));
    }

    public RequestSpecification s2sAuthRequest() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        return RestAssured
                .given()
                .header("ServiceAuthorization", "Bearer " + getS2sToken());
    }

    public String getIdamToken() {
        return getIdamToken("test@test.com");
    }

    public String getIdamToken(String username) {
        if (idamToken == null) {
            createUser(username, "password");
            String userId = findUserIdByUserEmail(username).toString();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", userId);
            jsonObject.put("role", "caseworker");

            Response response = RestAssured
                    .given()
                    .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .formParam("id", userId)
                    .formParam("role", "caseworker")
                    .post(Env.getIdamURL() + "/testing-support/lease");

            idamToken = response.getBody().print();
        }
        return idamToken;
    }

    private Integer findUserIdByUserEmail(String email) {
        return RestAssured
                .get(Env.getIdamURL() + "/users?email=" + email)
                .getBody()
                .jsonPath()
                .get("id");
    }

    public void createUser(String email, String password) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", email);
        jsonObject.put("password", password);
        jsonObject.put("forename", "test");
        jsonObject.put("surname", "test");

        RestAssured
            .given()
            .header("Content-Type", "application/json")
            .body(jsonObject.toString())
            .post(Env.getIdamURL() + "/testing-support/accounts");

    }


    public String getS2sToken() throws Exception {

        if (s2sToken == null) {
            String otp = String.valueOf(new GoogleAuthenticator().getTotpPassword(Env.getS2SToken()));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("microservice", Env.getS2SServiceName());
            jsonObject.put("oneTimePassword", otp);

            Response response = RestAssured
                    .given()
                    .header("Content-Type", "application/json")
                    .body(jsonObject.toString())
                    .post(Env.getS2SURL() + "/lease");
            s2sToken = response.getBody().asString();
            s2sToken = response.getBody().print();
        }

        return s2sToken;
    }

}
