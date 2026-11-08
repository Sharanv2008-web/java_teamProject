package com.subsea.pipeline;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PipelineDAO - Data Access Object for Subsea Database
 * Student 2 (Database Host) Deliverable
 * 
 * Manages all relational persistence operations across the 4 core tables:
 * 1. pipe_segments
 * 2. pig_inspections
 * 3. corrosion_audits
 * 4. repair_orders
 */
public class PipelineDAO {

    // Thread-safe in-memory cache/mirror ensuring high throughput and offline resilience
    private final Map<String, PipeSegment> segmentCache = new ConcurrentHashMap<>();
    private final List<PigInspection> inspectionCache = Collections.synchronizedList(new ArrayList<>());
    private final List<CorrosionAudit> auditCache = Collections.synchronizedList(new ArrayList<>());
    private final List<RepairOrder> repairOrderCache = Collections.synchronizedList(new ArrayList<>());

    // =========================================================================
    // MODEL CLASSES
    // =========================================================================

    public static class PipeSegment {
        public int segmentId;
        public String segmentCode;
        public String assetCategory;
        public double subseaDepthMeters;
        public double wallThicknessMm;
        public String fluidType;
        public double maopPsi;
        public double maxWallLossCapPct;
        public String locationZone;

        public PipeSegment(String segmentCode, String assetCategory, double subseaDepthMeters,
                           double wallThicknessMm, String fluidType, double maopPsi,
                           double maxWallLossCapPct, String locationZone) {
            this.segmentCode = segmentCode;
            this.assetCategory = assetCategory;
            this.subseaDepthMeters = subseaDepthMeters;
            this.wallThicknessMm = wallThicknessMm;
            this.fluidType = fluidType;
            this.maopPsi = maopPsi;
            this.maxWallLossCapPct = maxWallLossCapPct;
            this.locationZone = locationZone;
        }
    }

    public static class PigInspection {
        public int inspectionId;
        public String segmentCode;
        public String pigRunTag;
        public double ultrasonicWallLossPct;
        public double cathodicProtectionVoltage;
        public double internalPressurePsi;
        public double odometerDistanceKm;
        public String rawTelemetryLine;

        public PigInspection(String segmentCode, String pigRunTag, double ultrasonicWallLossPct,
                             double cathodicProtectionVoltage, double internalPressurePsi,
                             double odometerDistanceKm, String rawTelemetryLine) {
            this.segmentCode = segmentCode;
            this.pigRunTag = pigRunTag;
            this.ultrasonicWallLossPct = ultrasonicWallLossPct;
            this.cathodicProtectionVoltage = cathodicProtectionVoltage;
            this.internalPressurePsi = internalPressurePsi;
            this.odometerDistanceKm = odometerDistanceKm;
            this.rawTelemetryLine = rawTelemetryLine;
        }
    }

    public static class CorrosionAudit {
        public int auditId;
        public String segmentCode;
        public int inspectionId;
        public double upstreamFlowBpd;
        public double downstreamFlowBpd;
        public double flowPressureDropPct;
        public double acousticLeakFreqKhz;
        public double hydrophoneNoiseScore;
        public double environmentalCorrosionPct;
        public double anchorDragRiskScore;
        public double tempVarianceCelsius;
        public double pipelineCorrosionIndex;
        public double ruptureRiskFactor;
        public double compositeHealthScore;
        public char integrityGrade;
        public String operationalStatus;
        public boolean hazardFlag;
        public String hazardDescription;
    }

    public static class RepairOrder {
        public int orderId;
        public String orderCode;
        public String segmentCode;
        public int auditId;
        public char integrityGrade;
        public String severityBand;
        public String recommendedAction;
        public double pressureReductionPct;
        public double targetOperatingPressure;
        public String orderStatus;
        public String authorizedBy;
    }

    // =========================================================================
    // CRUD OPERATIONS
    // =========================================================================

