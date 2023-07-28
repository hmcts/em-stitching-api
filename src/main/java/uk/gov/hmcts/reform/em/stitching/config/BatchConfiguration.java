package uk.gov.hmcts.reform.em.stitching.config;

import jakarta.persistence.EntityManagerFactory;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import uk.gov.hmcts.reform.em.stitching.batch.DocumentTaskCallbackProcessor;
import uk.gov.hmcts.reform.em.stitching.batch.DocumentTaskItemProcessor;
import uk.gov.hmcts.reform.em.stitching.batch.RemoveOldDocumentTaskTasklet;
import uk.gov.hmcts.reform.em.stitching.batch.RemoveSpringBatchHistoryTasklet;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.info.BuildInfo;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

import java.util.random.RandomGenerator;
import javax.sql.DataSource;

@EnableBatchProcessing
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT3M")
@Configuration
@ConditionalOnProperty(name = "scheduling.enabled")
public class BatchConfiguration {

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JobRepository jobRepository;
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

    @Autowired
    DocumentTaskRepository documentTaskRepository;

    @Value("${spring.batch.historicExecutionsRetentionMilliseconds}")
    int historicExecutionsRetentionMilliseconds;

    @Value("${spring.batch.documenttask.numberofdays}")
    int numberOfDays;

    // We need a handle to limit the no of records that are deleted.
    // Specially in the first few runs of this cleanup task.
    @Value("${spring.batch.documenttask.numberofrecords}")
    int numberOfRecords;

    @Value("${spring.batch.historicExecutionsRetentionEnabled}")
    boolean historicExecutionsRetentionEnabled;

    @Scheduled(fixedRateString = "${spring.batch.document-task-milliseconds}")
    @SchedulerLock(name = "${task.env}")
    public void schedule() throws JobParametersInvalidException,
            JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException {

        jobLauncher
            .run(processDocument(step1()), new JobParametersBuilder()
                    .addString("date",
                            System.currentTimeMillis() + "-" + RandomGenerator.getDefault().nextInt(0, 200))
            .toJobParameters());

        jobLauncher
            .run(processDocumentCallback(callBackStep1()), new JobParametersBuilder()
                    .addString("date",
                            System.currentTimeMillis() + "-" + RandomGenerator.getDefault().nextInt(300, 500))
            .toJobParameters());

    }

    @Scheduled(fixedDelayString = "${spring.batch.historicExecutionsRetentionMilliseconds}")
    @SchedulerLock(name = "${task.env}-historicExecutionsRetention")
    public void scheduleCleanup() throws JobParametersInvalidException,
            JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException {

        //This is to resolve the delay in DocumentTask been picked up by Shedlock.
        if (historicExecutionsRetentionEnabled) {
            jobLauncher.run(clearHistoryData(), new JobParametersBuilder()
                    .addString("date",
                            System.currentTimeMillis() + "-" + RandomGenerator.getDefault().nextInt(600, 900))
                    .toJobParameters());
        }

    }

    @Scheduled(cron = "${spring.batch.historicDocumentTasksCronJobSchedule}")
    @SchedulerLock(name = "${task.env}-historicDocumentTaskRetention")
    public void scheduleDocumentTaskCleanup() throws JobParametersInvalidException,
            JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException {

        jobLauncher.run(clearHistoricalDocumentTaskRecords(), new JobParametersBuilder()
                .addString("date",
                        System.currentTimeMillis() + "-" + RandomGenerator.getDefault().nextInt(1000, 1300))
                .toJobParameters());

    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

    @Bean
    public JpaPagingItemReader newDocumentTaskReader() {
        return new JpaPagingItemReaderBuilder<DocumentTask>()
            .name("documentTaskReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("select t from DocumentTask t JOIN FETCH t.bundle b"
                    + " where t.taskState = 'NEW' and t.version <= " + buildInfo.getBuildNumber()
                    + " order by t.createdDate")
            .pageSize(5)
            .build();
    }

    @Bean
    public JpaPagingItemReader completedWithCallbackDocumentTaskReader() {
        return new JpaPagingItemReaderBuilder<DocumentTask>()
                .name("documentTaskNewCallbackReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT dt FROM DocumentTask dt JOIN FETCH dt.bundle b JOIN FETCH dt.callback c where "
                        + "dt.taskState in ('DONE', 'FAILED') "
                        + "and dt.callback is not null "
                        + "and dt.callback.callbackState = 'NEW' "
                        + "and dt.version <= " + buildInfo.getBuildNumber()
                        + " order by dt.lastModifiedDate")
                .pageSize(5)
                .build();
    }

    @Bean
    public JpaItemWriter itemWriter() {
        //Below line needs to be removed once the access issue is resolved.
        System.setProperty("pdfbox.fontcache", "/tmp");
        JpaItemWriter writer = new JpaItemWriter<DocumentTask>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    @Bean
    public Job processDocument(Step step1) {
        return new JobBuilder("processDocumentJob", this.jobRepository)
                .start(step1)
                .build();
    }


    @Bean
    public Step step1() {
        return new StepBuilder("step1", this.jobRepository)
                .<DocumentTask, DocumentTask>chunk(10, transactionManager)
                .reader(newDocumentTaskReader())
                .processor(documentTaskItemProcessor)
                .writer(itemWriter())
                .build();

    }

    @Bean
    public Job processDocumentCallback(Step callBackStep1) {
        return  new JobBuilder("processDocumentCallbackJob", this.jobRepository)
                .flow(callBackStep1)
                .end()
                .build();
    }

    @Bean
    public Step callBackStep1() {
        return new StepBuilder("callbackStep1", this.jobRepository)
                .<DocumentTask, DocumentTask>chunk(10, transactionManager)
                .reader(completedWithCallbackDocumentTaskReader())
                .processor(documentTaskCallbackProcessor)
                .writer(itemWriter())
                .build();

    }

    @Bean
    public Job clearHistoryData() {
        return new JobBuilder("clearHistoricBatchExecutions", this.jobRepository)
                .flow(new StepBuilder("deleteAllExpiredBatchExecutions", this.jobRepository)
                        .tasklet(
                                new RemoveSpringBatchHistoryTasklet(
                                        historicExecutionsRetentionMilliseconds,
                                        jdbcTemplate),
                                transactionManager
                        )
                        .build()).build().build();
    }

    @Bean
    public Job clearHistoricalDocumentTaskRecords() {
        return new JobBuilder("clearHistoricalDocumentTaskRecords", this.jobRepository)
                .flow(new StepBuilder("deleteAllHistoricalDocumentTaskRecords", this.jobRepository)
                        .tasklet(
                                new RemoveOldDocumentTaskTasklet(documentTaskRepository, numberOfDays, numberOfRecords),
                                transactionManager)
                        .build()).build().build();
    }

}
