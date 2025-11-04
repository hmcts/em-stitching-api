package uk.gov.hmcts.reform.em.stitching.testutil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.PostConstruct;
import net.serenitybdd.rest.SerenityRest;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static pl.touk.throwing.ThrowingFunction.unchecked;

// CHECKSTYLE:OFF: AvoidStarImport - Test Constants class specific for this TestUtil class.
import static uk.gov.hmcts.reform.em.stitching.testutil.TestDataConstants.*;
// CHECKSTYLE:ON: AvoidStarImport


@Service
@ComponentScan({ "uk.gov.hmcts.reform" })
public class TestUtil {

    private String idamAuth;
    private String s2sAuth;

    @Value("${test.url}")
    private String testUrl;

    @Value("${document_management.url}")
    private String dmApiUrl;

    private final IdamHelper idamHelper;

    private final S2sHelper s2sHelper;

    private final S2sHelper cdamS2sHelper;

    private final DmHelper dmHelper;

    private final CcdDataHelper ccdDataHelper;

    private final CdamHelper cdamHelper;

    public TestUtil(IdamHelper idamHelper, S2sHelper s2sHelper, @Qualifier("xuiS2sHelper") S2sHelper cdamS2sHelper,
                    DmHelper dmHelper, CcdDataHelper ccdDataHelper, CdamHelper cdamHelper) {
        this.idamHelper = idamHelper;
        this.s2sHelper = s2sHelper;
        this.cdamS2sHelper = cdamS2sHelper;
        this.dmHelper = dmHelper;
        this.ccdDataHelper = ccdDataHelper;
        this.cdamHelper = cdamHelper;
    }

    public static final String CREATE_CASE_TEMPLATE = """
        {
            "caseTitle": null,
            "caseOwner": null,
            "caseCreationDate": null,
            "caseDescription": null,
            "caseComments": null,
            "caseDocuments": %s
          }""";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final List<String> stitchingTestUserRoles = Stream.of(
            CASEWORKER_ROLE,
            CASEWORKER_PUBLICLAW_ROLE,
            CCD_IMPORT_ROLE)
        .toList();

    @PostConstruct
    public void init() {
        idamHelper.createUser(STITCHING_TEST_USER_EMAIL, stitchingTestUserRoles);
        SerenityRest.useRelaxedHTTPSValidation();
        idamAuth = idamHelper.authenticateUser(STITCHING_TEST_USER_EMAIL);
        s2sAuth = s2sHelper.getS2sToken();
    }

    public RequestSpecification authRequest() {
        return s2sAuthRequest()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .header(AUTH_HEADER, idamAuth);
    }

    public RequestSpecification unauthenticatedRequest() {
        return SerenityRest.given();
    }

