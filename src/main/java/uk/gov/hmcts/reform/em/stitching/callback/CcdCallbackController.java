package uk.gov.hmcts.reform.em.stitching.callback;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.em.stitching.service.callback.CcdCallbackHandlerService;
import uk.gov.hmcts.reform.em.stitching.service.callback.CcdCallbackDto;
import uk.gov.hmcts.reform.em.stitching.service.callback.impl.CcdBundleStitchingService;

@Controller
public class CcdCallbackController {

    private final Logger log = LoggerFactory.getLogger(CcdCallbackController.class);

    private CcdCallbackHandlerService ccdCallbackHandlerService;

    private CcdBundleStitchingService ccdBundleStitchingService;

    public CcdCallbackController(CcdCallbackHandlerService ccdCallbackHandlerService) {
        this.ccdCallbackHandlerService = ccdCallbackHandlerService;
    }

    @PostMapping(value = "/api/stitch-cdd-bundles",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectNode> stitchCcdBundles(
            @RequestBody CcdCallbackDto cmd,
            @RequestHeader(value="Authorization", required=false) String authorisationHeader)   {

        log.debug("CCD callback request received {}", cmd.getCaseData());

        cmd.setJwt(authorisationHeader);

        cmd.setPropertyName("caseBundles");

        ObjectNode updatedCaseData = ccdCallbackHandlerService.handleCddCallback(cmd, ccdBundleStitchingService);

        return ResponseEntity.ok(updatedCaseData);
    }

}
