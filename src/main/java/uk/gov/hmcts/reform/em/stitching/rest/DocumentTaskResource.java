package uk.gov.hmcts.reform.em.stitching.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.lang.Collections;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.rest.errors.BadRequestAlertException;
import uk.gov.hmcts.reform.em.stitching.rest.util.HeaderUtil;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
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
     * @return the ResponseEntity with status 201 (Created) and with body the new documentTaskDTO,
     *          or with status 400 (Bad Request) if the documentTask has already an ID
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
    public ResponseEntity<DocumentTaskDTO> createDocumentTask(
            @Valid @RequestBody DocumentTaskDTO documentTaskDTO,
            @RequestHeader(value = "Authorization", required = false) String authorisationHeader,
            HttpServletRequest request) throws URISyntaxException, IOException {

        log.info("REST request to save DocumentTask : {}, with headers {}", documentTaskDTO.toString(),
                Arrays.toString(Collections.toArray(request.getHeaderNames(), new String[]{})));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String docTask = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(documentTaskDTO);
        mapper.readValue(docTask, DocumentTaskDTO.class);
        log.info("Received request body as Json \n {}", docTask);

        if (documentTaskDTO.getId() != null) {
            throw new BadRequestAlertException("A new documentTask cannot already have an ID", ENTITY_NAME, "id exists");
        }

        try {
            DocumentTaskDTO result = null;
            documentTaskDTO.setJwt(authorisationHeader);
            documentTaskDTO.setTaskState(TaskState.NEW);
            result = documentTaskService.save(documentTaskDTO);

            return ResponseEntity.created(new URI("/api/document-tasks/" + result.getId()))
                    .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
                    .body(result);

        } catch (DataIntegrityViolationException e) {

            log.error("Error while mapping entities for DocumentTask : {} , DocumentTask contains {}", e.getCause(), docTask);
            log.info("Details on error", e);
            return ResponseEntity.badRequest().body(null);

        } catch (Exception e) {
            log.error("Error while mapping entities for DocumentTask : {} , DocumentTask contains {}", e.getCause(), docTask);
            log.info("Details on error", e);
            return ResponseEntity.badRequest().body(null);
        }
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

}
