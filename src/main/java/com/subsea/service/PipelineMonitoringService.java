package com.subsea.service;

import com.subsea.dao.CorrosionAuditDAO;
import com.subsea.dao.PigInspectionDAO;
import com.subsea.dao.PipeSegmentDAO;
import com.subsea.dao.RepairOrderDAO;
import com.subsea.model.CorrosionAudit;
import com.subsea.model.IntegrityResult;
import com.subsea.model.PigInspection;
import com.subsea.model.PipeSegment;
import com.subsea.model.RepairOrder;
import com.subsea.worker.CorrosionAuditWorker;
import com.subsea.worker.PigInspectionWorker;
import com.subsea.worker.PipelineIntegrityWorker;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PipelineMonitoringService is the primary service orchestrator for Student 1.
 *
 * Responsibilities:
 * 1. Fetch registered pipeline segments from MySQL.
 * 2. Concurrently fetch and preprocess PIG inspection telemetry.
 * 3. Concurrently fetch and preprocess corrosion & acoustic telemetry.
 * 4. Concurrently calculate PCI and Rupture Risk for each pipeline segment.
 * 5. Persist generated repair orders into the MySQL 'repair_orders' table.
 * 6. Return unified integrity results for reporting.
 */
public class PipelineMonitoringService {

    private static final Logger LOGGER = Logger.getLogger(PipelineMonitoringService.class.getName());

    private final PipeSegmentDAO segmentDAO;
    private final PigInspectionDAO inspectionDAO;
    private final CorrosionAuditDAO auditDAO;
    private final RepairOrderDAO repairOrderDAO;
    private final int threadPoolSize;

    public PipelineMonitoringService() {
        this(new PipeSegmentDAO(), new PigInspectionDAO(), new CorrosionAuditDAO(), new RepairOrderDAO(), 4);
    }

    public PipelineMonitoringService(PipeSegmentDAO segmentDAO, PigInspectionDAO inspectionDAO,
                                    CorrosionAuditDAO auditDAO, RepairOrderDAO repairOrderDAO,
                                    int threadPoolSize) {
        this.segmentDAO = segmentDAO;
        this.inspectionDAO = inspectionDAO;
        this.auditDAO = auditDAO;
        this.repairOrderDAO = repairOrderDAO;
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Executes the full pipeline integrity monitoring workflow with multithreaded processing.
     *
     * @return List of computed IntegrityResult objects
     * @throws SQLException if a database error occurs
     */
    public List<IntegrityResult> executeMonitoringCycle() throws SQLException {
        System.out.println("\n==========================================================================");
        System.out.println(">>> STARTING SUBSEA PIPELINE MONITORING CYCLE (MULTITHREADED ENGINE)");
        System.out.println("==========================================================================");

        // Step 1: Retrieve registered pipeline segments
        List<PipeSegment> segments = segmentDAO.getAllSegments();
        System.out.printf("[INFO] Retrieved %d active pipeline segment(s) from database.%n", segments.size());
        if (segments.isEmpty()) {
            System.out.println("[WARN] No pipeline segments found in database. Monitoring cycle terminated.");
            return new ArrayList<>();
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r);
            t.setName("SubseaWorker-" + t.getId());
            return t;
        });

        List<IntegrityResult> results = new ArrayList<>();

        try {
            // Step 2 & 3: Concurrently launch telemetry data acquisition
            System.out.println("\n--- [STAGE 1] Concurrently Acquiring PIG Inspections & Corrosion Telemetry ---");
            Map<Integer, Future<Optional<PigInspection>>> pigFutures = new HashMap<>();
            Map<Integer, Future<Optional<CorrosionAudit>>> auditFutures = new HashMap<>();

            for (PipeSegment segment : segments) {
                int segId = segment.getSegmentId();
                String segCode = segment.getSegmentCode();

                pigFutures.put(segId, executor.submit(new PigInspectionWorker(segId, segCode, inspectionDAO)));
                auditFutures.put(segId, executor.submit(new CorrosionAuditWorker(segId, segCode, auditDAO)));
            }

            // Gather telemetry results
            Map<Integer, PigInspection> pigResults = new HashMap<>();
            Map<Integer, CorrosionAudit> auditResults = new HashMap<>();

            for (Map.Entry<Integer, Future<Optional<PigInspection>>> entry : pigFutures.entrySet()) {
                try {
                    Optional<PigInspection> opt = entry.getValue().get();
                    opt.ifPresent(p -> pigResults.put(entry.getKey(), p));
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.log(Level.SEVERE, "PIG Inspection acquisition failed for segment " + entry.getKey(), e);
                }
            }

            for (Map.Entry<Integer, Future<Optional<CorrosionAudit>>> entry : auditFutures.entrySet()) {
                try {
                    Optional<CorrosionAudit> opt = entry.getValue().get();
                    opt.ifPresent(a -> auditResults.put(entry.getKey(), a));
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.log(Level.SEVERE, "Corrosion audit acquisition failed for segment " + entry.getKey(), e);
                }
            }

            // Step 4: Concurrently compute PCI and Rupture Risk Assessment
            System.out.println("\n--- [STAGE 2] Concurrently Computing PCI & Rupture Risk Factors ---");
            List<Future<IntegrityResult>> calculationFutures = new ArrayList<>();

            for (PipeSegment segment : segments) {
                int segId = segment.getSegmentId();
                PigInspection pig = pigResults.get(segId);
                CorrosionAudit audit = auditResults.get(segId);

                calculationFutures.add(executor.submit(new PipelineIntegrityWorker(segment, pig, audit)));
            }

            // Collect computation results
            for (Future<IntegrityResult> future : calculationFutures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.log(Level.SEVERE, "Integrity calculation worker failed: " + e.getMessage(), e);
                }
            }

            // Step 5: Persist Generated Repair Orders into MySQL
            System.out.println("\n--- [STAGE 3] Generating & Persisting Subsea Repair Orders into MySQL ---");
            int savedCount = 0;
            int skippedCount = 0;

            for (IntegrityResult res : results) {
                RepairOrder ro = res.toRepairOrder();

                // Prevent duplicates if already inserted recently with the same hazard code
                if (repairOrderDAO.existsRecentOrder(ro.getSegmentId(), ro.getHazardCode())) {
                    System.out.printf("  [SKIP] Recent repair order already on record for %s with hazard [%s].%n",
                            res.getSegment().getSegmentCode(), ro.getHazardCode());
                    skippedCount++;
                } else {
                    repairOrderDAO.insertRepairOrder(ro);
                    System.out.printf("  [SAVED] Repair Order #%d created for %s | Hazard: %s | Action: %s%n",
                            ro.getRepairId(), res.getSegment().getSegmentCode(),
                            ro.getHazardCode(), ro.getActionRequired());
                    savedCount++;
                }
            }

            System.out.printf("[SUMMARY] Repair Orders: %d newly saved, %d existing/skipped.%n", savedCount, skippedCount);

        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return results;
    }
}
