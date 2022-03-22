package uk.gov.hmcts.reform.em.stitching.pdf;

import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleFolder;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

final class PDFMergerTestUtil {

    private PDFMergerTestUtil() { }

    // Utils //
    static Bundle createFlatTestBundle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        bundle.setHasFolderCoversheets(false);
        bundle.setPaginationStyle(PaginationStyle.topLeft);

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

    static Bundle createFlatTestBundleWithMultilineTitles() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        bundle.setHasFolderCoversheets(false);
        bundle.setPaginationStyle(PaginationStyle.topLeft);

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("AAAAAAA");
        bundleDocument.setDocTitle("Bundle Doc 1 Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long");
        bundleDocument.setId(1L);
        bundle.getDocuments().add(bundleDocument);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocumentURI("BBBBBBB");
        bundleDocument2.setDocTitle("Bundle Doc 2 Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long Very long Very long Very long Very long Very long Very long Very long Very long"
                + " Very long");
        bundleDocument2.setId(1L);
        bundle.getDocuments().add(bundleDocument2);

        return bundle;
    }

    static Bundle createFolderedTestBundle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(true);
        bundle.setHasFolderCoversheets(true);
        bundle.setPaginationStyle(PaginationStyle.off);

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("AAAAAAA");
        bundleDocument.setDocTitle("Bundle Doc 1");
        bundleDocument.setId(1L);
        bundleDocument.setSortIndex(1);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocumentURI("BBBBBBB");
        bundleDocument2.setDocTitle("Bundle Doc 2");
        bundleDocument2.setId(2L);
        bundleDocument2.setSortIndex(2);

        BundleFolder folder = new BundleFolder();
        folder.setFolderName("Folder 1");
        folder.setDescription("This is folder 1");
        folder.setSortIndex(1);

        folder.getDocuments().add(bundleDocument);
        bundle.getFolders().add(folder);
        bundle.getDocuments().add(bundleDocument2);

        return bundle;
    }

    static Bundle createMultiFolderedTestBundle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        bundle.setHasFolderCoversheets(true);
        bundle.setPaginationStyle(PaginationStyle.topRight);

        BundleDocument bundleDocument1 = new BundleDocument();
        bundleDocument1.setDocumentURI("AAAAAAA");
        bundleDocument1.setDocTitle("Bundle Doc 1");
        bundleDocument1.setId(1L);
        bundleDocument1.setSortIndex(1);

        BundleFolder folder1 = new BundleFolder();
        folder1.setFolderName("Folder 1");
        folder1.setDescription("The first folder description - this is for folder 1");
        folder1.setSortIndex(1);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocumentURI("BBBBBBB");
        bundleDocument2.setDocTitle("A separate description - this one is of folder 2");
        bundleDocument2.setId(2L);
        bundleDocument2.setSortIndex(2);

        BundleFolder folder2 = new BundleFolder();
        folder2.setFolderName("Folder 2");
        folder2.setDescription("This is folder 2");
        folder2.setSortIndex(2);

        folder1.getDocuments().add(bundleDocument1);
        folder2.getDocuments().add(bundleDocument2);
        bundle.getFolders().add(folder1);
        bundle.getFolders().add(folder2);
        return bundle;
    }

    static Bundle createSubFolderedTestBundle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        bundle.setHasFolderCoversheets(true);
        bundle.setPaginationStyle(PaginationStyle.bottomLeft);

        BundleFolder folder1 = new BundleFolder();
        folder1.setFolderName("Folder 1");
        folder1.setDescription("The is a top level folder, Folder 1");
        folder1.setSortIndex(1);

        BundleDocument bundleDocument1 = new BundleDocument();
        bundleDocument1.setDocumentURI("AAAAAAA");
        bundleDocument1.setDocTitle("This is a doc inside a folder");
        bundleDocument1.setId(1L);
        bundleDocument1.setSortIndex(1);

        BundleFolder subfolder1 = new BundleFolder();
        subfolder1.setFolderName("Folder 2");
        subfolder1.setDescription("This is a subfolder, Folder 2");
        subfolder1.setSortIndex(2);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocumentURI("BBBBBBB");
        bundleDocument2.setDocTitle("This is a doc inside a subfolder");
        bundleDocument2.setId(2L);
        bundleDocument2.setSortIndex(1);

        subfolder1.getDocuments().add(bundleDocument2);
        folder1.getFolders().add(subfolder1);
        folder1.getDocuments().add(bundleDocument1);
        bundle.getFolders().add(folder1);
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

    static Bundle createFlatTestBundleWithAdditionalDoc() {

        BundleDocument bundleDocument3 = new BundleDocument();
        bundleDocument3.setDocumentURI("CCCCCCC");
        bundleDocument3.setDocTitle("Bundle Doc 3");
        bundleDocument3.setId(1L);

        Bundle bundle = createFlatTestBundle();
        bundle.getDocuments().add(bundleDocument3);

        return bundle;
    }

    static Bundle createFlatTestBundleWithMultilineDocumentTitlesWithAdditionalDoc() {

        BundleDocument bundleDocument3 = new BundleDocument();
        bundleDocument3.setDocumentURI("CCCCCCC");
        bundleDocument3.setDocTitle("Bundle Doc 3");
        bundleDocument3.setId(1L);

        Bundle bundle = createFlatTestBundleWithMultilineTitles();
        bundle.getDocuments().add(bundleDocument3);

        return bundle;
    }

    static Bundle createFlatTestBundleWithSameDocNameAsSubtitle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle");
        bundle.setDescription("This is the description, it should really be wrapped but it is not currently. The table limit is 255 characters anyway.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        bundle.setHasFolderCoversheets(false);
        bundle.setPaginationStyle(PaginationStyle.topLeft);

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("AAAAAAA");
        bundleDocument.setDocTitle("Slide 1");
        bundleDocument.setId(1L);
        bundle.getDocuments().add(bundleDocument);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocumentURI("BBBBBBB");
        bundleDocument2.setDocTitle("Bundle Doc 2");
        bundleDocument2.setId(1L);
        bundle.getDocuments().add(bundleDocument2);

        return bundle;
    }

    static Bundle createFlatTestBundleWithSpecialChars() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("ąćęłńóśźż");
        bundle.setDescription("This is the description, it should be wrapped now. The table limit is 1000 characters.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        bundle.setHasFolderCoversheets(false);
        bundle.setPaginationStyle(PaginationStyle.topLeft);

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("AAAAAAA");
        bundleDocument.setDocTitle("ąćęłńóśźż");
        bundleDocument.setId(1L);
        bundle.getDocuments().add(bundleDocument);

        return bundle;
    }

    static Bundle createFlatTestBundleWithLongDocTitle() {
        Bundle bundle = new Bundle();
        bundle.setBundleTitle("Title of the bundle : Bundle containing Long Doc Title");
        bundle.setDescription("This is the description.");
        bundle.setHasTableOfContents(true);
        bundle.setHasCoversheets(false);
        bundle.setHasFolderCoversheets(false);

        BundleDocument bundleDocument = new BundleDocument();
        bundleDocument.setDocumentURI("AAAAAAA");
        String docTitle = Stream.generate(() -> "DocName").limit(40).collect(Collectors.joining());
        bundleDocument.setDocTitle(docTitle);
        bundleDocument.setId(1L);
        bundle.getDocuments().add(bundleDocument);

        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocumentURI("BBBBBBB");
        bundleDocument2.setDocTitle("Bundle Doc 2");
        bundleDocument2.setId(1L);
        bundle.getDocuments().add(bundleDocument2);

        return bundle;
    }
}
