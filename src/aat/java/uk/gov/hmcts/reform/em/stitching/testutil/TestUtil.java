package uk.gov.hmcts.reform.em.stitching.testutil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.PostConstruct;
import net.serenitybdd.rest.SerenityRest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentImage;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRendering;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDocumentDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleFolderDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.dto.CcdBundleDocumentDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.dto.CcdDocument;
import uk.gov.hmcts.reform.em.stitching.testutil.dto.CcdValue;
import uk.gov.hmcts.reform.em.test.ccddata.CcdDataHelper;
import uk.gov.hmcts.reform.em.test.cdam.CdamHelper;
import uk.gov.hmcts.reform.em.test.dm.DmHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;

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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static pl.touk.throwing.ThrowingFunction.unchecked;

@Service
@ComponentScan({ "uk.gov.hmcts.reform" })
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
    @Qualifier("xuiS2sHelper")
    private S2sHelper cdamS2sHelper;

    @Autowired
    private DmHelper dmHelper;

    @Autowired
    private CcdDataHelper ccdDataHelper;

    @Autowired
    private CdamHelper cdamHelper;

    public final String createCaseTemplate = "{\n"
        + "    \"caseTitle\": null,\n"
        + "    \"caseOwner\": null,\n"
        + "    \"caseCreationDate\": null,\n"
        + "    \"caseDescription\": null,\n"
        + "    \"caseComments\": null,\n"
        + "    \"caseDocuments\": %s\n"
        + "  }";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String stitchingTestUser = "stitchingTestUser@stitchingTest.com";
    private List<String> stitchingTestUserRoles = Stream.of("caseworker", "caseworker-publiclaw", "ccd-import").collect(Collectors.toList());

    @PostConstruct
    public void init() {
        idamHelper.createUser("stitchingTestUser@stitchingTest.com",
            Stream.of("caseworker", "caseworker-publiclaw", "ccd-import").collect(Collectors.toList()));
        SerenityRest.useRelaxedHTTPSValidation();
        idamAuth = idamHelper.authenticateUser("stitchingTestUser@stitchingTest.com");
        s2sAuth = s2sHelper.getS2sToken();
    }

    public RequestSpecification authRequest() {
        return s2sAuthRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE)
                .header("Authorization", idamAuth);
    }

    public RequestSpecification unauthenticatedRequest() {
        return SerenityRest.given();
    }

    private RequestSpecification s2sAuthRequest() {
        return SerenityRest
                .given()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE)
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
        docs.add(getTestBundleDocument(uploadFile("wordDocument.doc", "application/msword"), "Test Word Document"));
        docs.add(getTestBundleDocument(uploadFile("wordDocument2.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                "Test DocX"));
        docs.add(getTestBundleDocument(uploadFile("largeDocument.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                "Test Word Document"));
        docs.add(getTestBundleDocument(uploadFile("wordDocumentInternallyZip.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                "Test Word DocX/Zip"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithTextFile() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Text File");
        bundle.setDescription("This bundle contains Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Test PDF"));
        docs.add(getTestBundleDocument(uploadFile("sample_text_file.txt", "text/plain"), "Test Text File"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithRichTextFile() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Rich Text File");
        bundle.setDescription("This bundle contains Rich Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Test PDF"));
        docs.add(getTestBundleDocument(uploadFile("rtf.rtf", "application/rtf"), "Rich Text File"));
        docs.add(getTestBundleDocument(uploadFile("potential_and_kinetic.ppt", "application/vnd.ms-powerpoint"), "Test PPT"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithExcelAndPptDoc() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Excel and PPT Documents");
        bundle.setDescription("This bundle contains PPT and Excel documents that have been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), "Test PDF"));
        docs.add(getTestBundleDocument(uploadFile("wordDocument.doc", "application/msword"), "Test Word Document"));
        docs.add(getTestBundleDocument(uploadFile("largeDocument.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                "Test Word Document"));
        docs.add(getTestBundleDocument(uploadFile("Performance_Out.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
                "Test PPTX"));
        docs.add(getTestBundleDocument(uploadFile("TestExcelConversion.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                "Test XLSX"));
        docs.add(getTestBundleDocument(uploadFile("XLSsample.xls", "application/vnd.ms-excel"), "Test XLS"));
        docs.add(getTestBundleDocument(uploadFile("Portable_XR_ReportTemplate.xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template"),
                "Test XLTX"));
//        docs.add(getTestBundleDocument(uploadFile("potential_and_kinetic.ppt", "application/vnd.ms-powerpoint"), "Test PPT"));
        docs.add(getTestBundleDocument(uploadFile("sample.ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow"),
                "Test PPSX"));
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
        docs.add(getTestBundleDocument(uploadFile("flying-pig.jpg", "image/jpeg"), "Welcome to the flying pig"));
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
        docs.add(getTestBundleDocument(uploadFile("flying-pig.jpg", "image/jpeg"), "Welcome to the flying pig"));
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

    private String uploadFile(String fileName, String mimeType) {
        return s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("files", fileName, ClassLoader.getSystemResourceAsStream(fileName), mimeType)
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

    public Response pollUntil(String endpoint, Function<JsonPath, Boolean> evaluator) throws InterruptedException, IOException {
        return pollUntil(endpoint, evaluator, 300);
    }

    private Response pollUntil(String endpoint,
                               Function<JsonPath, Boolean> evaluator,
                               int numRetries) throws InterruptedException, IOException {

        for (int i = 0; i < numRetries; i++) {
            Response response = authRequest().get(endpoint);

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

        Response createTaskResponse =
                authRequest()
                        .body(convertObjectToJsonBytes(documentTask))
                        .post("/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");

        return pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));
    }

    /**
     * Creates a bundle with structure:.
     * <p>
     * Bundle with folders
     *  - Folder 1
     *      - Document 1
     *  - Folder 2
     *      - Document 2
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
     * - Document 1
     * - Folder 2
     * - Document 2
     * </p>
     */
    public BundleDTO getTestBundleWithFlatFoldersAndLongDocumentTitle() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle with folders");
        bundle.setDescription("This is the description of the bundle: it is super-great. It is long enough to wrap and show in more than one line");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        bundle.setHasFolderCoversheets(false);

        BundleFolderDTO folder = new BundleFolderDTO();
        folder.setFolderName("Folder 1");
        String text = Stream.generate(() -> "DocName ").limit(20).collect(Collectors.joining());
        text += ".pdf";
        folder.getDocuments().add(getTestBundleDocumentWithSortIndices(uploadDocument("Document1.pdf"), text, 1));
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
     *  Bundle with folders
     *  - Folder 1
     *      - Document 1
     *      - Folder 1a
     *          - Document 1a
     *      - Folder 1b
     *          - Document 1b
     *  - Folder 2
     *      - Document 2
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
        return SerenityRest
                .given()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE)
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
        return SerenityRest
                .given()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE)
                .header("ServiceAuthorization", null);
    }

    public RequestSpecification invalidIdamAuthrequest() {
        return s2sAuthRequest().header("Authorization", "invalidIDAMAuthRequest");
    }

    public RequestSpecification invalidS2SAuth() {
        return invalidS2sAuthRequest().header("Authorization", idamAuth);
    }

    private RequestSpecification invalidS2sAuthRequest() {
        return SerenityRest
                .given()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE)
                .header("ServiceAuthorization", "invalidS2SAuthorization");
    }

    ////// CDAM //////////

    public String getServiceAuth() {
        return cdamS2sHelper.getS2sToken();
    }

    public CaseDetails createCase(String documents) throws Exception {
        return ccdDataHelper.createCase(stitchingTestUser, "PUBLICLAW", getEnvCcdCaseTypeId(), "createCase",
            objectMapper.readTree(String.format(createCaseTemplate, documents)));
    }

    public String getEnvCcdCaseTypeId() {
        return "CCD_BUNDLE_MVP_TYPE_ASYNC";
    }

    public List<String> uploadCdamDocuments(List<Pair<String, String>> fileDetails) throws Exception {

        List<MultipartFile> multipartFiles = fileDetails.stream()
            .map(unchecked(pair -> createMultipartFile(pair.getFirst(), pair.getSecond())))
            .collect(Collectors.toList());

        DocumentUploadRequest uploadRequest = new DocumentUploadRequest(Classification.PUBLIC.toString(), getEnvCcdCaseTypeId(),
            "PUBLICLAW", multipartFiles);

        UploadResponse uploadResponse =  cdamHelper.uploadDocuments("stitchingTestUser@stitchingTest.com",
            uploadRequest);

        return createCaseAndUploadDocuments(uploadResponse);
    }

    private MultipartFile createMultipartFile(String fileName, String contentType) throws IOException {
        return new MockMultipartFile(fileName, fileName, contentType,
            ClassLoader.getSystemResourceAsStream(fileName));
    }

    /*
    Uploads Documents through CDAM and attachs the response DocUrl & Hash against the case. And creates/submits the
    case.
     */
    public List<String> createCaseAndUploadDocuments(UploadResponse uploadResponse) throws Exception {
        List<CcdValue<CcdBundleDocumentDTO>> bundleDocuments = uploadResponse.getDocuments().stream()
            .map(this::createBundleDocument)
            .collect(Collectors.toList());
        String documentsString = objectMapper.writeValueAsString(bundleDocuments);
        createCase(documentsString);


        return uploadResponse.getDocuments().stream()
            .map(document -> document.links.self.href)
            .collect(Collectors.toList());
    }

    public CcdValue<CcdBundleDocumentDTO> createBundleDocument(Document document) {
        CcdDocument ccdDocument = CcdDocument.builder()
            .url(document.links.self.href)
            .binaryUrl(document.links.binary.href)
            .hash(document.hashToken)
            .fileName(document.originalDocumentName)
            .build();
        CcdBundleDocumentDTO ccdBundleDocumentDTO = CcdBundleDocumentDTO.builder()
            .documentLink(ccdDocument)
            .documentName(document.originalDocumentName)
            .build();
        return new CcdValue<>(ccdBundleDocumentDTO);
    }

    public BundleDTO getCdamTestBundle() throws Exception {

        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of("hundred-page.pdf", "application/pdf"));
        fileDetails.add(Pair.of("hundred-page.pdf", "application/pdf"));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        List<BundleDocumentDTO> docs = new ArrayList<>();

        docs.add(getTestBundleDocument(docUrls.get(0), "Document 1"));
        docs.add(getTestBundleDocument(docUrls.get(1), "Document 2"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithWordDoc() throws Exception {

        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of("hundred-page.pdf", "application/pdf"));
        fileDetails.add(Pair.of("wordDocument.doc", "application/msword"));
        fileDetails.add(Pair.of("wordDocument2.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        fileDetails.add(Pair.of("largeDocument.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        fileDetails.add(Pair.of("wordDocumentInternallyZip.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Word Documents");
        bundle.setDescription("This bundle contains Word documents that have been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), "Test PDF"));
        docs.add(getTestBundleDocument(docUrls.get(1), "Test Word Document"));
        docs.add(getTestBundleDocument(docUrls.get(2),  "Test DocX"));
        docs.add(getTestBundleDocument(docUrls.get(3),"Test Word Document"));
        docs.add(getTestBundleDocument(docUrls.get(4),"Test Word DocX/Zip"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithTextFile() throws Exception {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of("hundred-page.pdf", "application/pdf"));
        fileDetails.add(Pair.of("sample_text_file.txt", "text/plain"));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Text File");
        bundle.setDescription("This bundle contains Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), "Test PDF"));
        docs.add(getTestBundleDocument(docUrls.get(1), "Test Text File"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithRichTextFile() throws Exception {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of("hundred-page.pdf", "application/pdf"));
        fileDetails.add(Pair.of("rtf.rtf", "application/rtf"));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Rich Text File");
        bundle.setDescription("This bundle contains Rich Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), "Test PDF"));
        docs.add(getTestBundleDocument(docUrls.get(1), "Rich Text File"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithExcelAndPptDoc() throws Exception {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of("hundred-page.pdf", "application/pdf"));
        fileDetails.add(Pair.of("wordDocument.doc", "application/msword"));
        fileDetails.add(Pair.of("largeDocument.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        fileDetails.add(Pair.of("Performance_Out.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        fileDetails.add(Pair.of("TestExcelConversion.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        fileDetails.add(Pair.of("XLSsample.xls", "application/vnd.ms-excel"));
        fileDetails.add(Pair.of("Portable_XR_ReportTemplate.xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template"));
        fileDetails.add(Pair.of("potential_and_kinetic.ppt", "application/vnd.ms-powerpoint"));
        fileDetails.add(Pair.of("sample.ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow"));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Excel and PPT Documents");
        bundle.setDescription("This bundle contains PPT and Excel documents that have been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), "Test PDF"));
        docs.add(getTestBundleDocument(docUrls.get(1), "Test Word Document"));
        docs.add(getTestBundleDocument(docUrls.get(2),"Test Word Document"));
        docs.add(getTestBundleDocument(docUrls.get(3),"Test PPTX"));
        docs.add(getTestBundleDocument(docUrls.get(4),"Test XLSX"));
        docs.add(getTestBundleDocument(docUrls.get(5), "Test XLS"));
        docs.add(getTestBundleDocument(docUrls.get(6),"Test XLTX"));
        docs.add(getTestBundleDocument(docUrls.get(7), "Test PPT"));
        docs.add(getTestBundleDocument(docUrls.get(8),"Test PPSX"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithImage() throws Exception {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of("one-page.pdf", "application/pdf"));
        fileDetails.add(Pair.of("Document1.pdf", "application/pdf"));
        fileDetails.add(Pair.of("flying-pig.jpg", "image/jpeg"));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle with Image");
        bundle.setDescription("This bundle contains an Image that has been converted by pdfbox");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), "Document 1"));
        docs.add(getTestBundleDocument(docUrls.get(1), "Document 2"));
        docs.add(getTestBundleDocument(docUrls.get(2), "Welcome to the flying pig"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithWatermarkImage() throws Exception {

        DocumentImage documentImage = new DocumentImage();
        documentImage.setDocmosisAssetId("hmcts.png");
        documentImage.setImageRendering(ImageRendering.opaque);
        documentImage.setImageRenderingLocation(ImageRenderingLocation.firstPage);
        documentImage.setCoordinateX(50);
        documentImage.setCoordinateY(50);

        BundleDTO bundle = getCdamTestBundleWithImage();
        bundle.setDocumentImage(documentImage);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithDuplicateBundleDocuments() throws Exception {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of("hundred-page.pdf", "application/pdf"));

        List<String> docUrls = uploadCdamDocuments(fileDetails);
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        BundleDocumentDTO uploadedDocument = getTestBundleDocument(docUrls.get(0), "Document 1");
        docs.add(uploadedDocument);
        docs.add(uploadedDocument);
        bundle.setDocuments(docs);
        return bundle;
    }

    public BundleDTO getCdamTestBundleWithSortedDocuments() throws Exception {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of("Document1.pdf", "application/pdf"));
        fileDetails.add(Pair.of("Document2.pdf", "application/pdf"));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle Title");
        bundle.setDescription("This is the description of the bundle: it is great.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocumentWithSortIndices(docUrls.get(0), "Document1.pdf", 2));
        docs.add(getTestBundleDocumentWithSortIndices(docUrls.get(1), "Document2.pdf", 1));
        bundle.setDocuments(docs);
        return bundle;
    }
}
