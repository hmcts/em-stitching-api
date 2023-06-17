package uk.gov.hmcts.reform.em.stitching.rest;

import io.jsonwebtoken.lang.Collections;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.rest.errors.BadRequestAlertException;
import uk.gov.hmcts.reform.em.stitching.rest.util.HeaderUtil;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * REST controller for managing DocumentTask.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "DocumentTasks Service", description = "Endpoint for managing DocumentTasks.")
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
    @Operation(summary = "Create a documentTaskDTO", description = "A POST request to create a documentTaskDTO",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                            description = "Service Authorization (S2S Bearer token)", required = true,
                            schema = @Schema(type = "string"))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "documentTaskDTO not valid, invalid id"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
    })
    @PostMapping("/document-tasks")
    public ResponseEntity<DocumentTaskDTO> createDocumentTask(
            @Valid @RequestBody DocumentTaskDTO documentTaskDTO,
            @RequestHeader(value = "Authorization") String authorisationHeader,
            HttpServletRequest request) throws URISyntaxException, DocumentTaskProcessingException {

        log.debug("REST request to save DocumentTask : {}, with headers {}", documentTaskDTO.toString(),
                Arrays.toString(Collections.toArray(request.getHeaderNames(), new String[]{})));

        if (Objects.nonNull(documentTaskDTO.getId())) {
            throw new BadRequestAlertException("A new documentTask cannot already have an ID", ENTITY_NAME, "id exists");
        }

        try {
            documentTaskDTO.setJwt(authorisationHeader);
            documentTaskDTO.setTaskState(TaskState.NEW);
            DocumentTaskDTO result = documentTaskService.save(documentTaskDTO);

            return ResponseEntity.created(new URI("/api/document-tasks/" + result.getId()))
                    .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
                    .body(result);

        } catch (RuntimeException e) {
            final Optional<Throwable> rootCause = Stream.iterate(e, Throwable::getCause)
                    .filter(excep -> excep.getCause() == null).findFirst();

            if (rootCause.isPresent() && rootCause.get() instanceof ConstraintViolationException) {
                ConstraintViolationException constraintViolationException = (ConstraintViolationException) rootCause.get();
                Optional<ConstraintViolation<?>> violationExc = constraintViolationException.getConstraintViolations().stream()
                                                .findFirst();
                String violationMsg = violationExc.isPresent() ? violationExc.get().getMessage() : "Missing ConstraintViolationException Msg";
                throw new DocumentTaskProcessingException("Error saving Document Task : "
                    + e + " Caused by ConstraintViolationException :  " + violationMsg);
            }

            throw new DocumentTaskProcessingException("Error saving Document Task : "
                    + e + " Caused by " + (rootCause.isPresent() ? rootCause.get() : Optional.empty()), e);

        }
    }

    /**
     * GET  /document-tasks/:id : get the "id" documentTask.
     *
     * @param id the id of the documentTaskDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the documentTaskDTO, or with status 404 (Not Found)
     */
    @Operation(summary = "Get an existing documentTaskDTO", description = "A GET request to retrieve a documentTaskDTO",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "authorization",
                            description = "Authorization (Idam Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                            description = "Service Authorization (S2S Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.PATH, name = "id",
                            description = "DocumentTask Id", required = true,
                            schema = @Schema(type = "Long"))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
    })
    @GetMapping("/document-tasks/{id}")
    public ResponseEntity<DocumentTaskDTO> getDocumentTask(@PathVariable Long id) {
        log.debug("REST request to get DocumentTask : {}", id);
        Optional<DocumentTaskDTO> documentTaskDTO = documentTaskService.findOne(id);
        return ResponseUtil.wrapOrNotFound(documentTaskDTO);
    }

}
