package com.subsea.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unified pipeline integrity analysis result encapsulating segment data,
 * telemetry from PIG inspections and corrosion audits, computed PCI score,
 * rupture risk assessment, and recommended engineering action.
 */
public class IntegrityResult {

    private PipeSegment segment;
    private PigInspection inspection;
    private CorrosionAudit audit;

    private BigDecimal pciScore;
    private String integrityGrade;
    private BigDecimal ruptureRiskFactor;
    private String hazardCode;
    private String recommendedAction;
    private LocalDateTime analyzedAt;

    public IntegrityResult() {
        this.analyzedAt = LocalDateTime.now();
    }

    public IntegrityResult(PipeSegment segment, PigInspection inspection, CorrosionAudit audit,
                           BigDecimal pciScore, String integrityGrade, BigDecimal ruptureRiskFactor,
                           String hazardCode, String recommendedAction) {
        this.segment = segment;
        this.inspection = inspection;
        this.audit = audit;
        this.pciScore = pciScore;
        this.integrityGrade = integrityGrade;
        this.ruptureRiskFactor = ruptureRiskFactor;
        this.hazardCode = hazardCode;
        this.recommendedAction = recommendedAction;
        this.analyzedAt = LocalDateTime.now();
    }

    public PipeSegment getSegment() { return segment; }
    public void setSegment(PipeSegment segment) { this.segment = segment; }

    public PigInspection getInspection() { return inspection; }
    public void setInspection(PigInspection inspection) { this.inspection = inspection; }

    public CorrosionAudit getAudit() { return audit; }
    public void setAudit(CorrosionAudit audit) { this.audit = audit; }

    public BigDecimal getPciScore() { return pciScore; }
    public void setPciScore(BigDecimal pciScore) { this.pciScore = pciScore; }

    public String getIntegrityGrade() { return integrityGrade; }
    public void setIntegrityGrade(String integrityGrade) { this.integrityGrade = integrityGrade; }

    public BigDecimal getRuptureRiskFactor() { return ruptureRiskFactor; }
    public void setRuptureRiskFactor(BigDecimal ruptureRiskFactor) { this.ruptureRiskFactor = ruptureRiskFactor; }

    public String getHazardCode() { return hazardCode; }
    public void setHazardCode(String hazardCode) { this.hazardCode = hazardCode; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    /**
     * Converts the analysis result into a RepairOrder entity for DB persistence.
     */
    public RepairOrder toRepairOrder() {
        RepairOrder ro = new RepairOrder();
        ro.setSegmentId(segment != null ? segment.getSegmentId() : 0);
        ro.setHazardCode(hazardCode);
        ro.setIntegrityScore(pciScore);
        ro.setIntegrityGrade(integrityGrade);
        ro.setRuptureRiskFactor(ruptureRiskFactor);
        ro.setActionRequired(recommendedAction);
        ro.setRepairDate(new java.sql.Timestamp(System.currentTimeMillis()));
        return ro;
    }

    @Override
    public String toString() {
        return String.format("IntegrityResult[Segment=%s, PCI=%.2f (%s), RRF=%.4f, Hazard=%s, Action=%s]",
                segment != null ? segment.getSegmentCode() : "N/A",
                pciScore != null ? pciScore.doubleValue() : 0.0,
                integrityGrade,
                ruptureRiskFactor != null ? ruptureRiskFactor.doubleValue() : 0.0,
                hazardCode,
                recommendedAction);
    }
}
