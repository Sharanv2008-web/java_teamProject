package com.subsea.worker;

import com.subsea.dao.PigInspectionDAO;
import com.subsea.model.PigInspection;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Concurrent worker responsible for asynchronously retrieving and analyzing
 * PIG inspection telemetry for a specific pipeline segment.
 */
public class PigInspectionWorker implements Callable<Optional<PigInspection>> {

    private static final Logger LOGGER = Logger.getLogger(PigInspectionWorker.class.getName());

    private final int segmentId;
    private final String segmentCode;
    private final PigInspectionDAO inspectionDAO;

    public PigInspectionWorker(int segmentId, String segmentCode, PigInspectionDAO inspectionDAO) {
        this.segmentId = segmentId;
        this.segmentCode = segmentCode;
        this.inspectionDAO = inspectionDAO;
    }

    @Override
    public Optional<PigInspection> call() throws Exception {
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [Thread: %-18s] Processing PIG inspection for segment %s (ID: %d)...%n",
                threadName, segmentCode, segmentId);

        // Fetch latest PIG inspection data from DB
        Optional<PigInspection> inspection = inspectionDAO.getLatestInspectionBySegmentId(segmentId);

        if (inspection.isPresent()) {
            PigInspection pig = inspection.get();
            System.out.printf("  [Thread: %-18s] PIG Telemetry Acquired [%s]: Wall Loss=%.2f%%, CP=%.2fV, Pressure=%.1fpsi%n",
                    threadName, segmentCode,
                    pig.getWallLossPercent() != null ? pig.getWallLossPercent().doubleValue() : 0.0,
                    pig.getCathodicVoltageV() != null ? pig.getCathodicVoltageV().doubleValue() : 0.0,
                    pig.getInternalPressurePsi() != null ? pig.getInternalPressurePsi().doubleValue() : 0.0);
        } else {
            System.out.printf("  [Thread: %-18s] No PIG inspection records found for segment %s%n",
                    threadName, segmentCode);
        }

        return inspection;
    }
}
