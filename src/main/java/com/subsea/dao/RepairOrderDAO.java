package com.subsea.dao;

import com.subsea.config.DBConnection;
import com.subsea.model.RepairOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for 'repair_orders' table.
 */
public class RepairOrderDAO {

    private static final Logger LOGGER = Logger.getLogger(RepairOrderDAO.class.getName());

    private static final String SELECT_ALL =
            "SELECT repair_id, segment_id, hazard_code, integrity_score, integrity_grade, rupture_risk_factor, action_required, repair_date " +
            "FROM repair_orders ORDER BY repair_id ASC";

    private static final String SELECT_BY_SEGMENT =
            "SELECT repair_id, segment_id, hazard_code, integrity_score, integrity_grade, rupture_risk_factor, action_required, repair_date " +
            "FROM repair_orders WHERE segment_id = ? ORDER BY repair_date DESC, repair_id DESC";

    private static final String CHECK_EXISTS =
            "SELECT COUNT(*) FROM repair_orders WHERE segment_id = ? AND hazard_code = ? " +
            "AND repair_date >= NOW() - INTERVAL 1 HOUR";

    private static final String INSERT_REPAIR_ORDER =
            "INSERT INTO repair_orders (segment_id, hazard_code, integrity_score, integrity_grade, rupture_risk_factor, action_required) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    public List<RepairOrder> getAllRepairOrders() throws SQLException {
        List<RepairOrder> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving repair orders: {0}", e.getMessage());
            throw e;
        }
        return list;
    }

    public List<RepairOrder> getRepairOrdersBySegmentId(int segmentId) throws SQLException {
        List<RepairOrder> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_SEGMENT)) {
            ps.setInt(1, segmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving repair orders for segment " + segmentId + ": {0}", e.getMessage());
            throw e;
        }
        return list;
    }

    /**
     * Checks if an identical repair order has already been created within the last hour.
     * Prevents database clutter from rapid repeated application executions.
     */
    public boolean existsRecentOrder(int segmentId, String hazardCode) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(CHECK_EXISTS)) {
            ps.setInt(1, segmentId);
            ps.setString(2, hazardCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error checking duplicate repair order: {0}", e.getMessage());
        }
        return false;
    }

    public int insertRepairOrder(RepairOrder order) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_REPAIR_ORDER, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, order.getSegmentId());
            ps.setString(2, order.getHazardCode());
            ps.setBigDecimal(3, order.getIntegrityScore());
            ps.setString(4, order.getIntegrityGrade());
            ps.setBigDecimal(5, order.getRuptureRiskFactor());
            ps.setString(6, order.getActionRequired());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        order.setRepairId(keys.getInt(1));
                    }
                }
            }
            return affected;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting repair order: {0}", e.getMessage());
            throw e;
        }
    }

    private RepairOrder mapResultSet(ResultSet rs) throws SQLException {
        return new RepairOrder(
                rs.getInt("repair_id"),
                rs.getInt("segment_id"),
                rs.getString("hazard_code"),
                rs.getBigDecimal("integrity_score"),
                rs.getString("integrity_grade"),
                rs.getBigDecimal("rupture_risk_factor"),
                rs.getString("action_required"),
                rs.getTimestamp("repair_date")
        );
    }
}
