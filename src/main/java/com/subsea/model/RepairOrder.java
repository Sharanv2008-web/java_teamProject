package com.subsea.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model representing actionable maintenance and hazard remediation orders from 'repair_orders' table.
 */
public class RepairOrder {

    private int repairId;
    private int segmentId;
    private String hazardCode;
    private BigDecimal integrityScore;
    private String integrityGrade;
    private BigDecimal ruptureRiskFactor;
    private String actionRequired;
    private Timestamp repairDate;

    public RepairOrder() {}

    public RepairOrder(int repairId, int segmentId, String hazardCode,
                       BigDecimal integrityScore, String integrityGrade,
                       BigDecimal ruptureRiskFactor, String actionRequired,
                       Timestamp repairDate) {
        this.repairId = repairId;
        this.segmentId = segmentId;
        this.hazardCode = hazardCode;
        this.integrityScore = integrityScore;
        this.integrityGrade = integrityGrade;
        this.ruptureRiskFactor = ruptureRiskFactor;
        this.actionRequired = actionRequired;
        this.repairDate = repairDate;
    }

    public int getRepairId() { return repairId; }
    public void setRepairId(int repairId) { this.repairId = repairId; }

    public int getSegmentId() { return segmentId; }
    public void setSegmentId(int segmentId) { this.segmentId = segmentId; }

    public String getHazardCode() { return hazardCode; }
    public void setHazardCode(String hazardCode) { this.hazardCode = hazardCode; }

    public BigDecimal getIntegrityScore() { return integrityScore; }
    public void setIntegrityScore(BigDecimal integrityScore) { this.integrityScore = integrityScore; }

    public String getIntegrityGrade() { return integrityGrade; }
    public void setIntegrityGrade(String integrityGrade) { this.integrityGrade = integrityGrade; }

    public BigDecimal getRuptureRiskFactor() { return ruptureRiskFactor; }
    public void setRuptureRiskFactor(BigDecimal ruptureRiskFactor) { this.ruptureRiskFactor = ruptureRiskFactor; }

    public String getActionRequired() { return actionRequired; }
    public void setActionRequired(String actionRequired) { this.actionRequired = actionRequired; }

    public Timestamp getRepairDate() { return repairDate; }
    public void setRepairDate(Timestamp repairDate) { this.repairDate = repairDate; }

    @Override
    public String toString() {
        return String.format("RepairOrder[ID=%d, SegmentID=%d, Hazard=%s, Score=%.2f, Grade=%s, Risk=%.4f, Action=%s, Date=%s]",
                repairId, segmentId, hazardCode,
                integrityScore != null ? integrityScore.doubleValue() : 0.0,
                integrityGrade,
                ruptureRiskFactor != null ? ruptureRiskFactor.doubleValue() : 0.0,
                actionRequired, repairDate);
    }
}
