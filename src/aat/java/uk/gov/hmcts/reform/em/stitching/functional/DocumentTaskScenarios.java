package uk.gov.hmcts.reform.em.stitching.functional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.response.Response;
import jodd.csselly.selector.PseudoClass;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDocumentDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;
import uk.gov.hmcts.reform.em.stitching.testutil.Env;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DocumentTaskScenarios {

    TestUtil testUtil = new TestUtil();

    @Test
    public void testPostBundleStitch() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(convertObjectToJsonBytes(documentTask))
            .request("POST", Env.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithWordDoc() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithWordDoc();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(convertObjectToJsonBytes(documentTask))
            .request("POST", Env.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostBundleStitchWithImage() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithImage();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(convertObjectToJsonBytes(documentTask))
            .request("POST", Env.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response getTaskResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, getTaskResponse.getStatusCode());
        Assert.assertNotNull(getTaskResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testPostDocumentTask() throws IOException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();

        documentTask.setBundle(bundle);

        Response response = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, response.getStatusCode());
        Assert.assertEquals(response.getBody().jsonPath().getString("taskState"), TaskState.NEW.toString());
    }

    @Test
    public void testStitchTwoIdenticalDocuments() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithDuplicateBundleDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        Assert.assertEquals(201, createTaskResponse.getStatusCode());
        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        Assert.assertEquals(200, completedResponse.getStatusCode());
        Assert.assertNotNull(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
    }

    @Test
    public void testStitchDocumentsWithSortIndices() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithSortedDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        BundleDocument stitchedDocument = new BundleDocument();
        stitchedDocument.setDocumentURI(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        File stitchedFile = testUtil.downloadDocument(stitchedDocument.getDocumentURI());

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String parsedText = pdfStripper.getText(stitchedPdDocument);

        int indexOfDocument1 = parsedText.indexOf("Document 1");
        int indexOfDocument2 = parsedText.indexOf("Document 2");
        Assert.assertTrue(indexOfDocument2 < indexOfDocument1);
    }

    @Test
    public void testDefaultValuesForTableOfContentsAndCoversheets() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        BundleDocument stitchedDocument = new BundleDocument();
        stitchedDocument.setDocumentURI(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        File stitchedFile = testUtil.downloadDocument(stitchedDocument.getDocumentURI());

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDDocument document1 = PDDocument.load(new File(ClassLoader.getSystemResource("Document1.pdf").getPath()));
        PDDocument document2 = PDDocument.load(new File(ClassLoader.getSystemResource("Document2.pdf").getPath()));
        final int noOfPagesInTableOfContentsAndTwoCoversheets = 3;
        Assert.assertEquals(stitchedPdDocument.getNumberOfPages(),
                document1.getNumberOfPages() + document2.getNumberOfPages() + noOfPagesInTableOfContentsAndTwoCoversheets);
    }

   @Test
    public void testTableOfContentsOffCoversheetsOn() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasTableOfContents(false);
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

       Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        BundleDocument stitchedDocument = new BundleDocument();
        stitchedDocument.setDocumentURI(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        File stitchedFile = testUtil.downloadDocument(stitchedDocument.getDocumentURI());

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDDocument document1 = PDDocument.load(new File(ClassLoader.getSystemResource("Document1.pdf").getPath()));
        PDDocument document2 = PDDocument.load(new File(ClassLoader.getSystemResource("Document2.pdf").getPath()));
        final int noOfPagesInOnlyTwoCoversheets = 2;
        Assert.assertEquals(stitchedPdDocument.getNumberOfPages(),
                document1.getNumberOfPages() + document2.getNumberOfPages() + noOfPagesInOnlyTwoCoversheets);
    }

   @Test
    public void testTableOfContentsOnCoversheetsOff() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasCoversheets(false);
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

       Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        BundleDocument stitchedDocument = new BundleDocument();
        stitchedDocument.setDocumentURI(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        File stitchedFile = testUtil.downloadDocument(stitchedDocument.getDocumentURI());

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDDocument document1 = PDDocument.load(new File(ClassLoader.getSystemResource("Document1.pdf").getPath()));
        PDDocument document2 = PDDocument.load(new File(ClassLoader.getSystemResource("Document2.pdf").getPath()));
        final int noOfPagesInOnlyTableOfContents = 1;
        Assert.assertEquals(stitchedPdDocument.getNumberOfPages(),
                document1.getNumberOfPages() + document2.getNumberOfPages() + noOfPagesInOnlyTableOfContents);
    }

   @Test
    public void testTableOfContentsOffCoversheetsOff() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundleWithOnePageDocuments();
        bundle.setHasCoversheets(false);
        bundle.setHasTableOfContents(false);
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        BundleDocument stitchedDocument = new BundleDocument();
        stitchedDocument.setDocumentURI(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        File stitchedFile = testUtil.downloadDocument(stitchedDocument.getDocumentURI());

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDDocument document1 = PDDocument.load(new File(ClassLoader.getSystemResource("Document1.pdf").getPath()));
        PDDocument document2 = PDDocument.load(new File(ClassLoader.getSystemResource("Document2.pdf").getPath()));
        Assert.assertEquals(stitchedPdDocument.getNumberOfPages(),
                document1.getNumberOfPages() + document2.getNumberOfPages());
    }

// BELOW
//    @Test
//    public void testContentsPageDefaultsToTrue() throws IOException, InterruptedException {
//        BundleDTO bundle = testUtil.getTestBundle();
//
//        String firstDocTitle = testBundle.getDocuments().get(0).getDocTitle();
//        testBundle.setHasTableOfContents(true);
//        testBundle.setHasCoversheets(true);
//
//        // This prep work should stay
//        DocumentTask documentTask = new DocumentTask();
//        documentTask.setTaskState(TaskState.NEW);
//        documentTask.setBundle(testBundle);
//
//        // TODO This mocking paragraph can go
//        URL urlDoc1 = ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf");
//        URL urlDoc2 = ClassLoader.getSystemResource("annotationTemplate.pdf");
//        Pair<BundleDocument, File> mockPair1 = Pair.of(testBundle.getDocuments().get(0), new File(urlDoc1.getFile()));
//        Pair<BundleDocument, File> mockPair2 = Pair.of(testBundle.getDocuments().get(1), new File(urlDoc2.getFile()));
//        BDDMockito.given(dmStoreDownloader.downloadFiles(any())).willReturn(Stream.of(mockPair1, mockPair2));
//        BDDMockito.given(documentConverter.convert(mockPair1)).willReturn(mockPair1);
//        BDDMockito.given(documentConverter.convert(mockPair2)).willReturn(mockPair2);
//
//        // TODO Finish this test. It should get the merged document (using documentTask.getBundle().getStitchedDocUri()?),
//        //  and check if the bundle's first document's title shows up ONCE more in the stitched doc than it did in the individual docs.
//        File stitchedFile = ;// get stitched Document
//        String stitchedDocumentText = pdfStripper.getText(PDDocument.load(stitchedFile));
//
//        // TODO ensure that these are the files being added to the bundle
//        String firstFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_1));
//        String secondFileDocumentText = pdfStripper.getText(PDDocument.load(FILE_2));
//        int stitchedDocTitleFrequency = count(stitchedDocumentText, firstDocTitle);
//        int firstDocTitleFrequency = count(firstFileDocumentText, firstDocTitle);
//        int secondDocTitleFrequency = count(secondFileDocumentText, firstDocTitle);
//        final int contentsPageDocTitleFrequency = 1;
//
//        Assert.assertEquals(stitchedDocTitleFrequency,firstDocTitleFrequency + secondDocTitleFrequency + contentsPageDocTitleFrequency);
//    }
//ABOVE

    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);

        return mapper.writeValueAsBytes(object);
    }

}
