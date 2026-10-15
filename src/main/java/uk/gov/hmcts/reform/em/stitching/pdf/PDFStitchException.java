package uk.gov.hmcts.reform.em.stitching.pdf;

public class PDFStitchException extends Exception {

    private final String diagnosticsJson;

    public PDFStitchException(String message, String diagnosticsJson, Throwable cause) {
        super(message, cause);
        this.diagnosticsJson = diagnosticsJson;
    }

    public String getDiagnosticsJson() {
        return diagnosticsJson;
    }
}