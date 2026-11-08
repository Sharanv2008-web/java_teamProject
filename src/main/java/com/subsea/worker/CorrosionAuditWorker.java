package com.subsea.worker;

import com.subsea.dao.CorrosionAuditDAO;
import com.subsea.model.CorrosionAudit;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Concurrent worker responsible for asynchronously retrieving and analyzing
 * corrosion and acoustic telemetry audits for a specific pipeline segment.
 */
public class CorrosionAuditWorker implements Callable<Optional<CorrosionAudit>> {

    private static final Logger LOGGER = Logger.getLogger(CorrosionAuditWorker.class.getName());

    private final int segmentId;
    private final String segmentCode;
    private final CorrosionAuditDAO auditDAO;

    public CorrosionAuditWorker(int segmentId, String segmentCode, CorrosionAuditDAO auditDAO) {
        this.segmentId = segmentId;
        this.segmentCode = segmentCode;
        this.auditDAO = auditDAO;
    }

    @Override
    public Optional<CorrosionAudit> call() throws Exception {
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [Thread: %-18s] Processing Corrosion Audit for segment %s (ID: %d)...%n",
                threadName, segmentCode, segmentId);

        // Fetch latest Corrosion audit data from DB
        Optional<CorrosionAudit> audit = auditDAO.getLatestAuditBySegmentId(segmentId);

        if (audit.isPresent()) {
            CorrosionAudit ca = audit.get();
            System.out.printf("  [Thread: %-18s] Corrosion Telemetry Acquired [%s]: Flow Loss=%.2f%%, LeakHz=%.1fHz, HydrophoneScore=%.1f%n",
                    threadName, segmentCode,
                    ca.getFlowLossPercent(),
                    ca.getAcousticLeakHz() != null ? ca.getAcousticLeakHz().doubleValue() : 0.0,
                    ca.getHydrophoneNoiseScore() != null ? ca.getHydrophoneNoiseScore().doubleValue() : 0.0);
        } else {
            System.out.printf("  [Thread: %-18s] No corrosion audit records found for segment %s%n",
                    threadName, segmentCode);
        }

        return audit;
    }
}
