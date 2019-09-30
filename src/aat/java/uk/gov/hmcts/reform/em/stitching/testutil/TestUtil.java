package uk.gov.hmcts.reform.em.stitching.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.*;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDocumentDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleFolderDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TestUtil {

    private final String idamAuth;
    private final String s2sAuth;

    public TestUtil() {
        IdamHelper idamHelper = new IdamHelper(
            Env.getIdamUrl(),
            Env.getOAuthClient(),
            Env.getOAuthSecret(),
            Env.getOAuthRedirect()
        );

        S2sHelper s2sHelper = new S2sHelper(
            Env.getS2sUrl(),
            Env.getS2sSecret(),
            Env.getS2sMicroservice()
        );

        RestAssured.useRelaxedHTTPSValidation();

        idamAuth = idamHelper.getIdamToken();
        s2sAuth = s2sHelper.getS2sToken();
    }

    public RequestSpecification authRequest() {
        return s2sAuthRequest()
            .header("Authorization", idamAuth);
    }

    private RequestSpecification s2sAuthRequest() {
        return RestAssured
                .given()
                .header("ServiceAuthorization", s2sAuth);
    }

    public File downloadDocument(String documentURI) throws IOException {
        byte[] byteArray = s2sAuthRequest()
                .header("user-roles", "caseworker")
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .request("GET", uriWithBinarySuffix(documentURI))
                .getBody()
                .asByteArray();

        File tempFile = File.createTempFile("stitched-indexed-document", ".pdf");

        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            outputStream.write(byteArray);
        } catch (Exception e) {
            System.out.println("Error message: " + e);
        }
        return tempFile;
    }

    public static String uriWithBinarySuffix(String s) {
        return s.endsWith("/binary") ? s : s + "/binary";
    }

    private String uploadDocument(String pdfName) {
        return s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("files", "test.pdf", ClassLoader.getSystemResourceAsStream(pdfName), "application/pdf")
                .multiPart("classification", "PUBLIC")
                .request("POST", Env.getDmApiUrl() + "/documents")
                .getBody()
                .jsonPath()
                .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadDocument() {
        return uploadDocument("hundred-page.pdf");
    }

    public BundleDTO getTestBundle() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        bundle.setPageNumberFormat(PageNumberFormat.numberOfPages.toString());
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Document 1"));
        docs.add(getTestBundleDocument(uploadDocument(), "Document 2"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithOnePageDocuments() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        bundle.setPageNumberFormat(PageNumberFormat.numberOfPages.toString());
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument("Document1.pdf"), "Document 1"));
        docs.add(getTestBundleDocument(uploadDocument("Document2.pdf"), "Document 2"));
        bundle.setDocuments(docs);

        return bundle;
    }

    private BundleDocumentDTO getTestBundleDocument(String documentUrl, String title) {
        BundleDocumentDTO document = new BundleDocumentDTO();

        document.setDocumentURI(documentUrl);
        document.setDocTitle(String.format("Title (%s)", title));
        document.setDocDescription(String.format("Description (%s)", title));

        return document;
    }

    public BundleDTO getTestBundleWithWordDoc() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Word Documents");
        bundle.setDescription("This bundle contains Word documents that have been converted by Docmosis.");
        bundle.setPageNumberFormat(PageNumberFormat.numberOfPages.toString());
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Test PDF"));
        docs.add(getTestBundleDocument(uploadWordDocument("wordDocument.doc"), "Test Word Document"));
        docs.add(getTestBundleDocument(uploadDocX("wordDocument2.docx"), "Test DocX"));
        docs.add(getTestBundleDocument(uploadDocX("largeDocument.docx"), "Test Word Document"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithImage() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle with Image");
        bundle.setDescription("This bundle contains an Image that has been converted by pdfbox");
        bundle.setPageNumberFormat(PageNumberFormat.numberOfPages.toString());
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Test PDF"));
        docs.add(getTestBundleDocument(uploadImage("flying-pig.jpg"), "Welcome to the flying pig"));
        bundle.setDocuments(docs);

        return bundle;
    }

    private String uploadWordDocument(String docName) {
        return s2sAuthRequest()
            .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", "test.doc", ClassLoader.getSystemResourceAsStream(docName), "application/msword")
            .multiPart("classification", "PUBLIC")
            .request("POST", Env.getDmApiUrl() + "/documents")
            .getBody()
            .jsonPath()
            .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadDocX(String docName) {
        return s2sAuthRequest()
            .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart(
                "files",
                "test.docx",
                ClassLoader.getSystemResourceAsStream(docName),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
            .multiPart("classification", "PUBLIC")
            .request("POST", Env.getDmApiUrl() + "/documents")
            .getBody()
            .jsonPath()
            .get("_embedded.documents[0]._links.self.href");
    }

    public BundleDTO getTestBundleWithDuplicateBundleDocuments() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        bundle.setPageNumberFormat(PageNumberFormat.numberOfPages.toString());
        List<BundleDocumentDTO> docs = new ArrayList<>();
        BundleDocumentDTO uploadedDocument = getTestBundleDocument(uploadDocument(), "Document 1");
        docs.add(uploadedDocument);
        docs.add(uploadedDocument);
        bundle.setDocuments(docs);
        return bundle;
    }

    public BundleDTO getTestBundleWithSortedDocuments() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        bundle.setPageNumberFormat(PageNumberFormat.numberOfPages.toString());
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocumentWithSortIndices(uploadDocument("Document1.pdf"), "Document1.pdf", 2));
        docs.add(getTestBundleDocumentWithSortIndices(uploadDocument("Document2.pdf"), "Document2.pdf", 1));
        bundle.setDocuments(docs);
        return bundle;
    }

    private BundleDocumentDTO getTestBundleDocumentWithSortIndices(String documentUrl, String title, int sortIndex) {
        BundleDocumentDTO document = new BundleDocumentDTO();

        document.setDocumentURI(documentUrl);
        document.setDocTitle(String.format("Title (%s)", title));
        document.setDocDescription(String.format("Description (%s)", title));
        document.setSortIndex(sortIndex);

        return document;
    }

    private String uploadImage(String docName) {
        return s2sAuthRequest()
            .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", "test.jpg", ClassLoader.getSystemResourceAsStream(docName), "image/jpeg")
            .multiPart("classification", "PUBLIC")
            .request("POST", Env.getDmApiUrl() + "/documents")
            .getBody()
            .jsonPath()
            .get("_embedded.documents[0]._links.self.href");
    }

    public Response pollUntil(String endpoint, Function<JsonPath, Boolean> evaluator) throws InterruptedException, IOException {
        return pollUntil(endpoint, evaluator, 60);
    }

    private Response pollUntil(String endpoint,
                              Function<JsonPath, Boolean> evaluator,
                              int numRetries) throws InterruptedException, IOException {

        for (int i = 0; i < numRetries; i++) {
            Response response = authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .request("GET", Env.getTestUrl() + endpoint);

            if (response.getStatusCode() == 500) {
                throw new IOException("HTTP 500 from service");
            }
            if (evaluator.apply(response.body().jsonPath())) {
                return response;
            }

            Thread.sleep(1000);
        }

        throw new IOException("Task not in the correct state after max number of retries.");
    }

    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);

        return mapper.writeValueAsBytes(object);
    }

    public Response processBundle(BundleDTO bundle) throws IOException, InterruptedException {
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        String json = new String(TestUtil.convertObjectToJsonBytes(documentTask));
        System.out.println(json);

        Response createTaskResponse = authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
            .request("POST", Env.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");

        return pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));
    }

    /**
     * Creates a bundle with structure:.
     * <p>
     * - Folder 1
     *   - Document 1
     * - Folder 2
     *   - Document 2
     * </p>
     */
    public BundleDTO getTestBundleWithFlatFolders() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle with folders");
        bundle.setDescription("This is the description of the bundle: it is super-great.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        bundle.setHasFolderCoversheets(false);
        bundle.setPageNumberFormat(PageNumberFormat.numberOfPages.toString());

        BundleFolderDTO folder = new BundleFolderDTO();
        folder.setFolderName("Folder 1");
        folder.getDocuments().add(getTestBundleDocumentWithSortIndices(uploadDocument("Document1.pdf"), "Document1.pdf", 1));
        folder.setSortIndex(1);
        bundle.getFolders().add(folder);

        BundleFolderDTO folder2 = new BundleFolderDTO();
        folder2.setFolderName("Folder 2");
        folder2.getDocuments().add(getTestBundleDocumentWithSortIndices(uploadDocument("Document2.pdf"), "Document2.pdf", 1));
        folder2.setSortIndex(2);
        bundle.getFolders().add(folder2);

        return bundle;
    }

    /**
     * Creates a bundle with structure:.
     * <p>
     * - Folder 1
     *   - Document 1
     *   - Folder 1a
     *     - Document 1a
     *   - Folder 1b
     *     - Document 1b
     * - Folder 2
     *   - Document 2
     * </p>
     */
    public BundleDTO getTestBundleWithNestedFolders() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle with folders");
        bundle.setDescription("This is the description of the bundle: it is super-great.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        bundle.setHasFolderCoversheets(false);
        bundle.setPageNumberFormat(PageNumberFormat.numberOfPages.toString());

        BundleFolderDTO folder = new BundleFolderDTO();
        folder.setFolderName("Folder 1");
        folder.getDocuments().add(getTestBundleDocumentWithSortIndices(uploadDocument("Document1.pdf"), "Document1.pdf", 1));
        folder.setSortIndex(1);
        bundle.getFolders().add(folder);

        BundleFolderDTO folder1a = new BundleFolderDTO();
        folder1a.setFolderName("Folder 1a");
        folder1a.getDocuments().add(getTestBundleDocumentWithSortIndices(uploadDocument("Document1.pdf"), "Document1a.pdf", 1));
        folder1a.setSortIndex(2);
        folder.getFolders().add(folder1a);

        BundleFolderDTO folder1b = new BundleFolderDTO();
        folder1b.setFolderName("Folder 1b");
        folder1b.getDocuments().add(getTestBundleDocumentWithSortIndices(uploadDocument("Document1.pdf"), "Document1b.pdf", 1));
        folder1b.setSortIndex(3);
        folder.getFolders().add(folder1b);

        BundleFolderDTO folder2 = new BundleFolderDTO();
        folder2.setFolderName("Folder 2");
        folder2.getDocuments().add(getTestBundleDocumentWithSortIndices(uploadDocument("Document2.pdf"), "Document2.pdf", 1));
        folder2.setSortIndex(2);
        bundle.getFolders().add(folder2);

        return bundle;
    }

    public static int getNumPages(File file) throws IOException {
        final PDDocument doc = PDDocument.load(file);
        final int numPages = doc.getNumberOfPages();

        doc.close();

        return numPages;
    }
}
