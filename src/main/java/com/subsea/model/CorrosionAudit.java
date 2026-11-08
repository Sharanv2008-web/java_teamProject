package com.subsea.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model representing corrosion and acoustic sensor audit data from 'corrosion_audits' table.
 */
public class CorrosionAudit {

    private int auditId;
    private int segmentId;
    private BigDecimal upstreamFlowBpd;
    private BigDecimal downstreamFlowBpd;
    private BigDecimal acousticLeakHz;
    private BigDecimal hydrophoneNoiseScore;
    private Timestamp auditDate;

    public CorrosionAudit() {}

    public CorrosionAudit(int auditId, int segmentId, BigDecimal upstreamFlowBpd,
                          BigDecimal downstreamFlowBpd, BigDecimal acousticLeakHz,
                          BigDecimal hydrophoneNoiseScore, Timestamp auditDate) {
        this.auditId = auditId;
        this.segmentId = segmentId;
        this.upstreamFlowBpd = upstreamFlowBpd;
        this.downstreamFlowBpd = downstreamFlowBpd;
        this.acousticLeakHz = acousticLeakHz;
        this.hydrophoneNoiseScore = hydrophoneNoiseScore;
        this.auditDate = auditDate;
    }

    public int getAuditId() { return auditId; }
    public void setAuditId(int auditId) { this.auditId = auditId; }

    public int getSegmentId() { return segmentId; }
    public void setSegmentId(int segmentId) { this.segmentId = segmentId; }

    public BigDecimal getUpstreamFlowBpd() { return upstreamFlowBpd; }
    public void setUpstreamFlowBpd(BigDecimal upstreamFlowBpd) { this.upstreamFlowBpd = upstreamFlowBpd; }

    public BigDecimal getDownstreamFlowBpd() { return downstreamFlowBpd; }
    public void setDownstreamFlowBpd(BigDecimal downstreamFlowBpd) { this.downstreamFlowBpd = downstreamFlowBpd; }

    public BigDecimal getAcousticLeakHz() { return acousticLeakHz; }
    public void setAcousticLeakHz(BigDecimal acousticLeakHz) { this.acousticLeakHz = acousticLeakHz; }

    public BigDecimal getHydrophoneNoiseScore() { return hydrophoneNoiseScore; }
    public void setHydrophoneNoiseScore(BigDecimal hydrophoneNoiseScore) { this.hydrophoneNoiseScore = hydrophoneNoiseScore; }

    public Timestamp getAuditDate() { return auditDate; }
    public void setAuditDate(Timestamp auditDate) { this.auditDate = auditDate; }

    /**
     * Calculates flow loss percentage between upstream and downstream meters.
     */
    public double getFlowLossPercent() {
        if (upstreamFlowBpd == null || downstreamFlowBpd == null || upstreamFlowBpd.doubleValue() <= 0) {
            return 0.0;
        }
        double up = upstreamFlowBpd.doubleValue();
        double down = downstreamFlowBpd.doubleValue();
        if (up <= down) return 0.0;
        return ((up - down) / up) * 100.0;
    }

    @Override
    public String toString() {
        return String.format("CorrosionAudit[ID=%d, SegmentID=%d, UpFlow=%.1f, DownFlow=%.1f, LeakHz=%.1fHz, HydrophoneScore=%.1f, Date=%s]",
                auditId, segmentId,
                upstreamFlowBpd != null ? upstreamFlowBpd.doubleValue() : 0.0,
                downstreamFlowBpd != null ? downstreamFlowBpd.doubleValue() : 0.0,
                acousticLeakHz != null ? acousticLeakHz.doubleValue() : 0.0,
                hydrophoneNoiseScore != null ? hydrophoneNoiseScore.doubleValue() : 0.0,
                auditDate);
    }
}
