package com.subsea.pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * MainApp - Deep-Sea Oil & Gas Subsea Pipeline Integrity Diagnostics System
 * Project Assignment Set 30
 * 
 * Orchestrates:
 * 1. Database Connectivity to OCI VM 2 (Database Host)
 * 2. Phased Incremental Data Entry (Phase 1: Segment Registration, Phase 2: PIG Telemetry, Phase 3: Flow Audit)
 * 3. Multi-threaded parallel telemetry stream processing
 * 4. Regulatory CSV report generation
 */
public class MainApp {

    private static final String TELEMETRY_FILE = "data/pig_telemetry.txt";
    private static final String REPAIR_CSV_FILE = "reports/subsea_repair_authorizations.csv";
    private static final String INTEGRITY_CSV_FILE = "reports/pipeline_integrity_report.csv";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DEEP-SEA OIL & GAS SUBSEA PIPELINE INTEGRITY DIAGNOSTICS SYSTEM");
        System.out.println(" Project Assignment Set 30 - Integrated Mission Control");
        System.out.println(" Application Host (VM 1) <---> Database Host (VM 2: Port 3306/5432)");
        System.out.println("================================================================================\n");

        PipelineDAO dao = new PipelineDAO();

        // =====================================================================
        // STEP 1: INITIALIZE DATABASE CONNECTION TO OCI VM 2
        // =====================================================================
        System.out.println(">> [Step 1] Connecting to Subsea Database on OCI VM 2...");
        System.out.printf("   Target Host: %s | Port: %d | Database: %s | User: %s%n",
                DBConnection.getHost(), DBConnection.getPort(), DBConnection.getDatabase(), DBConnection.getUsername());
        
        try {
            if (DBConnection.isDriverAvailable()) {
                DBConnection.getConnection();
            } else {
                System.out.println("   [Mode] Operating in Resilient Embedded / Standalone Simulation Mode.");
            }
        } catch (Exception e) {
            System.out.println("   [DB Offline Fallback] Live DB connection deferred. Utilizing in-memory synchronized DAO mirror.");
        }

        // =====================================================================
        // STEP 2: PHASE 1 - SEGMENT REGISTRATION
        // =====================================================================
        System.out.println("\n>> [Step 2] PHASE 1: Subsea Pipeline Segment Registration...");
        registerBaselineSegments(dao);

        // =====================================================================
        // STEP 3: READ SMART PIG LOG FILE
        // =====================================================================
        System.out.println("\n>> [Step 3] Loading Smart PIG Inspection Telemetry from: " + TELEMETRY_FILE);
        List<String> rawLogLines = readTelemetryFile(TELEMETRY_FILE);
        System.out.printf("   Successfully loaded %d telemetry records from storage.%n", rawLogLines.size());

        // =====================================================================
        // STEP 4: PHASE 2 & 3 - MULTI-THREADED TELEMETRY STREAM PROCESSING
        // =====================================================================
        System.out.println("\n>> [Step 4] Launching Multi-Threaded Diagnostics Pool across Subsea Pipeline...");
        int threadPoolSize = Math.min(4, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<IntegrityWorker.WorkerResult>> futures = new ArrayList<>();

        int threadIndex = 1;
        for (String line : rawLogLines) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            IntegrityWorker worker = new IntegrityWorker(line, dao, threadIndex++);
            futures.add(executor.submit(worker));
        }

        // Collect and evaluate results
        List<String> hazardNotifications = new ArrayList<>();
        int successCount = 0;
        int regexFailCount = 0;

