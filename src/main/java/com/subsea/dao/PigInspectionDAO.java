package com.subsea.dao;

import com.subsea.config.DBConnection;
import com.subsea.model.PigInspection;

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
 * Data Access Object for 'pig_inspections' table.
 */
public class PigInspectionDAO {

    private static final Logger LOGGER = Logger.getLogger(PigInspectionDAO.class.getName());

    private static final String SELECT_ALL =
            "SELECT inspection_id, segment_id, wall_loss_percent, cathodic_voltage_v, internal_pressure_psi, inspection_date " +
            "FROM pig_inspections ORDER BY inspection_id ASC";

    private static final String SELECT_BY_SEGMENT =
            "SELECT inspection_id, segment_id, wall_loss_percent, cathodic_voltage_v, internal_pressure_psi, inspection_date " +
            "FROM pig_inspections WHERE segment_id = ? ORDER BY inspection_date DESC, inspection_id DESC";

    private static final String SELECT_LATEST_BY_SEGMENT =
            "SELECT inspection_id, segment_id, wall_loss_percent, cathodic_voltage_v, internal_pressure_psi, inspection_date " +
            "FROM pig_inspections WHERE segment_id = ? ORDER BY inspection_date DESC, inspection_id DESC LIMIT 1";

    private static final String INSERT_INSPECTION =
            "INSERT INTO pig_inspections (segment_id, wall_loss_percent, cathodic_voltage_v, internal_pressure_psi) " +
            "VALUES (?, ?, ?, ?)";

    public List<PigInspection> getAllInspections() throws SQLException {
        List<PigInspection> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving pig inspections: {0}", e.getMessage());
            throw e;
        }
        return list;
    }

    public List<PigInspection> getInspectionsBySegmentId(int segmentId) throws SQLException {
        List<PigInspection> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_SEGMENT)) {
            ps.setInt(1, segmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving inspections for segment " + segmentId + ": {0}", e.getMessage());
            throw e;
        }
        return list;
    }

    public Optional<PigInspection> getLatestInspectionBySegmentId(int segmentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_LATEST_BY_SEGMENT)) {
            ps.setInt(1, segmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving latest inspection for segment " + segmentId + ": {0}", e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    public int insertInspection(PigInspection inspection) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_INSPECTION, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, inspection.getSegmentId());
            ps.setBigDecimal(2, inspection.getWallLossPercent());
            ps.setBigDecimal(3, inspection.getCathodicVoltageV());
            ps.setBigDecimal(4, inspection.getInternalPressurePsi());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        inspection.setInspectionId(keys.getInt(1));
                    }
                }
            }
            return affected;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting pig inspection: {0}", e.getMessage());
            throw e;
        }
    }

    private PigInspection mapResultSet(ResultSet rs) throws SQLException {
        return new PigInspection(
                rs.getInt("inspection_id"),
                rs.getInt("segment_id"),
                rs.getBigDecimal("wall_loss_percent"),
                rs.getBigDecimal("cathodic_voltage_v"),
                rs.getBigDecimal("internal_pressure_psi"),
                rs.getTimestamp("inspection_date")
        );
    }
}
