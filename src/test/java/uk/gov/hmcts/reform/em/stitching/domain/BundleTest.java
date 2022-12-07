package uk.gov.hmcts.reform.em.stitching.domain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRendering;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class BundleTest {
    private static final String DEFAULT_DOCUMENT_ID = "/AAAAAAAAAA";

    private final ObjectMapper mapper = new ObjectMapper();
    private static final File FILE_1 = new File(
            ClassLoader.getSystemResource("Potential_Energy_PDF.pdf").getPath()
    );
    private static final File FILE_2 = new File(
            ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath()
    );
    private static final File FILE_3 = new File(
            ClassLoader.getSystemResource("bundle.json").getPath()
    );

    @Before
    public void setup() {
        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    public void serializesToJson() {
        Bundle bundle = BundleTest.getTestBundle();

        assertEquals("My bundle",bundle.getBundleTitle());
        assertEquals(DEFAULT_DOCUMENT_ID,bundle.getDocuments().get(0).getDocumentURI());
    }

    @Test
    public void getEmptyFileName() {
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setFileName(null);

        assertEquals(bundle.getFileName(), bundle.getBundleTitle());
    }

    @Test
    public void getPopulatedFileName() {
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setBundleTitle("Bundle Title");
        bundle.setFileName("fileName.pdf");

        assertNotEquals(bundle.getFileName(), bundle.getBundleTitle());
    }

    public static Bundle getTestBundle() {
        BundleDocument bundleDocument1 = new BundleDocument();
        bundleDocument1.setDocumentURI(DEFAULT_DOCUMENT_ID);
        bundleDocument1.setDocTitle("Document title");
        bundleDocument1.setDocDescription("Document description");
        BundleDocument bundleDocument2 = new BundleDocument();
        bundleDocument2.setDocumentURI(DEFAULT_DOCUMENT_ID);
        bundleDocument2.setDocTitle("Document title 2");
        bundleDocument2.setDocDescription("Document description 2");

        Bundle bundle = new Bundle();
        bundle.setBundleTitle("My bundle");
        bundle.setDescription("Bundle description");
        bundle.setCreatedDate(Instant.parse("2019-01-09T14:00:00Z"));
        bundle.setCreatedBy("Billy Bob");
        bundle.getDocuments().add(bundleDocument1);
        bundle.getDocuments().add(bundleDocument2);
        bundle.setFolders(new ArrayList<>());

        DocumentImage documentImage = new DocumentImage();
        documentImage.setDocmosisAssetId("schmcts.png");
        documentImage.setImageRendering(ImageRendering.opaque);
        documentImage.setImageRenderingLocation(ImageRenderingLocation.allPages);
        documentImage.setCoordinateX(50);
        documentImage.setCoordinateY(50);
        bundle.setDocumentImage(documentImage);

        return bundle;
    }

    /**
     * Create Bundle structure:
     * Bundle:
     *  - document1
     *  - folder1
     *    - folder1document1
     *    - folder1document2
     *  - folder2
     *    - folder2document1
     *    - folder3
     *      - folder3document1
     *  - document2
     * And expect the documents to be sorted as:
     * [document1, folder1document1, folder1document2, folder2document1, folder3document1, document2]
     * Note that in the test they are deliberately added out of order.
     */
    @Test
    public void sortsItems() {
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setDocuments(new ArrayList<>());
        bundle.setFolders(new ArrayList<>());
        BundleDocument document1 = getBundleDocument(1);

        BundleFolder folder1 = getBundleFolder(2);
        BundleDocument folder1document1 = getBundleDocument(1);
        BundleDocument folder1document2 = getBundleDocument(2);

        BundleFolder folder2 = getBundleFolder(3);
        BundleDocument folder2document1 = getBundleDocument(1);

        BundleFolder folder3 = getBundleFolder(2);
        BundleDocument folder3document1 = getBundleDocument(1);

        BundleDocument document2 = getBundleDocument(4);

        bundle.getDocuments().add(document2);
        bundle.getDocuments().add(document1);
        bundle.getFolders().add(folder1);
        bundle.getFolders().add(folder2);

        folder1.getDocuments().add(folder1document2);
        folder1.getDocuments().add(folder1document1);

        folder2.getFolders().add(folder3);
        folder2.getDocuments().add(folder2document1);

        folder3.getDocuments().add(folder3document1);

        List<BundleDocument> result = bundle.getSortedDocuments().collect(Collectors.toList());
        List<BundleDocument> expected = Stream.of(
            document1,
            folder1document1,
            folder1document2,
            folder2document1,
            folder3document1,
            document2
        ).collect(Collectors.toList());

        assertEquals(expected.size(), result.size());

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), result.get(i));
        }
    }

    @Test
    public void testRemovalOfEmptyFolders() {
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setDocuments(new ArrayList<>());
        bundle.setFolders(new ArrayList<>());
        bundle.getDocuments().add(getBundleDocument(1));

        BundleFolder folder1 = getBundleFolder(2);
        folder1.getDocuments().add(getBundleDocument(1));
        folder1.getDocuments().add(getBundleDocument(2));

        BundleFolder folder2 = getBundleFolder(3);
        BundleFolder folder3 = getBundleFolder(1);

        bundle.getFolders().add(folder1);
        bundle.getFolders().add(folder2);
        folder2.getFolders().add(folder3);

        final long result = bundle.getSortedItems().count();
        final int expected = 2; //one document, one folder

        assertEquals(expected, result);
    }

    @Test
    public void testGetNestedFolders() {
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setDocuments(new ArrayList<>());
        bundle.setFolders(new ArrayList<>());

        BundleFolder folder1 = getBundleFolder(2);
        folder1.getDocuments().add(getBundleDocument(1));
        folder1.getDocuments().add(getBundleDocument(2));

        BundleFolder folder2 = getBundleFolder(3);
        BundleFolder folder3 = getBundleFolder(1);

        bundle.getFolders().add(folder1);
        bundle.getFolders().add(folder2);
        folder2.getFolders().add(folder3);

        final long result = bundle.getNestedFolders().count();
        final int expected = 1; //folder 2 and folder 3 should be omitted

        assertEquals(expected, result);
    }


    @Test
    public void testGetNestedFolders_withSubFolder_has_doc() {
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setDocuments(new ArrayList<>());
        bundle.setFolders(new ArrayList<>());

        BundleFolder folder1 = getBundleFolder(2);
        folder1.getDocuments().add(getBundleDocument(1));
        folder1.getDocuments().add(getBundleDocument(2));

        BundleFolder folder2 = getBundleFolder(3);
        BundleFolder subFolder2 = getBundleFolder(1);

        bundle.getFolders().add(folder1);
        bundle.getFolders().add(folder2);
        folder2.getFolders().add(subFolder2);

        BundleFolder folder4 = getBundleFolder(4);
        bundle.getFolders().add(folder4);
        BundleFolder subFolder4 = getBundleFolder(1);
        folder4.getFolders().add(subFolder4);
        subFolder4.getDocuments().add(getBundleDocument(1));

        final long result = bundle.getNestedFolders().count();
        final int expected = 3; //folder 2 and subfolder 2 should be omitted

        assertEquals(expected, result);
    }

    @Test
    public void testGetNestedFolders_withFolder_has_doc() {
        Bundle bundle = BundleTest.getTestBundle();
        bundle.setDocuments(new ArrayList<>());
        bundle.setFolders(new ArrayList<>());

        BundleFolder folder1 = getBundleFolder(2);
        folder1.getDocuments().add(getBundleDocument(1));
        folder1.getDocuments().add(getBundleDocument(2));

        BundleFolder folder2 = getBundleFolder(3);
        BundleFolder subFolder2 = getBundleFolder(1);

        bundle.getFolders().add(folder1);
        bundle.getFolders().add(folder2);
        folder2.getFolders().add(subFolder2);

        BundleFolder folder4 = getBundleFolder(4);
        bundle.getFolders().add(folder4);
        folder4.getDocuments().add(getBundleDocument(1));

        BundleFolder subFolder4 = getBundleFolder(1);
        folder4.getFolders().add(subFolder4);

        final long result = bundle.getNestedFolders().count();
        final int expected = 2; //folder 2, subfolder 2, subfolder 4 should be omitted

        assertEquals(expected, result);
    }

    @Test
    public void getFileName() {
        Bundle bundle = new Bundle();
        assertNull(bundle.getFileName());
        bundle.setBundleTitle("x");
        assertEquals("x", bundle.getFileName());
        bundle.setFileName("y");
        assertEquals("y", bundle.getFileName());
    }

    @Test
    public void toStringTest() {
        Bundle bundle = new Bundle();
        String toString = bundle.toString();
        assertEquals("Bundle(id=null, bundleTitle=null, description=null, stitchedDocumentURI=null, stitchStatus=null, "
                + "fileName=null, hasTableOfContents=false, hasCoversheets=false, hasFolderCoversheets=false)", toString);
    }

    private static BundleDocument getBundleDocument(int index) {
        BundleDocument doc = new BundleDocument();
        doc.setSortIndex(index);

        return doc;
    }

    private static BundleFolder getBundleFolder(int index) {
        BundleFolder folder = new BundleFolder();
        folder.setDocuments(new ArrayList<>());
        folder.setFolders(new ArrayList<>());
        folder.setSortIndex(index);
        folder.setFolderName("Folder name");
        folder.setDescription("Folder description");

        return folder;
    }

    @Test
    public void testNumberOfSubtitlesInPDF() {
        Bundle bundle = getTestBundle();
        HashMap<BundleDocument, File>  documents = new HashMap<>();
        documents.put(bundle.getDocuments().get(0), FILE_1);
        documents.put(bundle.getDocuments().get(1), FILE_2);

        int numberOfSubtitle = bundle.getSubtitles(bundle,documents);

        assertEquals(8,numberOfSubtitle);
    }

    public static Bundle getTestBundleForFailure() throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Bundle bundle = mapper.readValue(FILE_3, Bundle.class);

        return bundle;

    }

}
