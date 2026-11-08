package com.subsea;

import com.subsea.config.DBConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DBConnectionTest {

    @Test
    @DisplayName("Should successfully connect to MySQL Database Host")
    void testDatabaseConnectivity() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
            assertTrue(conn.isValid(3), "Connection should be valid within 3 seconds");
        }
    }

    @Test
    @DisplayName("testConnection utility method should return true")
    void testConnectionHelperMethod() {
        assertTrue(DBConnection.testConnection(), "DBConnection.testConnection() should return true");
    }

    @Test
    @DisplayName("Configuration properties should not be empty")
    void testConfigurationParameters() {
        assertNotNull(DBConnection.getHost());
        assertFalse(DBConnection.getHost().isEmpty());
        assertTrue(DBConnection.getPort() > 0);
        assertEquals("subsea_pipeline", DBConnection.getDatabase());
        assertNotNull(DBConnection.getUsername());
    }
}
