package uk.gov.hmcts.reform.em.stitching.config;

import org.hibernate.LockOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.hmcts.reform.em.stitching.batch.DocumentTaskCallbackProcessor;
import uk.gov.hmcts.reform.em.stitching.batch.DocumentTaskItemProcessor;
import uk.gov.hmcts.reform.em.stitching.batch.RemoveSpringBatchHistoryTasklet;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.info.BuildInfo;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.util.Date;

@EnableBatchProcessing
@EnableScheduling
@Configuration
public class BatchConfiguration {

    private final Logger log = LoggerFactory.getLogger(BatchConfiguration.class);

    @Autowired
    JobBuilderFactory jobBuilderFactory;

    @Autowired
    StepBuilderFactory stepBuilderFactory;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    BuildInfo buildInfo;

    @Autowired
    DocumentTaskItemProcessor documentTaskItemProcessor;

    @Autowired
    DocumentTaskCallbackProcessor documentTaskCallbackProcessor;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${spring.batch.historicExecutionsRetentionMilliseconds}")
    int historicExecutionsRetentionMilliseconds;

    @Scheduled(fixedRateString = "${spring.batch.document-task-milliseconds}")
    public void schedule() throws JobParametersInvalidException,
            JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException {

        log.info(String.format("schedule invoked"));
        jobLauncher
            .run(processDocument(step1()), new JobParametersBuilder()
            .addDate("date", new Date())
            .toJobParameters());

        jobLauncher
            .run(processDocumentCallback(callBackStep1()), new JobParametersBuilder()
            .addDate("date", new Date())
            .toJobParameters());

    }

    @Scheduled(fixedDelayString = "${spring.batch.historicExecutionsRetentionMilliseconds}")
    public void scheduleCleanup() throws JobParametersInvalidException,
            JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException {

        jobLauncher.run(clearHistoryData(), new JobParametersBuilder()
                .addDate("date", new Date())
                .toJobParameters());

    }

    @Bean
    public JpaPagingItemReader newDocumentTaskReader() {
        log.info(String.format("newDocumentTaskReader invoked"));
        return new JpaPagingItemReaderBuilder<DocumentTask>()
            .name("documentTaskReader")
            .entityManagerFactory(entityManagerFactory)
            .queryProvider(new QueryProvider())
            .pageSize(5)
            .build();
    }

    @Bean
    public JpaPagingItemReader completedWithCallbackDocumentTaskReader() {
        log.info(String.format("completedWithCallbackDocumentTaskReader invoked"));
        return new JpaPagingItemReaderBuilder<DocumentTask>()
                .name("documentTaskNewCallbackReader")
                .entityManagerFactory(entityManagerFactory)
                .queryProvider(new QueryProviderCallback())
                .pageSize(5)
                .build();
    }

    @Bean
    public JpaItemWriter itemWriter() {
        log.info(String.format("itemWriter invoked"));
        //Below line needs to be removed once the access issue is resolved.
        System.setProperty("pdfbox.fontcache", "/tmp");
        JpaItemWriter writer = new JpaItemWriter<DocumentTask>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    @Bean
    public Job processDocument(Step step1) {
        return jobBuilderFactory.get("processDocumentJob")
            .flow(step1)
            .end()
            .build();
    }

    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
            .<DocumentTask, DocumentTask>chunk(10)
            .reader(newDocumentTaskReader())
            .processor(documentTaskItemProcessor)
            .writer(itemWriter())
            .build();

    }

    @Bean
    public Job processDocumentCallback(Step callBackStep1) {
        return jobBuilderFactory.get("processDocumentCallbackJob")
                .flow(callBackStep1)
                .end()
                .build();
    }

    @Bean
    public Step callBackStep1() {
        return stepBuilderFactory.get("callbackStep1")
                .<DocumentTask, DocumentTask>chunk(10)
                .reader(completedWithCallbackDocumentTaskReader())
                .processor(documentTaskCallbackProcessor)
                .writer(itemWriter())
                .build();

    }

    @Bean
    public Job clearHistoryData() {
        return jobBuilderFactory.get("clearHistoricBatchExecutions")
                .flow(stepBuilderFactory.get("deleteAllExpiredBatchExecutions")
                        .tasklet(new RemoveSpringBatchHistoryTasklet(historicExecutionsRetentionMilliseconds, jdbcTemplate))
                            .build()).build().build();
    }

    private class QueryProvider implements JpaQueryProvider {
        private EntityManager entityManager;

        @Override
        public Query createQuery() {
            return entityManager
                    .createQuery("select t from DocumentTask t JOIN FETCH t.bundle b"
                            + " where t.taskState = 'NEW' and t.version <= " + buildInfo.getBuildNumber()
                            + " order by t.createdDate")
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setHint("javax.persistence.lock.timeout", LockOptions.SKIP_LOCKED);
        }

        @Override
        public void setEntityManager(EntityManager entityManager) {
            this.entityManager = entityManager;

        }
    }

    private class QueryProviderCallback implements JpaQueryProvider {
        private EntityManager entityManager;

        @Override
        public Query createQuery() {
            return entityManager
                    .createQuery("SELECT dt FROM DocumentTask dt JOIN FETCH dt.bundle b JOIN FETCH dt.callback c where "
                            + "dt.taskState in ('DONE', 'FAILED') "
                            + "and dt.callback is not null "
                            + "and dt.callback.callbackState = 'NEW' "
                            + "and dt.version <= " + buildInfo.getBuildNumber()
                            + " order by dt.lastModifiedDate")
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setHint("javax.persistence.lock.timeout", LockOptions.SKIP_LOCKED);
        }

        @Override
        public void setEntityManager(EntityManager entityManager) {
            this.entityManager = entityManager;

        }
    }

}
