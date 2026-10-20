package uk.gov.hmcts.reform.em.stitching.service.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentImage;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRendering;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden Jackson 3 round trips for MVC/jsonb-backed bundle payloads.
 * Guards silent field loss from ignored Jackson 2 databind annotations
 * and coverpageTemplateData Map persistence shape.
 */
class BundleDtoJackson3IntegrityTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String BUNDLE_JSON = """
            {
              "bundleTitle": "Integrity bundle",
              "description": "Round-trip proof",
              "coverpageTemplate": "FL-FRM-GOR-ENG-12345",
              "coverpageTemplateData": {
                "caseNo": "12345",
                "nested": { "court": "Family" }
              },
              "documentImage": {
                "docmosisAssetId": "hmcts.png",
                "imageRenderingLocation": "allPages",
                "coordinateX": 40,
                "coordinateY": 60,
                "imageRendering": "opaque"
              },
              "hasTableOfContents": true,
              "hasCoversheets": true,
              "unknownFutureField": "must-be-ignored-or-accepted"
            }
            """;

    @Test
    void jackson3RoundTripsCoverpageMapAndDocumentImageEnums() {
        BundleDTO dto = MAPPER.readValue(BUNDLE_JSON, BundleDTO.class);

        assertEquals("Integrity bundle", dto.getBundleTitle());
        assertEquals("FL-FRM-GOR-ENG-12345", dto.getCoverpageTemplate());
        assertEquals("12345", dto.getCoverpageTemplateData().get("caseNo"));
        assertNotNull(dto.getCoverpageTemplateData().get("nested"));

        DocumentImage image = dto.getDocumentImage();
        assertEquals("hmcts.png", image.getDocmosisAssetId());
        assertEquals(ImageRendering.OPAQUE, image.getImageRendering());
        assertEquals(ImageRenderingLocation.ALL_PAGES, image.getImageRenderingLocation());
        assertEquals(40, image.getCoordinateX());
        assertEquals(60, image.getCoordinateY());

        String written = MAPPER.writeValueAsString(dto);
        assertTrue(written.contains("\"caseNo\":\"12345\""));
        assertTrue(written.contains("\"imageRendering\":\"opaque\""));
        assertTrue(written.contains("\"imageRenderingLocation\":\"allPages\""));
        assertFalse(written.contains("\"imageRendering\":\"OPAQUE\""));
        assertFalse(written.contains("\"imageRenderingLocation\":\"ALL_PAGES\""));

        BundleDTO again = MAPPER.readValue(written, BundleDTO.class);
        assertEquals(dto.getCoverpageTemplateData().get("caseNo"),
            again.getCoverpageTemplateData().get("caseNo"));
        assertEquals(dto.getDocumentImage().getImageRendering(),
            again.getDocumentImage().getImageRendering());
    }

    @Test
    void coverpageTemplateDataAcceptsObjectShapeUsedByClients() {
        BundleDTO dto = new BundleDTO();
        dto.setCoverpageTemplateData(Map.of("caseNo", "ABC-99"));

        String json = MAPPER.writeValueAsString(dto);
        assertTrue(json.contains("\"coverpageTemplateData\":{\"caseNo\":\"ABC-99\"}"));

        BundleDTO read = MAPPER.readValue(json, BundleDTO.class);
        assertEquals("ABC-99", read.getCoverpageTemplateData().get("caseNo"));
    }
}