    private RequestSpecification s2sAuthRequest() {
        return SerenityRest
            .given()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTH_HEADER, s2sAuth);
    }

    public File downloadDocument(String documentUrl) throws IOException {
        String documentId = documentUrl.substring(documentUrl.lastIndexOf('/') + 1);
        Path tempPath = Files.createTempFile(documentId + "-", "-test.pdf");
        Files.copy(dmHelper.getDocumentBinary(documentId), tempPath, StandardCopyOption.REPLACE_EXISTING);
        tempPath.toFile().deleteOnExit();
        return tempPath.toFile();
    }

    public String uploadDocument(String pdfName) {
        try {
            return dmHelper.getDocumentMetadata(
                dmHelper.uploadAndGetId(
                    ClassLoader.getSystemResourceAsStream(pdfName), APPLICATION_PDF, pdfName))
                .links.self.href;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private String uploadDocument() {
        return uploadDocument(HUNDRED_PAGE_PDF);
    }

    public BundleDTO getTestBundle() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), DOCUMENT_1_TITLE));
        docs.add(getTestBundleDocument(uploadDocument(), DOCUMENT_2_TITLE));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleforFailure() throws IOException {
        ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final File bundleJsonFile = new File(ClassLoader.getSystemResource("bundle.json").getPath());

        return mapper.readValue(bundleJsonFile, BundleDTO.class);
    }

    public BundleDTO getTestBundleWithOnePageDocuments() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(DOCUMENT_1_PDF), DOCUMENT_1_TITLE));
        docs.add(getTestBundleDocument(uploadDocument(DOCUMENT_2_PDF), DOCUMENT_2_TITLE));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithLargeToc() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument("five-hundred-page.pdf"), "Document 3"));
        docs.add(getTestBundleDocument(uploadDocument("annotationTemplate.pdf"), "Document 4"));
        docs.add(getTestBundleDocument(uploadDocument("SamplePDF_special_characters.pdf"), "Document 5"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleOutlineWithNoDestination() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(
            uploadDocument("Document-With-Outlines-No-Page-Links.pdf"), DOCUMENT_1_TITLE));
        docs.add(getTestBundleDocument(
            uploadDocument("Document-With-Outlines-No-Page-Links.pdf"), DOCUMENT_2_TITLE));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithOneDocumentWithAOutline() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(ONE_PAGE_PDF), DOCUMENT_1_TITLE));
        docs.add(getTestBundleDocument(uploadDocument(DOCUMENT_1_PDF), DOCUMENT_2_TITLE));
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
        docs.add(getTestBundleDocument(uploadDocument(), TEST_PDF_TITLE));
        docs.add(getTestBundleDocument(uploadFile(WORD_DOCUMENT_DOC, APPLICATION_MS_WORD), TEST_WORD_DOCUMENT_TITLE));
        docs.add(getTestBundleDocument(uploadFile(WORD_DOCUMENT_2_DOCX, DOCX_MIME_TYPE), "Test DocX"));
        docs.add(getTestBundleDocument(uploadFile(LARGE_DOCUMENT_DOCX, DOCX_MIME_TYPE), TEST_WORD_DOCUMENT_TITLE));
        docs.add(getTestBundleDocument(uploadFile(WORD_DOCUMENT_ZIP_DOCX, DOCX_MIME_TYPE), "Test Word DocX/Zip"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithTextFile() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Text File");
        bundle.setDescription("This bundle contains Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), TEST_PDF_TITLE));
        docs.add(getTestBundleDocument(
            uploadFile("sample_text_file.txt", TEXT_PLAIN_MIME_TYPE), "Test Text File"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithRichTextFile() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Rich Text File");
        bundle.setDescription("This bundle contains Rich Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), TEST_PDF_TITLE));
        docs.add(getTestBundleDocument(uploadFile("rtf.rtf", RTF_MIME_TYPE), "Rich Text File"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithExcelAndPptDoc() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Excel and PPT Documents");
        bundle.setDescription("This bundle contains PPT and Excel documents that have been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(), TEST_PDF_TITLE));
        docs.add(getTestBundleDocument(uploadFile(WORD_DOCUMENT_DOC, APPLICATION_MS_WORD), TEST_WORD_DOCUMENT_TITLE));
        docs.add(getTestBundleDocument(uploadFile(LARGE_DOCUMENT_DOCX, DOCX_MIME_TYPE), TEST_WORD_DOCUMENT_TITLE));
        docs.add(getTestBundleDocument(uploadFile("Performance_Out.pptx", PPTX_MIME_TYPE), "Test PPTX"));
        docs.add(getTestBundleDocument(uploadFile("TestExcelConversion.xlsx", XLSX_MIME_TYPE), "Test XLSX"));
        docs.add(getTestBundleDocument(uploadFile("XLSsample.xls", XLS_MIME_TYPE), "Test XLS"));
        docs.add(getTestBundleDocument(
            uploadFile("Portable_XR_ReportTemplate.xltx", XLTX_MIME_TYPE), "Test XLTX"));
        docs.add(getTestBundleDocument(uploadFile("file_example_PPT_250kB.ppt", PPT_MIME_TYPE), "Test PPT"));
        docs.add(getTestBundleDocument(uploadFile("sample.ppsx", PPSX_MIME_TYPE), "Test PPSX"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithImage() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle with Image");
        bundle.setDescription("This bundle contains an Image that has been converted by pdfbox");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(uploadDocument(ONE_PAGE_PDF), DOCUMENT_1_TITLE));
        docs.add(getTestBundleDocument(uploadDocument(DOCUMENT_1_PDF), DOCUMENT_2_TITLE));
        docs.add(getTestBundleDocument(uploadFile(FLYING_PIG_JPEG, IMAGE_JPEG_MIME_TYPE), "Welcome to the flying pig"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getTestBundleWithWatermarkImage() {
        DocumentImage documentImage = new DocumentImage();
        documentImage.setDocmosisAssetId(HMCTS_PNG_ASSET_ID);
        documentImage.setImageRendering(ImageRendering.OPAQUE);
        documentImage.setImageRenderingLocation(ImageRenderingLocation.FIRST_PAGE);
        documentImage.setCoordinateX(50);
        documentImage.setCoordinateY(50);

        BundleDTO bundle = getTestBundleWithImage();
        bundle.setDocumentImage(documentImage);

        return bundle;
    }

    private String uploadFile(String fileName, String mimeType) {
        return s2sAuthRequest()
            .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
            .multiPart("files", fileName, ClassLoader.getSystemResourceAsStream(fileName), mimeType)
            .multiPart("classification", PUBLIC_CLASSIFICATION)
            .request("POST", getDmApiUrl() + "/documents")
            .getBody()
            .jsonPath()
            .get("_embedded.documents[0]._links.self.href");
    }

    public BundleDTO getTestBundleWithDuplicateBundleDocuments() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();
        BundleDocumentDTO uploadedDocument = getTestBundleDocument(uploadDocument(), DOCUMENT_1_TITLE);
        docs.add(uploadedDocument);
        docs.add(uploadedDocument);
        bundle.setDocuments(docs);
        return bundle;
    }

    public BundleDTO getTestBundleWithSortedDocuments() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocumentWithSortIndices(uploadDocument(DOCUMENT_1_PDF), DOCUMENT_1_PDF, 2));
        docs.add(getTestBundleDocumentWithSortIndices(uploadDocument(DOCUMENT_2_PDF), DOCUMENT_2_PDF, 1));
        bundle.setDocuments(docs);
        return bundle;
    }

    private BundleDocumentDTO getTestBundleDocumentWithSortIndices(String documentUrl, String title, int sortIndex) {
        BundleDocumentDTO document = getTestBundleDocument(documentUrl, title);
        document.setSortIndex(sortIndex);
        return document;
    }

    public Response pollUntil(String endpoint,
                              Predicate<JsonPath> evaluator)
        throws InterruptedException, IOException {
        return pollUntil(endpoint, evaluator, 30);
    }

    private Response pollUntil(String endpoint,
                               Predicate<JsonPath> evaluator,
                               int numRetries) throws InterruptedException, IOException {

        for (int i = 0; i < numRetries; i++) {
            Response response = authRequest().get(endpoint);

            if (response.getStatusCode() == 500) {
                throw new IOException("HTTP 500 from service");
            }
            if (evaluator.test(response.body().jsonPath())) {
                return response;
            }

            Thread.sleep(1000);
        }

        throw new IOException("Task not in the correct state after max number of retries.");
    }

    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
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

        return pollUntil(taskUrl, body -> body.getString(TASK_STATE_FIELD).equals(DONE_STATE));
    }

    /**
     * Creates a bundle with structure:.
     *
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
        bundle.setBundleTitle(BUNDLE_WITH_FOLDERS_TITLE);
        bundle.setDescription(SUPER_GREAT_BUNDLE_DESCRIPTION);
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        bundle.setHasFolderCoversheets(false);

        BundleFolderDTO folder = new BundleFolderDTO();
        folder.setFolderName(FOLDER_1_NAME);
        folder.getDocuments().add(getTestBundleDocumentWithSortIndices(
            uploadDocument(DOCUMENT_1_PDF), DOCUMENT_1_PDF, 1));
        folder.setSortIndex(1);
        bundle.getFolders().add(folder);

        BundleFolderDTO folder2 = new BundleFolderDTO();
        folder2.setFolderName(FOLDER_2_NAME);
        folder2.getDocuments().add(getTestBundleDocumentWithSortIndices(
            uploadDocument(DOCUMENT_2_PDF), DOCUMENT_2_PDF, 1));
        folder2.setSortIndex(2);
        bundle.getFolders().add(folder2);

        return bundle;
    }

    /**
     * Creates a bundle with structure:.
     *
     * <p>
     * - Folder 1
     * - Document 1
     * - Folder 2
     * - Document 2
     * </p>
     */
    public BundleDTO getTestBundleWithFlatFoldersAndLongDocumentTitle() {
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_WITH_FOLDERS_TITLE);
        bundle.setDescription(SUPER_GREAT_BUNDLE_DESCRIPTION
            + " It is long enough to wrap and show in more than one line");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        bundle.setHasFolderCoversheets(false);

        BundleFolderDTO folder = new BundleFolderDTO();
        folder.setFolderName(FOLDER_1_NAME);
        String text = Stream.generate(() -> "DocName ").limit(20).collect(Collectors.joining());
        text += ".pdf";
        folder.getDocuments().add(getTestBundleDocumentWithSortIndices(
            uploadDocument(DOCUMENT_1_PDF), text, 1));
        folder.setSortIndex(1);
        bundle.getFolders().add(folder);

        BundleFolderDTO folder2 = new BundleFolderDTO();
        folder2.setFolderName(FOLDER_2_NAME);
        folder2.getDocuments().add(getTestBundleDocumentWithSortIndices(
            uploadDocument(DOCUMENT_2_PDF), DOCUMENT_2_PDF, 1));
        folder2.setSortIndex(2);
        bundle.getFolders().add(folder2);

        return bundle;
    }

    /**
     * Creates a bundle with structure:.
     *
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
        bundle.setBundleTitle(BUNDLE_WITH_FOLDERS_TITLE);
        bundle.setDescription(SUPER_GREAT_BUNDLE_DESCRIPTION);
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        bundle.setHasFolderCoversheets(false);

        BundleFolderDTO folder = new BundleFolderDTO();
        folder.setFolderName(FOLDER_1_NAME);
        folder.getDocuments().add(getTestBundleDocumentWithSortIndices(
            uploadDocument(DOCUMENT_1_PDF), DOCUMENT_1_PDF, 1));
        folder.setSortIndex(1);
        bundle.getFolders().add(folder);

        BundleFolderDTO folder1a = new BundleFolderDTO();
        folder1a.setFolderName("Folder 1a");
        folder1a.getDocuments().add(getTestBundleDocumentWithSortIndices(
            uploadDocument(DOCUMENT_1_PDF), "Document1a.pdf", 1));
        folder1a.setSortIndex(2);
        folder.getFolders().add(folder1a);

        BundleFolderDTO folder1b = new BundleFolderDTO();
        folder1b.setFolderName("Folder 1b");
        folder1b.getDocuments().add(
            getTestBundleDocumentWithSortIndices(uploadDocument(DOCUMENT_1_PDF), "Document1b.pdf", 1));
        folder1b.setSortIndex(3);
        folder.getFolders().add(folder1b);

        BundleFolderDTO folder2 = new BundleFolderDTO();
        folder2.setFolderName(FOLDER_2_NAME);
        folder2.getDocuments().add(
            getTestBundleDocumentWithSortIndices(uploadDocument(DOCUMENT_2_PDF), DOCUMENT_2_PDF, 1));
        folder2.setSortIndex(2);
        bundle.getFolders().add(folder2);

        return bundle;
    }

    public static int getNumPages(File file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file)) {
            return doc.getNumberOfPages();
        }
    }

    public static PDDocumentOutline getDocumentOutline(File file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file)) {
            return doc.getDocumentCatalog().getDocumentOutline();
        }
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
            .header(AUTH_HEADER, (String) null);
    }

    public RequestSpecification emptyIdamAuthAndEmptyS2SAuth() {
        return SerenityRest
            .given()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTH_HEADER, (String) null)
            .header(AUTH_HEADER, (String) null);
    }

    public RequestSpecification validAuthRequestWithEmptyS2SAuth() {
        return emptyS2sAuthRequest().header(AUTH_HEADER, idamAuth);
    }

    public RequestSpecification validS2SAuthWithEmptyIdamAuth() {
        return s2sAuthRequest().header(AUTH_HEADER, (String) null);
    }

    private RequestSpecification emptyS2sAuthRequest() {
        return SerenityRest
            .given()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTH_HEADER, null);
    }

    public RequestSpecification invalidIdamAuthrequest() {
        return s2sAuthRequest().header(AUTH_HEADER, "invalidIDAMAuthRequest");
    }

    public RequestSpecification invalidS2SAuth() {
        return invalidS2sAuthRequest().header(AUTH_HEADER, idamAuth);
    }

    private RequestSpecification invalidS2sAuthRequest() {
        return SerenityRest
            .given()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTH_HEADER, "invalidS2SAuthorization");
    }

    // CDAM

    public String getServiceAuth() {
        return cdamS2sHelper.getS2sToken();
    }

    public CaseDetails createCase(String documents) throws JsonProcessingException {
        return ccdDataHelper.createCase(
            STITCHING_TEST_USER_EMAIL, JURISDICTION, getEnvCcdCaseTypeId(), "createCase",
            objectMapper.readTree(String.format(CREATE_CASE_TEMPLATE, documents)));
    }

    public String getEnvCcdCaseTypeId() {
        return "CCD_BUNDLE_MVP_TYPE_ASYNC";
    }

    public List<String> uploadCdamDocuments(List<Pair<String, String>> fileDetails) throws JsonProcessingException {

        List<MultipartFile> multipartFiles = fileDetails.stream()
            .map(unchecked(pair -> createMultipartFile(pair.getFirst(), pair.getSecond())))
            .toList();

        DocumentUploadRequest uploadRequest = new DocumentUploadRequest(
            Classification.PUBLIC.toString(),
            getEnvCcdCaseTypeId(),
            JURISDICTION, multipartFiles);

        UploadResponse uploadResponse =  cdamHelper.uploadDocuments(STITCHING_TEST_USER_EMAIL,
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
    public List<String> createCaseAndUploadDocuments(UploadResponse uploadResponse) throws JsonProcessingException {
        List<CcdValue<CcdBundleDocumentDTO>> bundleDocuments = uploadResponse.getDocuments().stream()
            .map(this::createBundleDocument)
            .toList();
        String documentsString = objectMapper.writeValueAsString(bundleDocuments);
        createCase(documentsString);


        return uploadResponse.getDocuments().stream()
            .map(document -> document.links.self.href)
            .toList();
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

    public BundleDTO getCdamTestBundle() throws JsonProcessingException {

        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of(HUNDRED_PAGE_PDF, APPLICATION_PDF));
        fileDetails.add(Pair.of(HUNDRED_PAGE_PDF, APPLICATION_PDF));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();

        docs.add(getTestBundleDocument(docUrls.get(0), DOCUMENT_1_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(1), DOCUMENT_2_TITLE));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithWordDoc() throws JsonProcessingException {

        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of(HUNDRED_PAGE_PDF, APPLICATION_PDF));
        fileDetails.add(Pair.of(WORD_DOCUMENT_DOC, APPLICATION_MS_WORD));
        fileDetails.add(Pair.of(WORD_DOCUMENT_2_DOCX, DOCX_MIME_TYPE));
        fileDetails.add(Pair.of(LARGE_DOCUMENT_DOCX, DOCX_MIME_TYPE));
        fileDetails.add(Pair.of(WORD_DOCUMENT_ZIP_DOCX, DOCX_MIME_TYPE));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Word Documents");
        bundle.setDescription("This bundle contains Word documents that have been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), TEST_PDF_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(1), TEST_WORD_DOCUMENT_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(2),  "Test DocX"));
        docs.add(getTestBundleDocument(docUrls.get(3), TEST_WORD_DOCUMENT_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(4),"Test Word DocX/Zip"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithTextFile() throws JsonProcessingException {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of(HUNDRED_PAGE_PDF, APPLICATION_PDF));
        fileDetails.add(Pair.of("sample_text_file.txt", TEXT_PLAIN_MIME_TYPE));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Text File");
        bundle.setDescription("This bundle contains Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), TEST_PDF_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(1), "Test Text File"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithRichTextFile() throws JsonProcessingException {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of(HUNDRED_PAGE_PDF, APPLICATION_PDF));
        fileDetails.add(Pair.of("rtf.rtf", RTF_MIME_TYPE));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Rich Text File");
        bundle.setDescription("This bundle contains Rich Text File that has been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), TEST_PDF_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(1), "Rich Text File"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithExcelAndPptDoc() throws JsonProcessingException {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of(HUNDRED_PAGE_PDF, APPLICATION_PDF));
        fileDetails.add(Pair.of(WORD_DOCUMENT_DOC, APPLICATION_MS_WORD));
        fileDetails.add(Pair.of(LARGE_DOCUMENT_DOCX, DOCX_MIME_TYPE));
        fileDetails.add(Pair.of("Performance_Out.pptx", PPTX_MIME_TYPE));
        fileDetails.add(Pair.of("TestExcelConversion.xlsx", XLSX_MIME_TYPE));
        fileDetails.add(Pair.of("XLSsample.xls", XLS_MIME_TYPE));
        fileDetails.add(Pair.of("Portable_XR_ReportTemplate.xltx", XLTX_MIME_TYPE));
        fileDetails.add(Pair.of("potential_and_kinetic.ppt", PPT_MIME_TYPE));
        fileDetails.add(Pair.of("sample.ppsx", PPSX_MIME_TYPE));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle of Excel and PPT Documents");
        bundle.setDescription("This bundle contains PPT and Excel documents that have been converted by Docmosis.");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), TEST_PDF_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(1), TEST_WORD_DOCUMENT_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(2), TEST_WORD_DOCUMENT_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(3),"Test PPTX"));
        docs.add(getTestBundleDocument(docUrls.get(4),"Test XLSX"));
        docs.add(getTestBundleDocument(docUrls.get(5), "Test XLS"));
        docs.add(getTestBundleDocument(docUrls.get(6),"Test XLTX"));
        docs.add(getTestBundleDocument(docUrls.get(7), "Test PPT"));
        docs.add(getTestBundleDocument(docUrls.get(8),"Test PPSX"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithImage() throws JsonProcessingException {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of(ONE_PAGE_PDF, APPLICATION_PDF));
        fileDetails.add(Pair.of(DOCUMENT_1_PDF, APPLICATION_PDF));
        fileDetails.add(Pair.of(FLYING_PIG_JPEG, IMAGE_JPEG_MIME_TYPE));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle("Bundle with Image");
        bundle.setDescription("This bundle contains an Image that has been converted by pdfbox");
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocument(docUrls.get(0), DOCUMENT_1_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(1), DOCUMENT_2_TITLE));
        docs.add(getTestBundleDocument(docUrls.get(2), "Welcome to the flying pig"));
        bundle.setDocuments(docs);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithWatermarkImage() throws JsonProcessingException {
        DocumentImage documentImage = new DocumentImage();
        documentImage.setDocmosisAssetId(HMCTS_PNG_ASSET_ID);
        documentImage.setImageRendering(ImageRendering.OPAQUE);
        documentImage.setImageRenderingLocation(ImageRenderingLocation.FIRST_PAGE);
        documentImage.setCoordinateX(50);
        documentImage.setCoordinateY(50);

        BundleDTO bundle = getCdamTestBundleWithImage();
        bundle.setDocumentImage(documentImage);

        return bundle;
    }

    public BundleDTO getCdamTestBundleWithDuplicateBundleDocuments() throws JsonProcessingException {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of(HUNDRED_PAGE_PDF, APPLICATION_PDF));

        List<String> docUrls = uploadCdamDocuments(fileDetails);
        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();
        BundleDocumentDTO uploadedDocument = getTestBundleDocument(docUrls.getFirst(), DOCUMENT_1_TITLE);
        docs.add(uploadedDocument);
        docs.add(uploadedDocument);
        bundle.setDocuments(docs);
        return bundle;
    }

    public BundleDTO getCdamTestBundleWithSortedDocuments() throws JsonProcessingException {
        List<Pair<String, String>> fileDetails = new ArrayList<>();
        fileDetails.add(Pair.of(DOCUMENT_1_PDF, APPLICATION_PDF));
        fileDetails.add(Pair.of(DOCUMENT_2_PDF, APPLICATION_PDF));

        List<String> docUrls = uploadCdamDocuments(fileDetails);

        BundleDTO bundle = new BundleDTO();
        bundle.setBundleTitle(BUNDLE_TITLE);
        bundle.setDescription(BUNDLE_DESCRIPTION);
        List<BundleDocumentDTO> docs = new ArrayList<>();
        docs.add(getTestBundleDocumentWithSortIndices(docUrls.get(0), DOCUMENT_1_PDF, 2));
        docs.add(getTestBundleDocumentWithSortIndices(docUrls.get(1), DOCUMENT_2_PDF, 1));
        bundle.setDocuments(docs);
        return bundle;
    }
}