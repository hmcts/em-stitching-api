package uk.gov.hmcts.reform.em.stitching.domain;

import javax.persistence.Transient;
import java.util.List;
import java.util.stream.Stream;

public interface BundleContainer {

    @Transient
    default Stream<BundleFolder> getNestedFolders() {
        return Stream.concat(
            getFolders().stream(),
            getFolders().stream().flatMap(BundleContainer::getNestedFolders)
        ).filter(f -> !f.getDocuments().isEmpty() || f.getFolders().size()>0);
    }

    List<BundleFolder> getFolders();
}
