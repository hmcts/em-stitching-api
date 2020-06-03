package uk.gov.hmcts.reform.em.stitching.testutil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentImage;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRendering;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDocumentDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleFolderDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.test.dm.DmHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TestUtil {

    private String idamAuth;
    private String s2sAuth;

    @Value("${test.url}")
    private String testUrl;

    @Value("${document_management.url}")
    private String dmApiUrl;

    @Autowired
    private IdamHelper idamHelper;

    @Autowired
    private S2sHelper s2sHelper;

    @Autowired
    private DmHelper dmHelper;

    @PostConstruct
    public void init() {
        idamHelper.createUser("a@b.com", Stream.of("caseworker").collect(Collectors.toList()));
        RestAssured.useRelaxedHTTPSValidation();
        idamAuth = idamHelper.authenticateUser("a@b.com");
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

    public File downloadDocument(String documentUrl) throws IOException {
        String documentId = documentUrl.substring(documentUrl.lastIndexOf('/') + 1);
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir") + "/" + documentId + "-test.pdf");
        Files.copy(dmHelper.getDocumentBinary(documentId), tempPath, StandardCopyOption.REPLACE_EXISTING);
        return tempPath.toFile();
    }

    public String uploadDocument(String pdfName) {
        try {
            return dmHelper.getDocumentMetadata(
                    dmHelper.uploadAndGetId(
                            ClassLoader.getSystemResourceAsStream(pdfName), "application/pdf", pdfName))
                        .links.self.href;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private String uploadDocument() {
        return uploadDocument("hundred-page.pdf");
    }

    public BundleDTO getTestBundle() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Document 1"));
        docs.add(getTestBundleDocument(uploadDocument(), "Document 2"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleforFailure() throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final File bundleJsonFile = new File(ClassLoader.getSystemResource("bundle.json").getPath());
        BundleDTO bundle = mapper.readValue(bundleJsonFile, BundleDTO.class);

        return bundle;
    }

    public BundleDTO getTestBundleWithOnePageDocuments() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument("Document1.pdf"), "Document 1"));
        docs.add(getTestBundleDocument(uploadDocument("Document2.pdf"), "Document 2"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithLargeToc() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument("five-hundred-page.pdf"), "Document 3"));
        docs.add(getTestBundleDocument(uploadDocument("annotationTemplate.pdf"), "Document 4"));
        docs.add(getTestBundleDocument(uploadDocument("SamplePDF_special_characters.pdf"), "Document 5"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleOutlineWithNoDestination() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument("Document-With-Outlines-No-Page-Links.pdf"), "Document 1"));
        docs.add(getTestBundleDocument(uploadDocument("Document-With-Outlines-No-Page-Links.pdf"), "Document 2"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithOneDocumentWithAOutline() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument("one-page.pdf"), "Document 1"));
        docs.add(getTestBundleDocument(uploadDocument("Document1.pdf"), "Document 2"));
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
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Test PDF"));
        docs.add(getTestBundleDocument(uploadWordDocument("wordDocument.doc"), "Test Word Document"));
        docs.add(getTestBundleDocument(uploadDocX("wordDocument2.docx"), "Test DocX"));
        docs.add(getTestBundleDocument(uploadDocX("largeDocument.docx"), "Test Word Document"));
        docs.add(getTestBundleDocument(uploadDocX("wordDocumentInternallyZip.docx"), "Test Word DocX/Zip"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithTextFile() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Text File");
        bundle.setDescription("This bundle contains Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Test PDF"));
        docs.add(getTestBundleDocument(uploadTextFile("sample_text_file.txt"), "Test Text File"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithRichTextFile() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Rich Text File");
        bundle.setDescription("This bundle contains Rich Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Test PDF"));
        docs.add(getTestBundleDocument(uploadRichTextFile("rtf.rtf"), "Rich Text File"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithExcelAndPptDoc() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Excel and PPT Documents");
        bundle.setDescription("This bundle contains PPT and Excel documents that have been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Test PDF"));
        docs.add(getTestBundleDocument(uploadWordDocument("wordDocument.doc"), "Test Word Document"));
        docs.add(getTestBundleDocument(uploadDocX("largeDocument.docx"), "Test Word Document"));
        docs.add(getTestBundleDocument(uploadPptx("Performance_Out.pptx"), "Test PPTX"));
        docs.add(getTestBundleDocument(uploadXlsx("TestExcelConversion.xlsx"), "Test XLSX"));
        docs.add(getTestBundleDocument(uploadXls("XLSsample.xls"), "Test XLS"));
        docs.add(getTestBundleDocument(uploadXltx("Portable_XR_ReportTemplate.xltx"), "Test XLTX"));
        docs.add(getTestBundleDocument(uploadPPT("potential_and_kinetic.ppt"), "Test PPT"));
        docs.add(getTestBundleDocument(uploadPpsx("sample.ppsx"), "Test PPSX"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithImage() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle with Image");
        bundle.setDescription("This bundle contains an Image that has been converted by pdfbox");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument("one-page.pdf"), "Document 1"));
        docs.add(getTestBundleDocument(uploadDocument("Document1.pdf"), "Document 2"));
        docs.add(getTestBundleDocument(uploadImage("flying-pig.jpg"), "Welcome to the flying pig"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithWatermarkImage() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle with Image");
        bundle.setDescription("This bundle contains an Image that has been converted by pdfbox");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument("one-page.pdf"), "Document 1"));
        docs.add(getTestBundleDocument(uploadDocument("Document1.pdf"), "Document 2"));
        docs.add(getTestBundleDocument(uploadImage("flying-pig.jpg"), "Welcome to the flying pig"));
        bundle.setDocuments(docs);

        DocumentImage documentImage = new DocumentImage();
        documentImage.setDocmosisAssetId("hmcts.png");
        documentImage.setImageRendering(ImageRendering.opaque);
        documentImage.setImageRenderingLocation(ImageRenderingLocation.firstPage);
        documentImage.setCoordinateX(50);
        documentImage.setCoordinateY(50);
        bundle.setDocumentImage(documentImage);

        return bundle;
    }

    private String uploadWordDocument(String docName) {
        return s2sAuthRequest()
            .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", "test.doc", ClassLoader.getSystemResourceAsStream(docName), "application/msword")
            .multiPart("classification", "PUBLIC")
            .request("POST", getDmApiUrl() + "/documents")
            .getBody()
            .jsonPath()
            .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadTextFile(String textFileName) {
        return s2sAuthRequest()
            .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", "test.txt", ClassLoader.getSystemResourceAsStream(textFileName), "text/plain")
            .multiPart("classification", "PUBLIC")
            .request("POST", getDmApiUrl() + "/documents")
            .getBody()
            .jsonPath()
            .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadRichTextFile(String richTextFileName) {
        return s2sAuthRequest()
            .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", "rtf.rtf", ClassLoader.getSystemResourceAsStream(richTextFileName), "application/rtf")
            .multiPart("classification", "PUBLIC")
            .request("POST", getDmApiUrl() + "/documents")
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
            .request("POST", getDmApiUrl() + "/documents")
            .getBody()
            .jsonPath()
            .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadXls(String docName) {
        return s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("files", "test.xls", ClassLoader.getSystemResourceAsStream(docName), "application/vnd.ms-excel")
                .multiPart("classification", "PUBLIC")
                .request("POST", getDmApiUrl() + "/documents")
                .getBody()
                .jsonPath()
                .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadXltx(String docName) {
        return s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart(
                        "files",
                        "test.xltx",
                        ClassLoader.getSystemResourceAsStream(docName),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.template"
                )
                .multiPart("classification", "PUBLIC")
                .request("POST", getDmApiUrl() + "/documents")
                .getBody()
                .jsonPath()
                .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadPptx(String docName) {
        return s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart(
                        "files",
                        "test.pptx",
                        ClassLoader.getSystemResourceAsStream(docName),
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                )
                .multiPart("classification", "PUBLIC")
                .request("POST", getDmApiUrl() + "/documents")
                .getBody()
                .jsonPath()
                .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadPpsx(String docName) {
        return s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart(
                        "files",
                        "test.ppsx",
                        ClassLoader.getSystemResourceAsStream(docName),
                        "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
                )
                .multiPart("classification", "PUBLIC")
                .request("POST", getDmApiUrl() + "/documents")
                .getBody()
                .jsonPath()
                .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadPPT(String docName) {
        return s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("files", "test.ppt", ClassLoader.getSystemResourceAsStream(docName), "application/vnd.ms-powerpoint")
                .multiPart("classification", "PUBLIC")
                .request("POST", getDmApiUrl() + "/documents")
                .getBody()
                .jsonPath()
                .get("_embedded.documents[0]._links.self.href");
    }

    private String uploadXlsx(String docName) {
        return s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart(
                        "files",
                        "test.xlsx",
                        ClassLoader.getSystemResourceAsStream(docName),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
                .multiPart("classification", "PUBLIC")
                .request("POST", getDmApiUrl() + "/documents")
                .getBody()
                .jsonPath()
                .get("_embedded.documents[0]._links.self.href");
    }

    public BundleDTO getTestBundleWithDuplicateBundleDocuments() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
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
            .request("POST", getDmApiUrl() + "/documents")
            .getBody()
            .jsonPath()
            .get("_embedded.documents[0]._links.self.href");
    }

    public Response pollUntil(String endpoint, Function<JsonPath, Boolean> evaluator) throws InterruptedException, IOException {
        return pollUntil(endpoint, evaluator, 300);
    }

    private Response pollUntil(String endpoint,
                              Function<JsonPath, Boolean> evaluator,
                              int numRetries) throws InterruptedException, IOException {

        for (int i = 0; i < numRetries; i++) {
            Response response = authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .request("GET", testUrl + endpoint);

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
            .request("POST", testUrl + "/api/document-tasks");

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

    public static PDDocumentOutline getDocumentOutline(File file) throws IOException {
        final PDDocument doc = PDDocument.load(file);
        final PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();

        doc.close();

        return outline;
    }

    public static int getOutlinePage(PDOutlineItem outlineItem) throws IOException {
        PDPageDestination dest = (PDPageDestination) outlineItem.getDestination();
        return dest == null ? -1 : Math.max(dest.retrievePageNumber(), 0) + 1;
    }

    public String getTestUrl() {
        return testUrl;
    }

    public void setTestUrl(String testUrl) {
        this.testUrl = testUrl;
    }

    public String getDmApiUrl() {
        return dmApiUrl;
    }

    public void setDmApiUrl(String dmApiUrl) {
        this.dmApiUrl = dmApiUrl;
    }


    public RequestSpecification emptyIdamAuthRequest() {
        return s2sAuthRequest()
                .header("Authorization", null);
    }

    public RequestSpecification emptyIdamAuthAndEmptyS2SAuth() {
        return RestAssured
                .given()
                .header("ServiceAuthorization", null)
                .header("Authorization", null);
    }

    public RequestSpecification validAuthRequestWithEmptyS2SAuth() {
        return emptyS2sAuthRequest().header("Authorization", idamAuth);
    }

    public RequestSpecification validS2SAuthWithEmptyIdamAuth() {
        return s2sAuthRequest().header("Authorization", null);
    }

    private RequestSpecification emptyS2sAuthRequest() {
        return RestAssured.given().header("ServiceAuthorization", null);
    }

    public RequestSpecification invalidIdamAuthrequest() {
        return s2sAuthRequest().header("Authorization", "invalidIDAMAuthRequest");
    }

    public RequestSpecification invalidS2SAuth() {
        return invalidS2sAuthRequest().header("Authorization", idamAuth);
    }

    private RequestSpecification invalidS2sAuthRequest() {
        return RestAssured.given().header("ServiceAuthorization", "invalidS2SAuthorization");
    }
}
