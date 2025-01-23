package uk.gov.hmcts.reform.em.stitching.rest;

import okhttp3.OkHttpClient;
import okhttp3.mock.MockInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;
import uk.gov.hmcts.reform.em.stitching.rest.errors.ExceptionTranslator;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.em.stitching.rest.TestUtil.createFormattingConversionService;

/**
 * Test class for the DocumentTaskResource REST controller.
 *
 * @see DocumentTaskResource
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class DocumentTaskResourceIntTest {

    private static final TaskState DEFAULT_TASK_STATE = TaskState.NEW;

    @Autowired
    private DocumentTaskRepository documentTaskRepository;

    @Autowired
    private DocumentTaskMapper documentTaskMapper;
    
    @Autowired
    private DocumentTaskService documentTaskService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private OkHttpClient okHttpClient;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private SubjectResolver<User> userResolver;

    private MockMvc restDocumentTaskMockMvc;

    private DocumentTask defaultTestDocumentTask;

    private Bundle testBundle;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        final DocumentTaskResource documentTaskResource = new DocumentTaskResource(documentTaskService);
        this.restDocumentTaskMockMvc = MockMvcBuilders.standaloneSetup(documentTaskResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();


    }

    public DocumentTask createEntity() {
        testBundle = BundleTest.getTestBundle();
        DocumentTask documentTask = new DocumentTask()
            .bundle(testBundle)
            .taskState(DEFAULT_TASK_STATE);
        documentTask.setJwt("userjwt");
        return documentTask;
    }

    @BeforeEach
    public void initTest() {
        defaultTestDocumentTask = createEntity();
        MockInterceptor mockInterceptor = (MockInterceptor)okHttpClient.interceptors().get(0);
        mockInterceptor.reset();
    }

    @Test
    void createDocumentTask() throws Exception {
        BDDMockito.given(authTokenGenerator.generate()).willReturn("s2s");
        BDDMockito
                .given(userResolver.getTokenDetails(defaultTestDocumentTask.getJwt()))
                .willReturn(new User("id", null));

        int databaseSizeBeforeCreate = documentTaskRepository.findAll().size();

        // Create the DocumentTask
        DocumentTaskDTO documentTaskDTO = documentTaskMapper.toDto(defaultTestDocumentTask);
        documentTaskDTO.getBundle().setStitchedDocumentURI(null);

        restDocumentTaskMockMvc.perform(post("/api/document-tasks")
            .header("Authorization", defaultTestDocumentTask.getJwt())
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(documentTaskDTO)))
            .andExpect(status().isCreated());

        // Validate the DocumentTask in the database
        List<DocumentTask> documentTaskList = documentTaskRepository.findAll();
        assertThat(documentTaskList).hasSize(databaseSizeBeforeCreate + 1);
        DocumentTask testDocumentTask = documentTaskList.get(documentTaskList.size() - 1);
        assertThat(testDocumentTask.getBundle().getDescription()).isEqualTo(testBundle.getDescription());
        assertThat(testDocumentTask.getTaskState()).isEqualTo(TaskState.NEW);
    }

    @Test
    void createDocumentTaskWithCaseId() throws Exception {
        BDDMockito.given(authTokenGenerator.generate()).willReturn("s2s");
        BDDMockito
                .given(userResolver.getTokenDetails(defaultTestDocumentTask.getJwt()))
                .willReturn(new User("id", null));

        // Create the DocumentTask
        DocumentTaskDTO documentTaskDTO = documentTaskMapper.toDto(createEntity());
        documentTaskDTO.getBundle().setStitchedDocumentURI(null);
        String testCaseId = "testCaseId999";
        documentTaskDTO.setCaseId(testCaseId);

        int databaseSizeBeforeCreate = documentTaskRepository.findAll().size();

        restDocumentTaskMockMvc.perform(post("/api/document-tasks")
                        .header("Authorization", defaultTestDocumentTask.getJwt())
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(documentTaskDTO)))
                .andExpect(status().isCreated());

        // Validate the DocumentTask in the database
        List<DocumentTask> documentTaskList = documentTaskRepository.findAll();
        assertThat(documentTaskList).hasSize(databaseSizeBeforeCreate + 1);
        DocumentTask testDocumentTask = documentTaskList.get(documentTaskList.size() - 1);
        assertThat(testDocumentTask.getBundle().getDescription()).isEqualTo(testBundle.getDescription());
        assertThat(testDocumentTask.getTaskState()).isEqualTo(TaskState.NEW);
        assertThat(testDocumentTask.getCaseId()).isEqualTo(testCaseId);
    }

    @Test
    void createDocumentTaskWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = documentTaskRepository.findAll().size();

        // Create the DocumentTask with an existing ID
        defaultTestDocumentTask.setId(1L);
        DocumentTaskDTO documentTaskDTO = documentTaskMapper.toDto(defaultTestDocumentTask);

        // An entity with an existing ID cannot be created, so this API call must fail
        restDocumentTaskMockMvc.perform(post("/api/document-tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(documentTaskDTO)))
            .andExpect(status().isBadRequest());

        // Validate the DocumentTask in the database
        List<DocumentTask> documentTaskList = documentTaskRepository.findAll();
        assertThat(documentTaskList).hasSize(databaseSizeBeforeCreate);
    }
    
    @Test
    void getDocumentTask() throws Exception {
        // Initialize the database
        documentTaskRepository.saveAndFlush(defaultTestDocumentTask);

        // Get the documentTask
        restDocumentTaskMockMvc.perform(get("/api/document-tasks/{id}", defaultTestDocumentTask.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(defaultTestDocumentTask.getId().intValue()))
            .andExpect(jsonPath("$.bundle.description").value(defaultTestDocumentTask.getBundle().getDescription()))
            .andExpect(jsonPath("$.taskState").value(DEFAULT_TASK_STATE.toString()));
    }

    @Test
    void getNonExistingDocumentTask() throws Exception {
        // Get the documentTask
        restDocumentTaskMockMvc.perform(get("/api/document-tasks/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DocumentTask.class);
        DocumentTask documentTask1 = new DocumentTask();
        documentTask1.setId(1L);
        DocumentTask documentTask2 = new DocumentTask();
        documentTask2.setId(documentTask1.getId());
        assertThat(documentTask1).isEqualTo(documentTask2);
        documentTask2.setId(2L);
        assertThat(documentTask1).isNotEqualTo(documentTask2);
        documentTask1.setId(null);
        assertThat(documentTask1).isNotEqualTo(documentTask2);
    }

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DocumentTaskDTO.class);
        DocumentTaskDTO documentTaskDto1 = new DocumentTaskDTO();
        documentTaskDto1.setId(1L);
        DocumentTaskDTO documentTaskDto2 = new DocumentTaskDTO();
        assertThat(documentTaskDto1).isNotEqualTo(documentTaskDto2);
        documentTaskDto2.setId(documentTaskDto1.getId());
        assertThat(documentTaskDto1).isEqualTo(documentTaskDto2);
        documentTaskDto2.setId(2L);
        assertThat(documentTaskDto1).isNotEqualTo(documentTaskDto2);
        documentTaskDto1.setId(null);
        assertThat(documentTaskDto1).isNotEqualTo(documentTaskDto2);
    }

    @Test
    void createDocumentTaskWithIncorrectRequest() throws Exception {
        DocumentTask documentTask = createEntityWithFailingFeature();
        BDDMockito.given(authTokenGenerator.generate()).willReturn("s2s");
        BDDMockito.given(userResolver.getTokenDetails(documentTask.getJwt()))
                .willReturn(new User("id", null));

        DocumentTaskDTO documentTaskDTO = documentTaskMapper.toDto(documentTask);
        documentTaskDTO.getBundle().setStitchedDocumentURI(null);

        restDocumentTaskMockMvc.perform(post("/api/document-tasks")
                .header("Authorization", documentTask.getJwt())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(documentTaskDTO)))
                .andExpect(status().isBadRequest());
    }

    public DocumentTask createEntityWithFailingFeature() throws IOException {
        testBundle = BundleTest.getTestBundleForFailure();
        DocumentTask documentTask = new DocumentTask()
                .bundle(testBundle)
                .taskState(DEFAULT_TASK_STATE);
        documentTask.setJwt("userjwt");
        return documentTask;
    }

}