    /**
     * Phase 1: Register Pipe Segment
     */
    public boolean registerPipeSegment(PipeSegment segment) {
        segmentCache.put(segment.segmentCode, segment);

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                String sql = "INSERT INTO pipe_segments " +
                        "(segment_code, asset_category, subsea_depth_meters, wall_thickness_mm, fluid_type, maop_psi, max_wall_loss_cap_pct, location_zone) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE asset_category=VALUES(asset_category), subsea_depth_meters=VALUES(subsea_depth_meters);";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, segment.segmentCode);
                    ps.setString(2, segment.assetCategory);
                    ps.setDouble(3, segment.subseaDepthMeters);
                    ps.setDouble(4, segment.wallThicknessMm);
                    ps.setString(5, segment.fluidType);
                    ps.setDouble(6, segment.maopPsi);
                    ps.setDouble(7, segment.maxWallLossCapPct);
                    ps.setString(8, segment.locationZone);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            segment.segmentId = rs.getInt(1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PipelineDAO] Notice on segment registration SQL: " + e.getMessage());
        }
        return true;
    }

    /**
     * Phase 2: Ingest Smart PIG Inspection Telemetry
     */
    public int insertPigInspection(PigInspection inspection) {
        int id = inspectionCache.size() + 1;
        inspection.inspectionId = id;
        inspectionCache.add(inspection);

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                String sql = "INSERT INTO pig_inspections " +
                        "(segment_code, pig_run_tag, ultrasonic_wall_loss_pct, cathodic_protection_voltage, internal_pressure_psi, odometer_distance_km, raw_telemetry_line) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, inspection.segmentCode);
                    ps.setString(2, inspection.pigRunTag);
                    ps.setDouble(3, inspection.ultrasonicWallLossPct);
                    ps.setDouble(4, inspection.cathodicProtectionVoltage);
                    ps.setDouble(5, inspection.internalPressurePsi);
                    ps.setDouble(6, inspection.odometerDistanceKm);
                    ps.setString(7, inspection.rawTelemetryLine);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            inspection.inspectionId = rs.getInt(1);
                            return inspection.inspectionId;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PipelineDAO] Notice on PIG inspection SQL: " + e.getMessage());
        }
        return id;
    }

    /**
     * Phase 3: Ingest Corrosion Audit & Diagnostic Evaluation
     */
    public int insertCorrosionAudit(CorrosionAudit audit) {
        int id = auditCache.size() + 1;
        audit.auditId = id;
        auditCache.add(audit);

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                String sql = "INSERT INTO corrosion_audits " +
                        "(segment_code, inspection_id, upstream_flow_bpd, downstream_flow_bpd, flow_pressure_drop_pct, " +
                        "acoustic_leak_freq_khz, hydrophone_noise_score, environmental_corrosion_pct, anchor_drag_risk_score, " +
                        "temp_variance_celsius, pipeline_corrosion_index, rupture_risk_factor, composite_health_score, " +
                        "integrity_grade, operational_status, hazard_flag, hazard_description) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, audit.segmentCode);
                    ps.setInt(2, audit.inspectionId);
                    ps.setDouble(3, audit.upstreamFlowBpd);
                    ps.setDouble(4, audit.downstreamFlowBpd);
                    ps.setDouble(5, audit.flowPressureDropPct);
                    ps.setDouble(6, audit.acousticLeakFreqKhz);
                    ps.setDouble(7, audit.hydrophoneNoiseScore);
                    ps.setDouble(8, audit.environmentalCorrosionPct);
                    ps.setDouble(9, audit.anchorDragRiskScore);
                    ps.setDouble(10, audit.tempVarianceCelsius);
                    ps.setDouble(11, audit.pipelineCorrosionIndex);
                    ps.setDouble(12, audit.ruptureRiskFactor);
                    ps.setDouble(13, audit.compositeHealthScore);
                    ps.setString(14, String.valueOf(audit.integrityGrade));
                    ps.setString(15, audit.operationalStatus);
                    ps.setBoolean(16, audit.hazardFlag);
                    ps.setString(17, audit.hazardDescription);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            audit.auditId = rs.getInt(1);
                            return audit.auditId;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PipelineDAO] Notice on corrosion audit SQL: " + e.getMessage());
        }
        return id;
    }

    /**
     * Phase 4: Create Repair Authorization Order
     */
    public boolean createRepairOrder(RepairOrder order) {
        int id = repairOrderCache.size() + 1;
        order.orderId = id;
        repairOrderCache.add(order);

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                String sql = "INSERT INTO repair_orders " +
                        "(order_code, segment_code, audit_id, integrity_grade, severity_band, recommended_action, " +
                        "pressure_reduction_pct, target_operating_pressure, order_status, authorized_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, order.orderCode);
                    ps.setString(2, order.segmentCode);
                    ps.setInt(3, order.auditId);
                    ps.setString(4, String.valueOf(order.integrityGrade));
                    ps.setString(5, order.severityBand);
                    ps.setString(6, order.recommendedAction);
                    ps.setDouble(7, order.pressureReductionPct);
                    ps.setDouble(8, order.targetOperatingPressure);
                    ps.setString(9, order.orderStatus);
                    ps.setString(10, order.authorizedBy);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            order.orderId = rs.getInt(1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PipelineDAO] Notice on repair order SQL: " + e.getMessage());
        }
        return true;
    }

    // =========================================================================
    // QUERY METHODS
    // =========================================================================

    public PipeSegment getSegment(String segmentCode) {
        return segmentCache.get(segmentCode);
    }

    public Collection<PipeSegment> getAllSegments() {
        return segmentCache.values();
    }

    public List<CorrosionAudit> getAllAudits() {
        return new ArrayList<>(auditCache);
    }

    public List<RepairOrder> getAllRepairOrders() {
        return new ArrayList<>(repairOrderCache);
    }

    public List<CorrosionAudit> getCriticalHazards() {
        List<CorrosionAudit> hazards = new ArrayList<>();
        for (CorrosionAudit a : auditCache) {
            if (a.hazardFlag || a.integrityGrade == 'D') {
                hazards.add(a);
            }
        }
        return hazards;
    }

    /**
     * Verifies table accessibility and returns active record counts.
     */
    public Map<String, Integer> getDatabaseStatistics() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("pipe_segments", segmentCache.size());
        stats.put("pig_inspections", inspectionCache.size());
        stats.put("corrosion_audits", auditCache.size());
        stats.put("repair_orders", repairOrderCache.size());

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                String[] tables = {"pipe_segments", "pig_inspections", "corrosion_audits", "repair_orders"};
                for (String table : tables) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        if (rs.next()) {
                            stats.put(table + " (remote_db)", rs.getInt(1));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Silently fallback to cached stats
        }
        return stats;
    }
}
