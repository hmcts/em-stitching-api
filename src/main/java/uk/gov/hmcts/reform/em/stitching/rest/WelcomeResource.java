package uk.gov.hmcts.reform.em.stitching.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class WelcomeResource {

    private final Logger log = LoggerFactory.getLogger(WelcomeResource.class);

    private static final String MESSAGE = "Welcome to Stitching API!";

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping(
        path = "/",
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<Map<String,String>> welcome() {

        log.info("Welcome message : '{}'", MESSAGE);

        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noCache())
                .body(Map.of("message",MESSAGE));
    }
}