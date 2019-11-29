package uk.gov.hmcts.reform.em.stitching.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SchedulerLock;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.hmcts.reform.em.stitching.batch.DocumentTaskCallbackProcessor;
import uk.gov.hmcts.reform.em.stitching.batch.DocumentTaskItemProcessor;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.info.BuildInfo;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Date;

@EnableBatchProcessing
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
@Configuration
public class BatchConfiguration {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public EntityManagerFactory entityManagerFactory;

    @Autowired
    public JobLauncher jobLauncher;

    @Autowired
    public BuildInfo buildInfo;

    @Autowired
    public DocumentTaskItemProcessor documentTaskItemProcessor;

    @Autowired
    public DocumentTaskCallbackProcessor documentTaskCallbackProcessor;

    @Scheduled(fixedRate = 1000)
    @SchedulerLock(name = "${task.env}")
    public void schedule() throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        jobLauncher
            .run(processDocument(step1()), new JobParametersBuilder()
            .addDate("date", new Date())
            .toJobParameters());
    }


    @Scheduled(fixedRate = 1000)
    @SchedulerLock(name = "${task.env}_callback")
    public void callbackSchedule()
            throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        jobLauncher
                .run(processDocumentCallback(callBackStep1()), new JobParametersBuilder()
                        .addDate("date", new Date())
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
            .queryString("select t from DocumentTask t where t.taskState = 'NEW' and t.version <= " + buildInfo.getBuildNumber() + " order by t.createdDate")
            .pageSize(10)
            .build();
    }

    @Bean
    public JpaPagingItemReader completedWithCallbackDocumentTaskReader() {
        return new JpaPagingItemReaderBuilder<DocumentTask>()
                .name("documentTaskNewCallbackReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT dt FROM DocumentTask dt where "
                        + "dt.taskState in ('DONE', 'FAILED') "
                        + "and dt.callback is not null "
                        + "and dt.callback.callbackState = 'NEW' "
                        + "and dt.version <= " + buildInfo.getBuildNumber()
                        + " order by dt.lastModifiedDate")
                .pageSize(10)
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

}
