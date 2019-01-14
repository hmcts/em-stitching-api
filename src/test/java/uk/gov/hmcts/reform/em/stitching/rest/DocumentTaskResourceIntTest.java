package uk.gov.hmcts.reform.em.stitching.rest;

import okhttp3.OkHttpClient;
import okhttp3.mock.MockInterceptor;
import okhttp3.mock.Rule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;
import uk.gov.hmcts.reform.em.stitching.rest.errors.ExceptionTranslator;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.hmcts.reform.em.stitching.rest.TestUtil.createFormattingConversionService;

/**
 * Test class for the DocumentTaskResource REST controller.
 *
 * @see DocumentTaskResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DocumentTaskResourceIntTest {

    private static final String DEFAULT_OUTPUT_DOCUMENT_ID = "AAAAAAAAAA";
    private static final String UPDATED_OUTPUT_DOCUMENT_ID = "BBBBBBBBBB";

    private static final TaskState DEFAULT_TASK_STATE = TaskState.NEW;
    private static final TaskState UPDATED_TASK_STATE = TaskState.IN_PROGRESS;

    private static final String DEFAULT_FAILURE_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_FAILURE_DESCRIPTION = "BBBBBBBBBB";

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

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private SubjectResolver<User> userResolver;

    @Autowired
    private EntityManager em;

    @Value("${dm-store-app.base-url}")
    private String dmBaseUrl;

    @Value("${em-rpa-stitching-api.base-url}")
    private String emStitchingAppBaseUrl;

    private MockMvc restDocumentTaskMockMvc;

    private DocumentTask documentTask;

    private Bundle testBundle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final DocumentTaskResource documentTaskResource = new DocumentTaskResource(documentTaskService);
        this.restDocumentTaskMockMvc = MockMvcBuilders.standaloneSetup(documentTaskResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();


    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public DocumentTask createEntity(EntityManager em) {
        testBundle = BundleTest.getTestBundle();
        DocumentTask documentTask = new DocumentTask()
            .bundle(testBundle)
            .outputDocumentId(DEFAULT_OUTPUT_DOCUMENT_ID)
            .taskState(DEFAULT_TASK_STATE)
            .failureDescription(DEFAULT_FAILURE_DESCRIPTION);
        documentTask.setJwt("userjwt");
        return documentTask;
    }

    @Before
    public void initTest() {
        documentTask = createEntity(em);
        MockInterceptor mockInterceptor = (MockInterceptor)okHttpClient.interceptors().get(0);
        mockInterceptor.reset();
    }

    @Test
    @Transactional
    public void createDocumentTask() throws Exception {
        BDDMockito.given(authTokenGenerator.generate()).willReturn("s2s");
        BDDMockito.given(userResolver.getTokenDetails(documentTask.getJwt())).willReturn(new User("id", null));

        int databaseSizeBeforeCreate = documentTaskRepository.findAll().size();

        // Create the DocumentTask
        DocumentTaskDTO documentTaskDTO = documentTaskMapper.toDto(documentTask);
        documentTaskDTO.setOutputDocumentId(null);

        restDocumentTaskMockMvc.perform(post("/api/document-tasks")
            .header("Authorization", documentTask.getJwt())
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(documentTaskDTO)))
            .andExpect(status().isCreated());

        // Validate the DocumentTask in the database
        List<DocumentTask> documentTaskList = documentTaskRepository.findAll();
        assertThat(documentTaskList).hasSize(databaseSizeBeforeCreate + 1);
        DocumentTask testDocumentTask = documentTaskList.get(documentTaskList.size() - 1);
        assertThat(testDocumentTask.getBundle().getDescription()).isEqualTo(testBundle.getDescription());
        assertThat(testDocumentTask.getTaskState()).isEqualTo(TaskState.NEW);
        assertThat(testDocumentTask.getFailureDescription()).isEqualTo(DEFAULT_FAILURE_DESCRIPTION);
    }

    @Test
    @Transactional
    public void createDocumentTaskWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = documentTaskRepository.findAll().size();

        // Create the DocumentTask with an existing ID
        documentTask.setId(1L);
        DocumentTaskDTO documentTaskDTO = documentTaskMapper.toDto(documentTask);

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
    @Transactional
    public void getAllDocumentTasks() throws Exception {
        // Initialize the database
        documentTaskRepository.saveAndFlush(documentTask);

        // Get all the documentTaskList
        restDocumentTaskMockMvc.perform(get("/api/document-tasks?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(documentTask.getId().intValue())))
            .andExpect(jsonPath("$.[*].bundle.description").value(hasItem(documentTask.getBundle().getDescription())))
            .andExpect(jsonPath("$.[*].bundle.version").value(hasItem(documentTask.getBundle().getVersion())))
            .andExpect(jsonPath("$.[*].outputDocumentId").value(hasItem(DEFAULT_OUTPUT_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].taskState").value(hasItem(DEFAULT_TASK_STATE.toString())))
            .andExpect(jsonPath("$.[*].failureDescription").value(hasItem(DEFAULT_FAILURE_DESCRIPTION.toString())));
    }
    
    @Test
    @Transactional
    public void getDocumentTask() throws Exception {
        // Initialize the database
        documentTaskRepository.saveAndFlush(documentTask);

        // Get the documentTask
        restDocumentTaskMockMvc.perform(get("/api/document-tasks/{id}", documentTask.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(documentTask.getId().intValue()))
            .andExpect(jsonPath("$.bundle.description").value(documentTask.getBundle().getDescription()))
            .andExpect(jsonPath("$.outputDocumentId").value(DEFAULT_OUTPUT_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.taskState").value(DEFAULT_TASK_STATE.toString()))
            .andExpect(jsonPath("$.failureDescription").value(DEFAULT_FAILURE_DESCRIPTION.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingDocumentTask() throws Exception {
        // Get the documentTask
        restDocumentTaskMockMvc.perform(get("/api/document-tasks/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateDocumentTask() throws Exception {
        // Initialize the database
        documentTaskRepository.saveAndFlush(documentTask);

        int databaseSizeBeforeUpdate = documentTaskRepository.findAll().size();

        // Update the documentTask
        DocumentTask updatedDocumentTask = documentTaskRepository.findById(documentTask.getId()).get();
        // Disconnect from session so that the updates on updatedDocumentTask are not directly saved in db
        em.detach(updatedDocumentTask);
        updatedDocumentTask
            .bundle(testBundle)
            .outputDocumentId(UPDATED_OUTPUT_DOCUMENT_ID)
            .taskState(UPDATED_TASK_STATE)
            .failureDescription(UPDATED_FAILURE_DESCRIPTION);
        DocumentTaskDTO documentTaskDTO = documentTaskMapper.toDto(updatedDocumentTask);

        restDocumentTaskMockMvc.perform(put("/api/document-tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(documentTaskDTO)))
            .andExpect(status().isOk());

        // Validate the DocumentTask in the database
        List<DocumentTask> documentTaskList = documentTaskRepository.findAll();
        assertThat(documentTaskList).hasSize(databaseSizeBeforeUpdate);
        DocumentTask testDocumentTask = documentTaskList.get(documentTaskList.size() - 1);
        assertThat(testDocumentTask.getBundle().getDescription()).isEqualTo(testBundle.getDescription());
        assertThat(testDocumentTask.getOutputDocumentId()).isEqualTo(UPDATED_OUTPUT_DOCUMENT_ID);
        assertThat(testDocumentTask.getTaskState()).isEqualTo(UPDATED_TASK_STATE);
        assertThat(testDocumentTask.getFailureDescription()).isEqualTo(UPDATED_FAILURE_DESCRIPTION);
    }

    @Test
    @Transactional
    public void updateNonExistingDocumentTask() throws Exception {
        int databaseSizeBeforeUpdate = documentTaskRepository.findAll().size();

        // Create the DocumentTask
        DocumentTaskDTO documentTaskDTO = documentTaskMapper.toDto(documentTask);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDocumentTaskMockMvc.perform(put("/api/document-tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(documentTaskDTO)))
            .andExpect(status().isBadRequest());

        // Validate the DocumentTask in the database
        List<DocumentTask> documentTaskList = documentTaskRepository.findAll();
        assertThat(documentTaskList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteDocumentTask() throws Exception {
        // Initialize the database
        documentTaskRepository.saveAndFlush(documentTask);

        int databaseSizeBeforeDelete = documentTaskRepository.findAll().size();

        // Get the documentTask
        restDocumentTaskMockMvc.perform(delete("/api/document-tasks/{id}", documentTask.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<DocumentTask> documentTaskList = documentTaskRepository.findAll();
        assertThat(documentTaskList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
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
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DocumentTaskDTO.class);
        DocumentTaskDTO documentTaskDTO1 = new DocumentTaskDTO();
        documentTaskDTO1.setId(1L);
        DocumentTaskDTO documentTaskDTO2 = new DocumentTaskDTO();
        assertThat(documentTaskDTO1).isNotEqualTo(documentTaskDTO2);
        documentTaskDTO2.setId(documentTaskDTO1.getId());
        assertThat(documentTaskDTO1).isEqualTo(documentTaskDTO2);
        documentTaskDTO2.setId(2L);
        assertThat(documentTaskDTO1).isNotEqualTo(documentTaskDTO2);
        documentTaskDTO1.setId(null);
        assertThat(documentTaskDTO1).isNotEqualTo(documentTaskDTO2);
    }

}
