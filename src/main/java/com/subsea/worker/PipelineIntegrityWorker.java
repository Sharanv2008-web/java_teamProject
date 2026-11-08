package com.subsea.worker;

import com.subsea.calculation.PCICalculator;
import com.subsea.calculation.RuptureRiskCalculator;
import com.subsea.model.CorrosionAudit;
import com.subsea.model.IntegrityResult;
import com.subsea.model.PigInspection;
import com.subsea.model.PipeSegment;

import java.math.BigDecimal;
import java.util.concurrent.Callable;

/**
 * Concurrent worker that synthesizes pipeline segment data, PIG inspection telemetry,
 * and corrosion audit results to compute the Pipeline Condition Index (PCI),
 * Rupture Risk Factor (RRF), hazard classification, and required maintenance action.
 */
public class PipelineIntegrityWorker implements Callable<IntegrityResult> {

    private final PipeSegment segment;
    private final PigInspection inspection;
    private final CorrosionAudit audit;

    public PipelineIntegrityWorker(PipeSegment segment, PigInspection inspection, CorrosionAudit audit) {
        this.segment = segment;
        this.inspection = inspection;
        this.audit = audit;
    }

    @Override
    public IntegrityResult call() throws Exception {
        String threadName = Thread.currentThread().getName();
        String code = segment != null ? segment.getSegmentCode() : "UNKNOWN";

        System.out.printf("  [Thread: %-18s] Computing Integrity & Rupture Risk for %s...%n",
                threadName, code);

        // 1. Calculate Pipeline Condition Index (PCI)
        BigDecimal pciScore = PCICalculator.calculatePCI(segment, inspection, audit);

        // 2. Determine qualitative integrity grade (A - F)
        String grade = PCICalculator.determineGrade(pciScore);

        // 3. Compute Rupture Risk Factor (RRF)
        BigDecimal ruptureRisk = RuptureRiskCalculator.calculateRuptureRisk(segment, inspection, audit, pciScore);

        // 4. Identify dominant hazard classification
        String hazardCode = RuptureRiskCalculator.identifyHazardCode(segment, inspection, audit, pciScore, ruptureRisk);

        // 5. Formulate actionable subsea maintenance directive
        String recommendedAction = RuptureRiskCalculator.determineActionRequired(hazardCode, pciScore, ruptureRisk, code);

        System.out.printf("  [Thread: %-18s] Computed [%s] -> PCI: %.2f (Grade %s) | Rupture Risk: %.4f | Hazard: %s%n",
                threadName, code, pciScore.doubleValue(), grade, ruptureRisk.doubleValue(), hazardCode);

        return new IntegrityResult(segment, inspection, audit, pciScore, grade, ruptureRisk, hazardCode, recommendedAction);
    }
}
