package uk.gov.hmcts.reform.em.stitching.functional;

import io.restassured.response.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.dto.BundleDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.testutil.TestUtil;
import uk.gov.hmcts.reform.em.stitching.testutil.Env;

import java.io.File;
import java.io.IOException;

public class DocumentTaskScenarios {

    private TestUtil testUtil = new TestUtil();

    @Test
    public void testPostBundleStitch() throws IOException, InterruptedException {
        BundleDTO bundle = testUtil.getTestBundle();
        DocumentTaskDTO documentTask = new DocumentTaskDTO();
        documentTask.setBundle(bundle);

        Response createTaskResponse = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
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
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
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
            .body(TestUtil.convertObjectToJsonBytes(documentTask))
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
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
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
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
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
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
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
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        BundleDocument stitchedDocument = new BundleDocument();
        stitchedDocument.setDocumentURI(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        File stitchedFile = testUtil.downloadDocument(stitchedDocument.getDocumentURI());

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDDocument document1 = PDDocument.load(new File(ClassLoader.getSystemResource("Document1.pdf").getPath()));
        PDDocument document2 = PDDocument.load(new File(ClassLoader.getSystemResource("Document2.pdf").getPath()));
        final long noOfPagesInTableOfContentsAndTwoCoversheets = 3;
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
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        BundleDocument stitchedDocument = new BundleDocument();
        stitchedDocument.setDocumentURI(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        File stitchedFile = testUtil.downloadDocument(stitchedDocument.getDocumentURI());

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDDocument document1 = PDDocument.load(new File(ClassLoader.getSystemResource("Document1.pdf").getPath()));
        PDDocument document2 = PDDocument.load(new File(ClassLoader.getSystemResource("Document2.pdf").getPath()));
        final long noOfPagesInOnlyTwoCoversheets = 2;
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
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
                .request("POST", Env.getTestUrl() + "/api/document-tasks");

        String taskUrl = "/api/document-tasks/" + createTaskResponse.getBody().jsonPath().getString("id");
        Response completedResponse = testUtil.pollUntil(taskUrl, body -> body.getString("taskState").equals("DONE"));

        BundleDocument stitchedDocument = new BundleDocument();
        stitchedDocument.setDocumentURI(completedResponse.getBody().jsonPath().getString("bundle.stitchedDocumentURI"));
        File stitchedFile = testUtil.downloadDocument(stitchedDocument.getDocumentURI());

        PDDocument stitchedPdDocument = PDDocument.load(stitchedFile);
        PDDocument document1 = PDDocument.load(new File(ClassLoader.getSystemResource("Document1.pdf").getPath()));
        PDDocument document2 = PDDocument.load(new File(ClassLoader.getSystemResource("Document2.pdf").getPath()));
        final long noOfPagesInOnlyTableOfContents = 1;
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
                .body(TestUtil.convertObjectToJsonBytes(documentTask))
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
}
