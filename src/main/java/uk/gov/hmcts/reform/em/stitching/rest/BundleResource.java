package uk.gov.hmcts.reform.em.stitching.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.stitching.service.BundleService;
import uk.gov.hmcts.reform.em.stitching.service.dto.StitchedDocumentDto;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;

import java.util.Optional;

/**
 * REST controller for managing Bundle.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BundleResource {

    private final Logger log = LoggerFactory.getLogger(BundleResource.class);

    private final BundleService bundleService;

    /**
     * GET  /bundle/:id : get the "id" bundle.
     *
     * @param id the id of the documentTaskDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the bundleResponseDto, or with status 404 (Not Found)
     */
    @ApiOperation(value = "Get an existing bundleResponseDto", notes = "A GET request to retrieve a bundleResponseDto")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success", response = DocumentTaskDTO.class),
        @ApiResponse(code = 401, message = "Unauthorised"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "Not Found"),
    })
    @GetMapping("/bundle/{id}/stitched-document")
    //@Timed
    public ResponseEntity<StitchedDocumentDto> getBundle(@PathVariable Long id) {
        log.debug("REST request to get Bundle : {}", id);
        Optional<StitchedDocumentDto> bundleResponseDto = bundleService.findOne(id);
        return ResponseUtil.wrapOrNotFound(bundleResponseDto);
    }
}
