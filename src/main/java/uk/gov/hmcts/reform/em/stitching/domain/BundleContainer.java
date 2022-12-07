package uk.gov.hmcts.reform.em.stitching.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Transient;
import java.util.List;
import java.util.stream.Stream;


public interface BundleContainer {
    Logger log = LoggerFactory.getLogger(BundleContainer.class);

    @Transient
    default Stream<BundleFolder> getNestedFolders() {
        return Stream.concat(
            getFolders().stream(),
            getFolders().stream().flatMap(BundleContainer::getNestedFolders)
        ).filter(f -> {
                    var r = hasAnyDoc(f);
                    log.info("folder name:{} hasAnyDoc:{}", f.getTitle(), r);
                    return r;
                }
        );
    }

    private boolean hasAnyDoc(BundleFolder folder) {
        if (!folder.getDocuments().isEmpty()) {
            return true;
        }

        if (!folder.getFolders().isEmpty()) {
            for (var subFolder : folder.getFolders())
                return hasAnyDoc(subFolder);
        }
        return false;
    }

    List<BundleFolder> getFolders();
}
