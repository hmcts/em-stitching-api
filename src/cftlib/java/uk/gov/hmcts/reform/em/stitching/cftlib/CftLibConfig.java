package uk.gov.hmcts.reform.em.stitching.cftlib;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class CftLibConfig implements CFTLibConfigurer {

    @Override
    public void configure(CFTLib lib) throws Exception {
        for (String p : List.of(
                "bundle-tester@gmail.com",
                "stitchingTestUser@stitchingTest.com")) {
            lib.createProfile(p, "PUBLICLAW", "CCD_BUNDLE_MVP_TYPE_ASYNC", "1_OPENED");
        }

        lib.createRoles("caseworker","caseworker-publiclaw", "ccd-import");

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        var json = IOUtils.toString(resourceLoader.getResource("classpath:cftlib-am-role-assignments.json")
                .getInputStream(), Charset.defaultCharset());
        lib.configureRoleAssignments(json);

        lib.importDefinition(Files.readAllBytes(
                Path.of("src/aat/resources/adv_stitching_functional_tests_ccd_def.xlsx")));

    }
}