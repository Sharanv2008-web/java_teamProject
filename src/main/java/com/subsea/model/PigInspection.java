package com.subsea.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model representing in-line PIG inspection telemetry from 'pig_inspections' table.
 */
public class PigInspection {

    private int inspectionId;
    private int segmentId;
    private BigDecimal wallLossPercent;
    private BigDecimal cathodicVoltageV;
    private BigDecimal internalPressurePsi;
    private Timestamp inspectionDate;

    public PigInspection() {}

    public PigInspection(int inspectionId, int segmentId, BigDecimal wallLossPercent,
                         BigDecimal cathodicVoltageV, BigDecimal internalPressurePsi,
                         Timestamp inspectionDate) {
        this.inspectionId = inspectionId;
        this.segmentId = segmentId;
        this.wallLossPercent = wallLossPercent;
        this.cathodicVoltageV = cathodicVoltageV;
        this.internalPressurePsi = internalPressurePsi;
        this.inspectionDate = inspectionDate;
    }

    public int getInspectionId() { return inspectionId; }
    public void setInspectionId(int inspectionId) { this.inspectionId = inspectionId; }

    public int getSegmentId() { return segmentId; }
    public void setSegmentId(int segmentId) { this.segmentId = segmentId; }

    public BigDecimal getWallLossPercent() { return wallLossPercent; }
    public void setWallLossPercent(BigDecimal wallLossPercent) { this.wallLossPercent = wallLossPercent; }

    public BigDecimal getCathodicVoltageV() { return cathodicVoltageV; }
    public void setCathodicVoltageV(BigDecimal cathodicVoltageV) { this.cathodicVoltageV = cathodicVoltageV; }

    public BigDecimal getInternalPressurePsi() { return internalPressurePsi; }
    public void setInternalPressurePsi(BigDecimal internalPressurePsi) { this.internalPressurePsi = internalPressurePsi; }

    public Timestamp getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(Timestamp inspectionDate) { this.inspectionDate = inspectionDate; }

    @Override
    public String toString() {
        return String.format("PigInspection[ID=%d, SegmentID=%d, WallLoss=%.2f%%, CathodicV=%.2fV, Pressure=%.1fpsi, Date=%s]",
                inspectionId, segmentId,
                wallLossPercent != null ? wallLossPercent.doubleValue() : 0.0,
                cathodicVoltageV != null ? cathodicVoltageV.doubleValue() : 0.0,
                internalPressurePsi != null ? internalPressurePsi.doubleValue() : 0.0,
                inspectionDate);
    }
}
