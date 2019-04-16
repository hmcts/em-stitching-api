package uk.gov.hmcts.reform.em.stitching.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.util.stream.Stream;

@Entity
@Table(name = "bundle_document")
public class BundleDocument extends AbstractAuditingEntity implements SortableBundleItem, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String docTitle;
    private String docDescription;
    private String documentURI;
    private int sortIndex;
    private String fileName;
    private boolean hasTableOfContents;
    private boolean hasCoversheets;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public String getDocDescription() {
        return docDescription;
    }

    public void setDocDescription(String docDescription) {
        this.docDescription = docDescription;
    }

    public String getDocumentURI() {
        return documentURI;
    }

    public void setDocumentURI(String documentURI) {
        this.documentURI = documentURI;
    }

    @Override
    @Transient
    public Stream<BundleDocument> getSortedItems() {
        return Stream.of(this);
    }

    @Override
    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean hasTableOfContents() {
        return hasTableOfContents;
    }

    public void setHasTableOfContents(boolean hasTableOfContents) {
        this.hasTableOfContents = hasTableOfContents;
    }

    public boolean hasCoversheets() {
        return hasCoversheets;
    }

    public void setHasCoversheets(boolean hasCoversheets) {
        this.hasCoversheets = hasCoversheets;
    }
}
