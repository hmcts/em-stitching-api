package uk.gov.hmcts.reform.em.stitching.domain;

import java.util.stream.Stream;

public interface SortableBundleItem {

    String getTitle();

    String getDescription();

    Stream<SortableBundleItem> getSortedItems();

    Stream<BundleDocument> getSortedDocuments();

    int getSortIndex();

}
