package uk.gov.hmcts.reform.em.stitching.domain;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.stream.Stream;

@Entity
@Table(name = "bundle_document")
public class BundleDocument extends AbstractAuditingEntity implements SortableBundleItem, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Size(max = 255, message = "Bundle Doc Title can not be more than 255 Chars")
    private String docTitle;
    @Size(max = 1000, message = "Bundle Doc Description can not be more than 1000 Chars")
    private String docDescription;
    private String documentURI;
    private int sortIndex;

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
    public String getTitle() {
        return getDocTitle();
    }

    @Override
    @Transient
    public String getDescription() {
        return getDocDescription();
    }

    @Override
    @Transient
    public Stream<SortableBundleItem> getSortedItems() {
        return Stream.empty();
    }

    @Override
    @Transient
    public Stream<BundleDocument> getSortedDocuments() {
        return Stream.of(this);
    }

    @Override
    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }

    @Override
    @Transient
    public BundleItemType getType() {
        return BundleItemType.DOCUMENT;
    }
}
