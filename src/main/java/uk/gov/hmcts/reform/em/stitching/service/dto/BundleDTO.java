package uk.gov.hmcts.reform.em.stitching.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentImage;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle.off;

public class BundleDTO extends AbstractAuditingDTO implements Serializable {

    @JsonIgnore
    private Long id;

    private String bundleTitle;
    private String description;
    private String stitchedDocumentURI;
    private String stitchStatus;
    private List<BundleFolderDTO> folders = new ArrayList<>();
    private List<BundleDocumentDTO> documents = new ArrayList<>();
    private String fileName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String fileNameIdentifier;
    private String coverpageTemplate;
    private JsonNode coverpageTemplateData;
    private PageNumberFormat pageNumberFormat = PageNumberFormat.numberOfPages;
    private boolean hasTableOfContents = true;
    private boolean hasCoversheets = true;
    private boolean hasFolderCoversheets = false;
    private PaginationStyle paginationStyle = off;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DocumentImage documentImage;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enableEmailNotification;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBundleTitle() {
        return bundleTitle;
    }

    public void setBundleTitle(String bundleTitle) {
        this.bundleTitle = bundleTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStitchedDocumentURI() {
        return stitchedDocumentURI;
    }

    public void setStitchedDocumentURI(String stitchedDocumentURI) {
        this.stitchedDocumentURI = stitchedDocumentURI;
    }

    public String getStitchStatus() {
        return stitchStatus;
    }

    public void setStitchStatus(String stitchStatus) {
        this.stitchStatus = stitchStatus;
    }

    public List<BundleFolderDTO> getFolders() {
        return folders;
    }

    public void setFolders(List<BundleFolderDTO> folders) {
        this.folders = folders;
    }

    public List<BundleDocumentDTO> getDocuments() {
        return documents;
    }

    public void setDocuments(List<BundleDocumentDTO> documents) {
        this.documents = documents;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileNameIdentifier() {
        return fileNameIdentifier;
    }

    public void setFileNameIdentifier(String fileNameIdentifier) {
        this.fileNameIdentifier = fileNameIdentifier;
    }

    public String getCoverpageTemplate() {
        return coverpageTemplate;
    }

    public void setCoverpageTemplate(String coverpageTemplate) {
        this.coverpageTemplate = coverpageTemplate;
    }

    public JsonNode getCoverpageTemplateData() {
        return coverpageTemplateData;
    }

    public void setCoverpageTemplateData(JsonNode coverpageTemplateData) {
        this.coverpageTemplateData = coverpageTemplateData;
    }

    public PageNumberFormat getPageNumberFormat() {
        return pageNumberFormat;
    }

    public void setPageNumberFormat(PageNumberFormat pageNumberFormat) {
        this.pageNumberFormat = pageNumberFormat;
    }

    public boolean getHasTableOfContents() {
        return hasTableOfContents;
    }

    public void setHasTableOfContents(boolean hasTableOfContents) {
        this.hasTableOfContents = hasTableOfContents;
    }

    public boolean getHasCoversheets() {
        return hasCoversheets;
    }

    public void setHasCoversheets(boolean hasCoversheets) {
        this.hasCoversheets = hasCoversheets;
    }

    public boolean getHasFolderCoversheets() {
        return hasFolderCoversheets;
    }

    public void setHasFolderCoversheets(boolean hasFolderCoversheets) {
        this.hasFolderCoversheets = hasFolderCoversheets;
    }

    public PaginationStyle getPaginationStyle() {
        return paginationStyle;
    }

    public void setPaginationStyle(PaginationStyle paginationStyle) {
        this.paginationStyle = paginationStyle;
    }

    public Boolean getEnableEmailNotification() {
        return enableEmailNotification;
    }

    public void setEnableEmailNotification(Boolean enableEmailNotification) {
        this.enableEmailNotification = enableEmailNotification;
    }

    public DocumentImage getDocumentImage() {
        return documentImage;
    }

    public void setDocumentImage(DocumentImage documentImage) {
        this.documentImage = documentImage;
    }

    @Override
    public String toString() {
        return "BundleDTO{"
                + "id=" + id
                + ",\n bundleTitle='" + bundleTitle + '\''
                + ",\n description='" + description + '\''
                + ",\n stitchedDocumentURI='" + stitchedDocumentURI + '\''
                + ",\n stitchStatus='" + stitchStatus + '\''
                + ",\n folders=" + folders
                + ",\n documents=" + documents
                + ",\n fileName='" + fileName + '\''
                + ",\n fileNameIdentifier='" + fileNameIdentifier + '\''
                + ",\n coverpageTemplate='" + coverpageTemplate + '\''
                + ",\n coverpageTemplateData=" + coverpageTemplateData
                + ",\n pageNumberFormat=" + pageNumberFormat
                + ",\n hasTableOfContents=" + hasTableOfContents
                + ",\n hasCoversheets=" + hasCoversheets
                + ",\n hasFolderCoversheets=" + hasFolderCoversheets
                + ",\n paginationStyle=" + paginationStyle
                + ",\n documentImage=" + documentImage
                + ",\n enableEmailNotification=" + enableEmailNotification
                + '}';
    }
}

