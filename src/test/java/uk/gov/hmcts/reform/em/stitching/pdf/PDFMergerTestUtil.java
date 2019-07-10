package uk.gov.hmcts.reform.em.stitching.pdf;

import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;

class PDFMergerTestUtil {

    private PDFMergerTestUtil() { }

    // Utils //
    static Bundle createFlatTestBundle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        bundle.setHasFolderCoversheets(false);

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("AAAAAAA");
        bundleDocument.setDocTitle("Bundle Doc 1");
        bundleDocument.setId(1L);
        bundle.getDocuments().add(bundleDocument);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocumentURI("BBBBBBB");
        bundleDocument2.setDocTitle("Bundle Doc 2");
        bundleDocument2.setId(1L);
        bundle.getDocuments().add(bundleDocument2);

        return bundle;
    }

    static int countSubstrings(String text, String find) {
        int index = 0;
        int count = 0;
        int length = find.length();
        while ((index = text.indexOf(find, index)) != -1) {
            index += length;
            count++;
        }
        return count;
    }

}
