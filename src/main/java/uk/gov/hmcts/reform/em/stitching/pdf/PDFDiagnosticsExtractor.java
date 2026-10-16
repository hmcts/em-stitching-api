package uk.gov.hmcts.reform.em.stitching.pdf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PDFDiagnosticsExtractor {

    private static final int MAX_STRING_FIELD_LENGTH = 255;
    private static final int MAX_ERROR_STACK_LENGTH = 1000;
    public static final int MAX_DB_COLUMN_LENGTH = 5000;

    private static final String SERIALIZATION_FAILURE =
        "{\"diagnosticsStatus\":\"SERIALIZATION_FAILED\"}";

    private static final String ROOT_CAUSE_STACK = "rootCauseStack";
    private static final String DIAGNOSTICS_TRUNCATED = "diagnosticsTruncated";

    private static final List<String> MINIMAL_DIAGNOSTIC_FIELDS = List.of(
        "bundleDocumentId",
        "documentTitle",
        "errorClass",
        "errorMessage"
    );

    private final ObjectWriter objectWriter;

    public PDFDiagnosticsExtractor(ObjectMapper objectMapper) {
        this.objectWriter = objectMapper.copy().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            .writer();
    }

    public String extractDiagnosticsJson(
        File file,
        String documentTitle,
        String bundleDocumentId,
        Throwable failure
    ) {
        try {
            Map<String, Object> diagnostics = new LinkedHashMap<>();
            diagnostics.put("bundleDocumentId", truncate(bundleDocumentId, MAX_STRING_FIELD_LENGTH));
            diagnostics.put("documentTitle", truncate(documentTitle, MAX_STRING_FIELD_LENGTH));
            diagnostics.put("fileSize", file != null && file.exists() ? file.length() : 0);

            addFailureDetails(diagnostics, failure);

            if (file == null || !file.exists()) {
                diagnostics.put("fileStatus", "FILE_NOT_FOUND");
                return serialize(diagnostics);
            }

            diagnostics.put("rawHeader", readHeader(file));
            addPdfDetails(diagnostics, file);

            return serialize(diagnostics);
        } catch (Exception _) {
            return SERIALIZATION_FAILURE;
        }
    }

    private void addFailureDetails(Map<String, Object> diagnostics, Throwable failure) {
        if (failure == null) {
            return;
        }

        diagnostics.put("errorClass", failure.getClass().getName());
        diagnostics.put("errorMessage", truncate(failure.getMessage(), MAX_STRING_FIELD_LENGTH));
        diagnostics.put(
            ROOT_CAUSE_STACK,
            truncate(ExceptionUtils.getStackTrace(failure), MAX_ERROR_STACK_LENGTH)
        );
    }

    private void addPdfDetails(Map<String, Object> diagnostics, File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            diagnostics.put("pdfVersion", document.getVersion());
            diagnostics.put("isEncrypted", document.isEncrypted());
            diagnostics.put("pageCount", document.getNumberOfPages());

            addDocumentInformation(diagnostics, document.getDocumentInformation());
            addAcroFormInformation(diagnostics, document.getDocumentCatalog().getAcroForm());
            addFirstPageInformation(diagnostics, document);
        } catch (Exception exception) {
            diagnostics.put(
                "pdfBoxParseError",
                truncate(
                    exception.getClass().getName() + ": " + exception.getMessage(),
                    MAX_STRING_FIELD_LENGTH
                )
            );
        }
    }

    private void addDocumentInformation(
        Map<String, Object> diagnostics,
        PDDocumentInformation information
    ) {
        if (information == null) {
            return;
        }
        Map<String, Object> documentInformation = new LinkedHashMap<>();
        documentInformation.put("author", truncate(information.getAuthor(), MAX_STRING_FIELD_LENGTH));
        documentInformation.put("creator", truncate(information.getCreator(), MAX_STRING_FIELD_LENGTH));
        documentInformation.put("producer", truncate(information.getProducer(), MAX_STRING_FIELD_LENGTH));
        documentInformation.put("title", truncate(information.getTitle(), MAX_STRING_FIELD_LENGTH));
        documentInformation.put("subject", truncate(information.getSubject(), MAX_STRING_FIELD_LENGTH));
        documentInformation.put("keywords", truncate(information.getKeywords(), MAX_STRING_FIELD_LENGTH));

        if (information.getCreationDate() != null) {
            documentInformation.put(
                "creationDate",
                information.getCreationDate().toInstant().toString()
            );
        }

        if (information.getModificationDate() != null) {
            documentInformation.put(
                "modificationDate",
                information.getModificationDate().toInstant().toString()
            );
        }

        diagnostics.put("documentInfo", documentInformation);
    }

    private void addAcroFormInformation(
        Map<String, Object> diagnostics,
        PDAcroForm acroForm
    ) {
        diagnostics.put("hasAcroForm", acroForm != null);

        if (acroForm != null) {
            diagnostics.put("hasDynamicXFA", acroForm.hasXFA());
            diagnostics.put("hasSignatures", acroForm.isSignaturesExist());
        }
    }

    private void addFirstPageInformation(
        Map<String, Object> diagnostics,
        PDDocument document
    ) {
        if (document.getNumberOfPages() == 0 || document.getPage(0).getMediaBox() == null) {
            return;
        }

        var mediaBox = document.getPage(0).getMediaBox();
        diagnostics.put("firstPageWidth", mediaBox.getWidth());
        diagnostics.put("firstPageHeight", mediaBox.getHeight());
    }

    private String readHeader(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] header = new byte[32];
            int bytesRead = inputStream.read(header);

            return bytesRead > 0
                ? new String(header, 0, bytesRead, StandardCharsets.ISO_8859_1).trim()
                : "EMPTY";
        } catch (Exception _) {
            return "UNREADABLE";
        }
    }

    private String truncate(String value, int maximumLength) {
        return value == null ? null : StringUtils.abbreviate(value, maximumLength);
    }

    private String serialize(Map<String, Object> diagnostics) {
        try {
            String json = objectWriter.writeValueAsString(diagnostics);
            if (json.length() <= MAX_DB_COLUMN_LENGTH) {
                return json;
            }

            diagnostics.remove(ROOT_CAUSE_STACK);
            diagnostics.put(DIAGNOSTICS_TRUNCATED, true);

            json = objectWriter.writeValueAsString(diagnostics);
            if (json.length() <= MAX_DB_COLUMN_LENGTH) {
                return json;
            }

            Map<String, Object> minimalDiagnostics = new LinkedHashMap<>();
            MINIMAL_DIAGNOSTIC_FIELDS.forEach(field -> {
                if (diagnostics.containsKey(field)) {
                    minimalDiagnostics.put(field, diagnostics.get(field));
                }
            });
            minimalDiagnostics.put(DIAGNOSTICS_TRUNCATED, true);

            return objectWriter.writeValueAsString(minimalDiagnostics);
        } catch (Exception _) {
            return SERIALIZATION_FAILURE;
        }
    }
}