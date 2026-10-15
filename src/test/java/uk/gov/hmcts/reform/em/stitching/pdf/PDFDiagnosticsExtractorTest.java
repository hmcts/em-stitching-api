package uk.gov.hmcts.reform.em.stitching.pdf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class PDFDiagnosticsExtractorTest {

    private ObjectMapper objectMapper;
    private PDFDiagnosticsExtractor diagnosticsExtractor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        diagnosticsExtractor = new PDFDiagnosticsExtractor(objectMapper);
    }

    private PDFDiagnosticsExtractor createExtractorWithMockWriter(ObjectWriter mockWriter) {
        ObjectMapper realMapper = new ObjectMapper();
        ObjectMapper spyMapper = spy(realMapper);
        ObjectMapper copyMapper = spy(realMapper.copy());

        when(spyMapper.copy()).thenReturn(copyMapper);
        when(copyMapper.writer()).thenReturn(mockWriter);

        return new PDFDiagnosticsExtractor(spyMapper);
    }

    @Nested
    @DisplayName("PDF File Processing Tests")
    class PdfProcessingTests {

        @Test
        void shouldExtractDiagnosticsFromValidPdfWithMetadataAndAcroForm() throws Exception {
            File pdfFile = tempDir.resolve("valid.pdf").toFile();

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                PDDocumentInformation info = doc.getDocumentInformation();
                info.setTitle("Test Title");
                info.setAuthor("Test Author");
                info.setCreator("Test Creator");
                info.setProducer("Test Producer");
                info.setSubject("Test Subject");
                info.setKeywords("tag1, tag2");
                Calendar now = Calendar.getInstance();
                info.setCreationDate(now);
                info.setModificationDate(now);

                PDAcroForm acroForm = new PDAcroForm(doc);
                doc.getDocumentCatalog().setAcroForm(acroForm);

                doc.save(pdfFile);
            }

            Exception failure = new RuntimeException("Test processing failure");

            String jsonResult = diagnosticsExtractor.extractDiagnosticsJson(
                pdfFile,
                "Sample Document",
                "doc-12345",
                failure
            );

            JsonNode root = objectMapper.readTree(jsonResult);

            assertEquals("doc-12345", root.get("bundleDocumentId").asText());
            assertEquals("Sample Document", root.get("documentTitle").asText());
            assertTrue(root.get("fileSize").asLong() > 0);
            assertTrue(root.get("rawHeader").asText().startsWith("%PDF-"));
            assertNotNull(root.get("pdfVersion"));
            assertFalse(root.get("isEncrypted").asBoolean());
            assertEquals(1, root.get("pageCount").asInt());

            assertEquals(PDRectangle.A4.getWidth(), root.get("firstPageWidth").floatValue());
            assertEquals(PDRectangle.A4.getHeight(), root.get("firstPageHeight").floatValue());

            JsonNode docInfo = root.get("documentInfo");
            assertNotNull(docInfo);
            assertEquals("Test Author", docInfo.get("author").asText());
            assertEquals("Test Creator", docInfo.get("creator").asText());
            assertEquals("Test Producer", docInfo.get("producer").asText());
            assertEquals("Test Title", docInfo.get("title").asText());
            assertEquals("Test Subject", docInfo.get("subject").asText());
            assertEquals("tag1, tag2", docInfo.get("keywords").asText());
            assertNotNull(docInfo.get("creationDate"));
            assertNotNull(docInfo.get("modificationDate"));

            assertTrue(root.get("hasAcroForm").asBoolean());
            assertFalse(root.get("hasDynamicXFA").asBoolean());
            assertFalse(root.get("hasSignatures").asBoolean());

            assertEquals("java.lang.RuntimeException", root.get("errorClass").asText());
            assertEquals("Test processing failure", root.get("errorMessage").asText());
            assertNotNull(root.get("rootCauseStack"));
        }

        @Test
        void shouldHandlePdfWithoutDocumentInformationOrAcroForm() throws Exception {
            File pdfFile = tempDir.resolve("plain.pdf").toFile();

            try (PDDocument doc = new PDDocument()) {
                doc.addPage(new PDPage());
                doc.save(pdfFile);
            }

            String jsonResult = diagnosticsExtractor.extractDiagnosticsJson(
                pdfFile,
                "Plain Document",
                "doc-001",
                null
            );

            JsonNode root = objectMapper.readTree(jsonResult);

            assertEquals("doc-001", root.get("bundleDocumentId").asText());
            assertFalse(root.get("hasAcroForm").asBoolean());
            assertNull(root.get("errorClass"));
            assertNull(root.get("errorMessage"));

            JsonNode docInfo = root.get("documentInfo");
            if (docInfo != null) {
                assertNull(docInfo.get("author"));
                assertNull(docInfo.get("title"));
            }
        }

        @Test
        void shouldRecordParseErrorWhenPdfIsCorrupted() throws Exception {
            File corruptedFile = tempDir.resolve("corrupted.pdf").toFile();
            try (FileOutputStream fos = new FileOutputStream(corruptedFile)) {
                fos.write("Not a real PDF file content at all".getBytes(StandardCharsets.UTF_8));
            }

            String jsonResult = diagnosticsExtractor.extractDiagnosticsJson(
                corruptedFile,
                "Corrupted Document",
                "doc-err",
                null
            );

            JsonNode root = objectMapper.readTree(jsonResult);

            // First 32 bytes read and trimmed
            assertEquals("Not a real PDF file content at a", root.get("rawHeader").asText());
            assertTrue(root.has("pdfBoxParseError"));
            assertTrue(root.get("pdfBoxParseError").asText().contains("IOException"));
        }

        @Test
        void shouldHandleEmptyFile() throws Exception {
            File emptyFile = tempDir.resolve("empty.pdf").toFile();
            assertTrue(emptyFile.createNewFile());

            String jsonResult = diagnosticsExtractor.extractDiagnosticsJson(
                emptyFile,
                "Empty Document",
                "doc-empty",
                null
            );

            JsonNode root = objectMapper.readTree(jsonResult);

            assertEquals(0, root.get("fileSize").asLong());
            assertEquals("EMPTY", root.get("rawHeader").asText());
            assertTrue(root.has("pdfBoxParseError"));
        }

        @Test
        void shouldHandleNonExistentFile() throws Exception {
            File nonExistentFile = tempDir.resolve("non-existent.pdf").toFile();

            String jsonResult = diagnosticsExtractor.extractDiagnosticsJson(
                nonExistentFile,
                "Missing Document",
                "doc-missing",
                new IOException("File read error")
            );

            JsonNode root = objectMapper.readTree(jsonResult);

            assertEquals("FILE_NOT_FOUND", root.get("fileStatus").asText());
            assertEquals(0, root.get("fileSize").asLong());
            assertEquals("java.io.IOException", root.get("errorClass").asText());
            assertEquals("File read error", root.get("errorMessage").asText());
        }

        @Test
        void shouldHandleNullFile() throws Exception {
            String jsonResult = diagnosticsExtractor.extractDiagnosticsJson(
                null,
                "Null File Document",
                "doc-null",
                null
            );

            JsonNode root = objectMapper.readTree(jsonResult);

            assertEquals("FILE_NOT_FOUND", root.get("fileStatus").asText());
            assertEquals(0, root.get("fileSize").asLong());
        }
    }

    @Nested
    @DisplayName("String Truncation Tests")
    class TruncationTests {

        @Test
        void shouldTruncateIndividualFieldsExceedingMaxStringLength() throws Exception {
            String longTitle = "Title_" + StringUtils.repeat("A", 300);
            String longBundleDocId = "Id_" + StringUtils.repeat("B", 300);
            String longErrorMessage = "Error_" + StringUtils.repeat("C", 300);

            Exception failure = new RuntimeException(longErrorMessage);

            String jsonResult = diagnosticsExtractor.extractDiagnosticsJson(
                null,
                longTitle,
                longBundleDocId,
                failure
            );

            JsonNode root = objectMapper.readTree(jsonResult);

            assertEquals(255, root.get("documentTitle").asText().length());
            assertTrue(root.get("documentTitle").asText().endsWith("..."));

            assertEquals(255, root.get("bundleDocumentId").asText().length());
            assertTrue(root.get("bundleDocumentId").asText().endsWith("..."));

            assertEquals(255, root.get("errorMessage").asText().length());
            assertTrue(root.get("errorMessage").asText().endsWith("..."));

            assertTrue(root.get("rootCauseStack").asText().length() <= 1000);
        }
    }

    @Nested
    @DisplayName("Serialization & Size Limit Fallback Tests")
    class SerializationLimitTests {

        @Test
        void shouldDropStackTraceWhenJsonExceedsMaxDbColumnLength() throws Exception {
            ObjectWriter mockWriter = mock(ObjectWriter.class);

            String oversizedJson = "{\"rootCauseStack\":\"" + StringUtils.repeat("X", 5001) + "\"}";
            String truncatedJson = "{\"diagnosticsTruncated\":true}";

            when(mockWriter.writeValueAsString(any()))
                .thenReturn(oversizedJson)
                .thenReturn(truncatedJson);

            PDFDiagnosticsExtractor extractor = createExtractorWithMockWriter(mockWriter);

            String result = extractor.extractDiagnosticsJson(
                null,
                "Test Title",
                "123",
                new RuntimeException("Error")
            );

            assertEquals(truncatedJson, result);
        }

        @Test
        void shouldFallbackToMinimalDiagnosticsWhenSecondSerializationStillExceedsMax() throws Exception {
            ObjectWriter mockWriter = mock(ObjectWriter.class);

            String oversizedFirst = StringUtils.repeat("A", 5005);
            String oversizedSecond = StringUtils.repeat("B", 5005);
            String minimalJson = "{\"bundleDocumentId\":\"123\",\"diagnosticsTruncated\":true}";

            when(mockWriter.writeValueAsString(any()))
                .thenReturn(oversizedFirst)
                .thenReturn(oversizedSecond)
                .thenReturn(minimalJson);

            PDFDiagnosticsExtractor extractor = createExtractorWithMockWriter(mockWriter);

            String result = extractor.extractDiagnosticsJson(
                null,
                "Title",
                "123",
                new RuntimeException("Error")
            );

            assertEquals(minimalJson, result);
        }

        @Test
        void shouldReturnSerializationFailureConstantWhenWriterThrowsException() throws Exception {
            ObjectWriter mockWriter = mock(ObjectWriter.class);

            when(mockWriter.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failed") {});

            PDFDiagnosticsExtractor extractor = createExtractorWithMockWriter(mockWriter);

            String result = extractor.extractDiagnosticsJson(
                null,
                "Title",
                "123",
                null
            );

            assertEquals("{\"diagnosticsStatus\":\"SERIALIZATION_FAILED\"}", result);
        }
    }
}