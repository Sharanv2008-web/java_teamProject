package com.subsea.dao;

import com.subsea.config.DBConnection;
import com.subsea.model.CorrosionAudit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for 'corrosion_audits' table.
 */
public class CorrosionAuditDAO {

    private static final Logger LOGGER = Logger.getLogger(CorrosionAuditDAO.class.getName());

    private static final String SELECT_ALL =
            "SELECT audit_id, segment_id, upstream_flow_bpd, downstream_flow_bpd, acoustic_leak_hz, hydrophone_noise_score, audit_date " +
            "FROM corrosion_audits ORDER BY audit_id ASC";

    private static final String SELECT_BY_SEGMENT =
            "SELECT audit_id, segment_id, upstream_flow_bpd, downstream_flow_bpd, acoustic_leak_hz, hydrophone_noise_score, audit_date " +
            "FROM corrosion_audits WHERE segment_id = ? ORDER BY audit_date DESC, audit_id DESC";

    private static final String SELECT_LATEST_BY_SEGMENT =
            "SELECT audit_id, segment_id, upstream_flow_bpd, downstream_flow_bpd, acoustic_leak_hz, hydrophone_noise_score, audit_date " +
            "FROM corrosion_audits WHERE segment_id = ? ORDER BY audit_date DESC, audit_id DESC LIMIT 1";

    private static final String INSERT_AUDIT =
            "INSERT INTO corrosion_audits (segment_id, upstream_flow_bpd, downstream_flow_bpd, acoustic_leak_hz, hydrophone_noise_score) " +
            "VALUES (?, ?, ?, ?, ?)";

    public List<CorrosionAudit> getAllAudits() throws SQLException {
        List<CorrosionAudit> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving corrosion audits: {0}", e.getMessage());
            throw e;
        }
        return list;
    }

    public List<CorrosionAudit> getAuditsBySegmentId(int segmentId) throws SQLException {
        List<CorrosionAudit> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_SEGMENT)) {
            ps.setInt(1, segmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving audits for segment " + segmentId + ": {0}", e.getMessage());
            throw e;
        }
        return list;
    }

    public Optional<CorrosionAudit> getLatestAuditBySegmentId(int segmentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_LATEST_BY_SEGMENT)) {
            ps.setInt(1, segmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving latest audit for segment " + segmentId + ": {0}", e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    public int insertAudit(CorrosionAudit audit) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_AUDIT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, audit.getSegmentId());
            ps.setBigDecimal(2, audit.getUpstreamFlowBpd());
            ps.setBigDecimal(3, audit.getDownstreamFlowBpd());
            ps.setBigDecimal(4, audit.getAcousticLeakHz());
            ps.setBigDecimal(5, audit.getHydrophoneNoiseScore());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        audit.setAuditId(keys.getInt(1));
                    }
                }
            }
            return affected;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting corrosion audit: {0}", e.getMessage());
            throw e;
        }
    }

    private CorrosionAudit mapResultSet(ResultSet rs) throws SQLException {
        return new CorrosionAudit(
                rs.getInt("audit_id"),
                rs.getInt("segment_id"),
                rs.getBigDecimal("upstream_flow_bpd"),
                rs.getBigDecimal("downstream_flow_bpd"),
                rs.getBigDecimal("acoustic_leak_hz"),
                rs.getBigDecimal("hydrophone_noise_score"),
                rs.getTimestamp("audit_date")
        );
    }
}
