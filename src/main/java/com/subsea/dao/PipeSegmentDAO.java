package com.subsea.dao;

import com.subsea.config.DBConnection;
import com.subsea.model.PipeSegment;

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
 * Data Access Object for 'pipe_segments' table.
 */
public class PipeSegmentDAO {

    private static final Logger LOGGER = Logger.getLogger(PipeSegmentDAO.class.getName());

    private static final String SELECT_ALL =
            "SELECT segment_id, segment_code, category, subsea_depth_m, wall_thickness_mm, fluid_type, maop_psi " +
            "FROM pipe_segments ORDER BY segment_id ASC";

    private static final String SELECT_BY_ID =
            "SELECT segment_id, segment_code, category, subsea_depth_m, wall_thickness_mm, fluid_type, maop_psi " +
            "FROM pipe_segments WHERE segment_id = ?";

    private static final String SELECT_BY_CODE =
            "SELECT segment_id, segment_code, category, subsea_depth_m, wall_thickness_mm, fluid_type, maop_psi " +
            "FROM pipe_segments WHERE segment_code = ?";

    private static final String INSERT_SEGMENT =
            "INSERT INTO pipe_segments (segment_code, category, subsea_depth_m, wall_thickness_mm, fluid_type, maop_psi) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    public List<PipeSegment> getAllSegments() throws SQLException {
        List<PipeSegment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving pipe segments: {0}", e.getMessage());
            throw e;
        }
        return list;
    }

    public Optional<PipeSegment> getSegmentById(int segmentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, segmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving segment id " + segmentId + ": {0}", e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    public Optional<PipeSegment> getSegmentByCode(String code) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CODE)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving segment code " + code + ": {0}", e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    public int insertSegment(PipeSegment segment) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SEGMENT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, segment.getSegmentCode());
            ps.setString(2, segment.getCategory());
            ps.setBigDecimal(3, segment.getSubseaDepthM());
            ps.setBigDecimal(4, segment.getWallThicknessMm());
            ps.setString(5, segment.getFluidType());
            ps.setBigDecimal(6, segment.getMaopPsi());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        segment.setSegmentId(keys.getInt(1));
                    }
                }
            }
            return affected;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting pipe segment: {0}", e.getMessage());
            throw e;
        }
    }

    private PipeSegment mapResultSet(ResultSet rs) throws SQLException {
        return new PipeSegment(
                rs.getInt("segment_id"),
                rs.getString("segment_code"),
                rs.getString("category"),
                rs.getBigDecimal("subsea_depth_m"),
                rs.getBigDecimal("wall_thickness_mm"),
                rs.getString("fluid_type"),
                rs.getBigDecimal("maop_psi")
        );
    }
}
