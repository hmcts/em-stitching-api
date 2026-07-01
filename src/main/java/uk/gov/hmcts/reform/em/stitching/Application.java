package uk.gov.hmcts.reform.em.stitching;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.hmcts.reform.em.stitching.service.ScheduledTaskRunner;

import java.util.Objects;

@SpringBootApplication(
        scanBasePackages = {
            "uk.gov.hmcts.reform.ccd.document.am",
            "uk.gov.hmcts.reform.em.stitching",
            "uk.gov.hmcts.reform.auth"}
)
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, it's not a utility class
@EnableScheduling
public class Application implements CommandLineRunner {

    private static final String TASK_NAME = "TASK_NAME";


    private final ScheduledTaskRunner taskRunner;

    public Application(ScheduledTaskRunner taskRunner) {
        this.taskRunner = taskRunner;
    }

    public static void main(String[] args) {
        final var application = new SpringApplication(Application.class);
        final var instance = application.run(args);

        //When TASK_NAME exists, we need the Application to be run as AKS job.
        if (Objects.nonNull(System.getenv(TASK_NAME))) {
            instance.close();
        }
    }

    @Override
    public void run(String... args) {
        if (Objects.nonNull(System.getenv(TASK_NAME))) {
            taskRunner.run(System.getenv(TASK_NAME));
        }
    }
}
