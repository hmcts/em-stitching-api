package uk.gov.hmcts.reform.em.stitching.domain;

import jakarta.persistence.Transient;

import java.util.List;
import java.util.stream.Stream;


public interface BundleContainer {

    @Transient
    default Stream<BundleFolder> getNestedFolders() {
        return Stream.concat(
                getFolders().stream(),
                getFolders().stream().flatMap(BundleContainer::getNestedFolders)
        ).filter(f -> hasAnyDoc(f));
    }

    private boolean hasAnyDoc(BundleFolder folder) {
        if (!folder.getDocuments().isEmpty()) {
            return true;
        }

        if (!folder.getFolders().isEmpty()) {
            for (var subFolder : folder.getFolders()) {
                if (hasAnyDoc(subFolder)) {
                    return true;
                }
            }
        }
        return false;
    }

    List<BundleFolder> getFolders();
}
