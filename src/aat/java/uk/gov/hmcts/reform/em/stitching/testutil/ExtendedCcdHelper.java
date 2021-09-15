package uk.gov.hmcts.reform.em.stitching.testutil;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefinitionHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExtendedCcdHelper {

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    private IdamHelper idamHelper;



    @Autowired
    private CcdDefinitionHelper ccdDefinitionHelper;

    private String stitchingTestUser = "stitchingTestUser@stitchingTest.com";
    private List<String> stitchingTestUserRoles = Stream.of("caseworker", "caseworker-publiclaw", "ccd-import").collect(Collectors.toList());

    @PostConstruct
    public void init() throws Exception {
        importCcdDefinitionFile();
    }

    public void importCcdDefinitionFile() throws Exception {

        ccdDefinitionHelper.importDefinitionFile(stitchingTestUser,
                "caseworker-publiclaw",
                getEnvSpecificDefinitionFile());
    }

    public InputStream getEnvSpecificDefinitionFile() throws Exception {
        Workbook workbook = new XSSFWorkbook(ClassLoader.getSystemResourceAsStream(
            "adv_stitching_functional_tests_ccd_def.xlsx"));
        Sheet caseEventSheet = workbook.getSheet("CaseEvent");


        Sheet caseTypeSheet = workbook.getSheet("CaseType");

        caseTypeSheet.getRow(3).getCell(3).setCellValue(getEnvCcdCaseTypeId());

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType().equals(CellType.STRING)
                            && cell.getStringCellValue().trim().equals("CCD_BUNDLE_MVP_TYPE_ASYNC")) {
                        cell.setCellValue(getEnvCcdCaseTypeId());
                    }
                    if (cell.getCellType().equals(CellType.STRING)
                            && cell.getStringCellValue().trim().equals("bundle-tester@gmail.com")) {
                        cell.setCellValue(stitchingTestUser);
                    }
                }
            }
        }

        File outputFile = File.createTempFile("ccd", "ftest-def");

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            workbook.write(fileOutputStream);
        }

        return new FileInputStream(outputFile);
    }

    public String getEnvCcdCaseTypeId() {
        return String.format("STITCHING_%d", testUrl.hashCode());
    }
}



