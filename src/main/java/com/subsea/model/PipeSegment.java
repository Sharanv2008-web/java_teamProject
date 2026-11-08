package com.subsea.model;

import java.math.BigDecimal;

/**
 * Model representing an underwater pipeline segment from the 'pipe_segments' table.
 */
public class PipeSegment {

    private int segmentId;
    private String segmentCode;
    private String category;
    private BigDecimal subseaDepthM;
    private BigDecimal wallThicknessMm;
    private String fluidType;
    private BigDecimal maopPsi;

    public PipeSegment() {}

    public PipeSegment(int segmentId, String segmentCode, String category,
                       BigDecimal subseaDepthM, BigDecimal wallThicknessMm,
                       String fluidType, BigDecimal maopPsi) {
        this.segmentId = segmentId;
        this.segmentCode = segmentCode;
        this.category = category;
        this.subseaDepthM = subseaDepthM;
        this.wallThicknessMm = wallThicknessMm;
        this.fluidType = fluidType;
        this.maopPsi = maopPsi;
    }

    public int getSegmentId() { return segmentId; }
    public void setSegmentId(int segmentId) { this.segmentId = segmentId; }

    public String getSegmentCode() { return segmentCode; }
    public void setSegmentCode(String segmentCode) { this.segmentCode = segmentCode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getSubseaDepthM() { return subseaDepthM; }
    public void setSubseaDepthM(BigDecimal subseaDepthM) { this.subseaDepthM = subseaDepthM; }

    public BigDecimal getWallThicknessMm() { return wallThicknessMm; }
    public void setWallThicknessMm(BigDecimal wallThicknessMm) { this.wallThicknessMm = wallThicknessMm; }

    public String getFluidType() { return fluidType; }
    public void setFluidType(String fluidType) { this.fluidType = fluidType; }

    public BigDecimal getMaopPsi() { return maopPsi; }
    public void setMaopPsi(BigDecimal maopPsi) { this.maopPsi = maopPsi; }

    @Override
    public String toString() {
        return String.format("PipeSegment[ID=%d, Code=%s, Category=%s, Depth=%.1fm, Wall=%.2fmm, Fluid=%s, MAOP=%.1fpsi]",
                segmentId, segmentCode, category,
                subseaDepthM != null ? subseaDepthM.doubleValue() : 0.0,
                wallThicknessMm != null ? wallThicknessMm.doubleValue() : 0.0,
                fluidType,
                maopPsi != null ? maopPsi.doubleValue() : 0.0);
    }
}
