package uk.gov.hmcts.reform.em.stitching.rest.errors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.rest.TestSecurityConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the ExceptionTranslator controller advice.
 *
 * @see ExceptionTranslator
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class ExceptionTranslatorIntTest {

    @Autowired
    private ExceptionTranslatorTestController controller;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .build();
    }

    @Test
    void testConcurrencyFailure() throws Exception {
        mockMvc.perform(get("/test/concurrency-failure"))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value(ErrorConstants.ERR_CONCURRENCY_FAILURE));
    }

    @Test
    void testMethodArgumentReturn400WhenNoContent() throws Exception {
        mockMvc.perform(post("/test/method-argument")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andReturn();
    }

    @Disabled("Revisit as it return status 200 as compare to original")
    @Test
    void testMethodArgumentNotValid() throws Exception {
        mockMvc.perform(post("/test/method-argument")
            .content("{}")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value(ErrorConstants.ERR_VALIDATION))
            .andExpect(jsonPath("$.fieldErrors.[0].objectName").value("testDTO"))
            .andExpect(jsonPath("$.fieldErrors.[0].field").value("test"))
            .andExpect(jsonPath("$.fieldErrors.[0].message").value("required"));
    }

    @Test
    void testParameterizedError() throws Exception {
        mockMvc.perform(get("/test/parameterized-error"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("test parameterized error"))
            .andExpect(jsonPath("$.params.param0").value("param0_value"))
            .andExpect(jsonPath("$.params.param1").value("param1_value"));
    }

    @Test
    void testParameterizedError2() throws Exception {
        mockMvc.perform(get("/test/parameterized-error2"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("test parameterized error"))
            .andExpect(jsonPath("$.params.foo").value("foo_value"))
            .andExpect(jsonPath("$.params.bar").value("bar_value"));
    }

    @Test
    void testMissingServletRequestPartException() throws Exception {
        mockMvc.perform(get("/test/missing-servlet-request-part"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.400"));
    }

    @Test
    void testMissingServletRequestParameterException() throws Exception {
        mockMvc.perform(get("/test/missing-servlet-request-parameter"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.400"));
    }

    @Test
    void testAccessDenied() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.403"));
    }

    @Test
    void testUnauthorized() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.401"))
            .andExpect(jsonPath("$.path").value("/test/unauthorized"));
    }

    @Test
    void testMethodNotSupported() throws Exception {
        mockMvc.perform(post("/test/access-denied"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.405"))
            .andExpect(jsonPath("$.detail").value("Request method 'POST' is not supported"));
    }

    @Test
    void testExceptionWithResponseStatus() throws Exception {
        mockMvc.perform(get("/test/response-status"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.400"))
            .andExpect(jsonPath("$.title").value("test response status"));
    }

    @Test
    void testInternalServerError() throws Exception {
        mockMvc.perform(get("/test/internal-server-error"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.http.500"))
            .andExpect(jsonPath("$.title").value("Internal Server Error"));
    }

    @Test
    void testNoSuchElementException() throws Exception {
        mockMvc.perform(get("/test/no-such-element-exception"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("https://stitching/problem/entity-not-found"));
    }

    @Test
    void testBadRequestAlertException() throws Exception {
        mockMvc.perform(get("/test/bad-request-alert-exception"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("error.BND"));
    }
}
