package uk.gov.hmcts.reform.em.bundling.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.em.bundling.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.bundling.rest.errors.BadRequestAlertException;
import uk.gov.hmcts.reform.em.bundling.rest.util.HeaderUtil;
import uk.gov.hmcts.reform.em.bundling.rest.util.PaginationUtil;
import uk.gov.hmcts.reform.em.bundling.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.bundling.service.dto.DocumentTaskDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing DocumentTask.
 */
@RestController
@RequestMapping("/api")
public class DocumentTaskResource {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskResource.class);

    private static final String ENTITY_NAME = "documentTask";

    private final DocumentTaskService documentTaskService;

    public DocumentTaskResource(DocumentTaskService documentTaskService) {
        this.documentTaskService = documentTaskService;
    }

    /**
     * POST  /document-tasks : Create a new documentTask.
     *
     * @param documentTaskDTO the documentTaskDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new documentTaskDTO, or with status 400 (Bad Request) if the documentTask has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @ApiOperation(value = "Create a documentTaskDTO", notes = "A POST request to create a documentTaskDTO")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created", response = DocumentTaskDTO.class),
            @ApiResponse(code = 400, message = "documentTaskDTO not valid, invalid id"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
    })
    @PostMapping("/document-tasks")
    ////@Timed
    public ResponseEntity<DocumentTaskDTO> createDocumentTask(@RequestBody DocumentTaskDTO documentTaskDTO, @RequestHeader(value="Authorization", required=false) String authorisationHeader) throws URISyntaxException {
        log.debug("REST request to save DocumentTask : {}", documentTaskDTO);
        if (documentTaskDTO.getId() != null) {
            throw new BadRequestAlertException("A new documentTask cannot already have an ID", ENTITY_NAME, "idexists");
        }
        documentTaskDTO.setJwt(authorisationHeader);
        documentTaskDTO.setTaskState(TaskState.NEW);
        DocumentTaskDTO result = documentTaskService.save(documentTaskDTO);

        return ResponseEntity.created(new URI("/api/document-tasks/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /document-tasks : Updates an existing documentTask.
     *
     * @param documentTaskDTO the documentTaskDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated documentTaskDTO,
     * or with status 400 (Bad Request) if the documentTaskDTO is not valid,
     * or with status 500 (Internal Server Error) if the documentTaskDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @ApiOperation(value = "Update an existing documentTaskDTO", notes = "A PUT request to update a documentTaskDTO")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = DocumentTaskDTO.class),
            @ApiResponse(code = 400, message = "documentTaskDTO not valid"),
            @ApiResponse(code = 500, message = "documentTaskDTO couldn't be updated"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
    })
    @PutMapping("/document-tasks")
    //@Timed
    public ResponseEntity<DocumentTaskDTO> updateDocumentTask(@RequestBody DocumentTaskDTO documentTaskDTO, @RequestHeader(value="Authorization", required=false) String authorisationHeader) throws URISyntaxException {
        log.debug("REST request to update DocumentTask : {}", documentTaskDTO);
        if (documentTaskDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        documentTaskDTO.setJwt(authorisationHeader);
        DocumentTaskDTO result = documentTaskService.save(documentTaskDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, documentTaskDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /document-tasks : get all the documentTasks.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of documentTasks in body
     */
    @ApiOperation(value = "Get all documentTaskDTOs", notes = "A GET request without a body is used to retrieve all documentTaskDTOs")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = DocumentTaskDTO.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
    })
    @GetMapping("/document-tasks")
    //@Timed
    public ResponseEntity<List<DocumentTaskDTO>> getAllDocumentTasks(Pageable pageable) {
        log.debug("REST request to get a page of DocumentTasks");
        Page<DocumentTaskDTO> page = documentTaskService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/document-tasks");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /document-tasks/:id : get the "id" documentTask.
     *
     * @param id the id of the documentTaskDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the documentTaskDTO, or with status 404 (Not Found)
     */
    @ApiOperation(value = "Get an existing documentTaskDTO", notes = "A GET request to retrieve a documentTaskDTO")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = DocumentTaskDTO.class),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
    })
    @GetMapping("/document-tasks/{id}")
    //@Timed
    public ResponseEntity<DocumentTaskDTO> getDocumentTask(@PathVariable Long id) {
        log.debug("REST request to get DocumentTask : {}", id);
        Optional<DocumentTaskDTO> documentTaskDTO = documentTaskService.findOne(id);
        return ResponseUtil.wrapOrNotFound(documentTaskDTO);
    }

    /**
     * DELETE  /document-tasks/:id : delete the "id" documentTask.
     *
     * @param id the id of the documentTaskDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @ApiOperation(value = "Delete a documentTaskDTO", notes = "A DELETE request to delete a documentTaskDTO")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
    })
    @DeleteMapping("/document-tasks/{id}")
    //@Timed
    public ResponseEntity<Void> deleteDocumentTask(@PathVariable Long id) {
        log.debug("REST request to delete DocumentTask : {}", id);
        documentTaskService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
