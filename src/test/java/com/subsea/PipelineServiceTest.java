package com.subsea;

import com.subsea.config.DBConnection;
import com.subsea.model.IntegrityResult;
import com.subsea.report.CsvReportGenerator;
import com.subsea.service.PipelineMonitoringService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PipelineServiceTest {

    @BeforeAll
    static void checkDatabaseAvailable() {
        Assumptions.assumeTrue(DBConnection.testConnection(),
                "MySQL database must be reachable to run integration tests.");
    }

    @Test
    @DisplayName("Complete Monitoring Service Cycle with Concurrency and DB Persistence")
    void testExecuteMonitoringCycle() throws SQLException, IOException {
        PipelineMonitoringService service = new PipelineMonitoringService();
        List<IntegrityResult> results = service.executeMonitoringCycle();

        assertNotNull(results, "Monitoring results list should not be null");
        assertFalse(results.isEmpty(), "Expected at least 1 pipeline segment result");

        for (IntegrityResult r : results) {
            assertNotNull(r.getSegment(), "Segment must not be null");
            assertNotNull(r.getPciScore(), "PCI Score must be computed");
            assertNotNull(r.getIntegrityGrade(), "Grade must be assigned");
            assertNotNull(r.getRuptureRiskFactor(), "Rupture risk must be computed");
            assertNotNull(r.getHazardCode(), "Hazard code must be determined");
            assertNotNull(r.getRecommendedAction(), "Action required must be populated");
        }

        // Test CSV Report Generation
        CsvReportGenerator generator = new CsvReportGenerator("reports");
        File report = generator.generateReport(results);

        assertNotNull(report, "Generated report file should not be null");
        assertTrue(report.exists(), "CSV report file must exist on disk");
        assertTrue(report.length() > 0, "CSV report must contain data bytes");
    }
}
