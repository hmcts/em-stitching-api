package uk.gov.hmcts.reform.em.stitching.testutil.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class CcdDocument implements Serializable {

    @JsonProperty("document_url")
    private String url;
    @JsonProperty("document_filename")
    private String fileName;
    @JsonProperty("document_binary_url")
    private String binaryUrl;
    @JsonProperty("document_hash")
    private String hash;

}