        for (Future<IntegrityWorker.WorkerResult> future : futures) {
            try {
                IntegrityWorker.WorkerResult res = future.get(5, TimeUnit.SECONDS);
                if (res.success) {
                    successCount++;
                    System.out.println("   " + res.logMessage);
                    if (res.hazardNotification != null) {
                        hazardNotifications.add(res.hazardNotification);
                    }
                } else {
                    if (res.validationError != null && res.validationError.contains("REGEX")) {
                        regexFailCount++;
                        System.out.println("   [WARNING] " + res.validationError);
                    }
                }
            } catch (Exception e) {
                System.err.println("   [ERROR] Worker thread interrupted: " + e.getMessage());
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        // =====================================================================
        // STEP 5: HAZARD NOTIFICATION BROADCAST
        // =====================================================================
        System.out.println("\n>> [Step 5] ACTIVE CRITICAL CORROSION HAZARD ALERTS:");
        if (hazardNotifications.isEmpty()) {
            System.out.println("   [STATUS] All monitored subsea pipeline segments operating within safe bands.");
        } else {
            for (String alert : hazardNotifications) {
                System.out.println("--------------------------------------------------------------------------------");
                System.out.println(alert);
            }
            System.out.println("--------------------------------------------------------------------------------");
        }

        // =====================================================================
        // STEP 6: FILE I/O - EXPORT CSV REPORTS
        // =====================================================================
        System.out.println("\n>> [Step 6] Generating Regulatory Subsea CSV Compliance Reports...");
        RepairOrderWriter.writeRepairAuthorizations(dao.getAllRepairOrders(), dao, REPAIR_CSV_FILE);
        RepairOrderWriter.writeIntegrityReport(dao.getAllAudits(), dao, INTEGRITY_CSV_FILE);

        // =====================================================================
        // STEP 7: DATABASE VERIFICATION AUDIT & SYSTEM SUMMARY
        // =====================================================================
        System.out.println("\n================================================================================");
        System.out.println(" SUBSEA SYSTEM DIAGNOSTIC & DATABASE AUDIT SUMMARY (STUDENT 2 DB HOST)");
        System.out.println("================================================================================");
        Map<String, Integer> stats = dao.getDatabaseStatistics();
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            System.out.printf("   - Table [%-24s] : %d records synchronized%n", entry.getKey(), entry.getValue());
        }
        System.out.printf("   - Total PIG Ingestion Streams Processed: %d%n", successCount);
        System.out.printf("   - Corrupt/Invalid Regex Lines Filtered : %d%n", regexFailCount);
        System.out.printf("   - Active Emergency Repair Orders Issued: %d%n", dao.getAllRepairOrders().size());
        System.out.println("================================================================================\n");
    }

    private static void registerBaselineSegments(PipelineDAO dao) {
        // High-Pressure Deepwater Pipe: Max Wall Loss Cap = 30%
        dao.registerPipeSegment(new PipelineDAO.PipeSegment(
                "PIPE-SUB-10001", "HIGH-PRESSURE DEEPWATER PIPE", 2150.50, 38.50, "Crude Oil", 4200.00, 30.00, "Green Canyon Ultra-Deep 800"
        ));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment(
                "PIPE-SUB-10002", "HIGH-PRESSURE DEEPWATER PIPE", 1890.00, 35.00, "Crude Oil", 3900.00, 30.00, "Mississippi Canyon Deep"
        ));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment(
                "PIPE-SUB-10003", "HIGH-PRESSURE DEEPWATER PIPE", 2420.00, 40.00, "Crude Oil", 4400.00, 30.00, "Walker Ridge Basin"
        ));

        // Shallow Coastal Trunk Line: Max Wall Loss Cap = 35%
        dao.registerPipeSegment(new PipelineDAO.PipeSegment(
                "PIPE-SUB-20001", "SHALLOW COASTAL TRUNK LINE", 145.20, 22.00, "Multiphase", 1600.00, 35.00, "Galveston Coastal Shelf"
        ));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment(
                "PIPE-SUB-20002", "SHALLOW COASTAL TRUNK LINE", 85.00, 20.00, "Multiphase", 1450.00, 35.00, "Mobile Bay Coastal Transition"
        ));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment(
                "PIPE-SUB-20003", "SHALLOW COASTAL TRUNK LINE", 110.50, 21.00, "Multiphase", 1550.00, 35.00, "Sabine Pass Coastal Sector"
        ));

        // Liquefied Gas Feeder: Max Wall Loss Cap = 25%
        dao.registerPipeSegment(new PipelineDAO.PipeSegment(
                "PIPE-SUB-30001", "LIQUEFIED GAS FEEDER", 620.80, 28.00, "Liquefied Gas", 2800.00, 25.00, "Garden Banks Gas Corridor"
        ));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment(
                "PIPE-SUB-30002", "LIQUEFIED GAS FEEDER", 710.00, 26.50, "Liquefied Gas", 2700.00, 25.00, "Alaminos Canyon Gas Feeder"
        ));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment(
                "PIPE-SUB-30003", "LIQUEFIED GAS FEEDER", 580.00, 25.00, "Liquefied Gas", 2650.00, 25.00, "Keathley Canyon Gas Trunk"
        ));

        System.out.printf("   Registered 9 baseline subsea assets across all 3 asset categories.%n");
    }

    private static List<String> readTelemetryFile(String path) {
        List<String> lines = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("   [WARNING] Telemetry file not found at " + path);
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("   [ERROR] Could not read telemetry file: " + e.getMessage());
        }
        return lines;
    }
}
