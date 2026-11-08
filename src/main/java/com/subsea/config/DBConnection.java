package com.subsea.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DBConnection provides centralized and robust JDBC database connectivity for the
 * Subsea Pipeline Integrity Monitoring System.
 *
 * It is managed by Student 1 (Java Application Host) to communicate with the
 * MySQL Database Host (managed locally or on OCI VM 2 by Student 2).
 *
 * Configuration Hierarchy:
 * 1. Environment Variables: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
 * 2. External file: config/application.properties (or path specified by -Dconfig.file)
 * 3. Classpath resource: /db.properties
 * 4. Fallback defaults
 */
public class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private static final Properties PROPS = new Properties();

    private static String host;
    private static int port;
    private static String database;
    private static String username;
    private static String password;
    private static String jdbcUrl;

    static {
        loadConfiguration();
    }

    private static synchronized void loadConfiguration() {
        // 1. Load from classpath resource first
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No classpath db.properties found: {0}", e.getMessage());
        }

        // 2. Load from external config/application.properties if available
        String externalPath = System.getProperty("config.file", "config/application.properties");
        try (FileInputStream fin = new FileInputStream(externalPath)) {
            PROPS.load(fin);
            LOGGER.log(Level.INFO, "Loaded external configuration from {0}", externalPath);
        } catch (Exception ignored) {
            // Optional external config file
        }

        // 3. Resolve host, port, database, credentials with Environment Variable overrides
        host = getEnvOrProp("DB_HOST", "db.host", "localhost");
        port = Integer.parseInt(getEnvOrProp("DB_PORT", "db.port", "3306"));
        database = getEnvOrProp("DB_NAME", "db.name", "subsea_pipeline");
        username = getEnvOrProp("DB_USER", "db.username", "subsea_app");
        password = getEnvOrProp("DB_PASSWORD", "db.password", "SubseaApp@2026#ChangeMe");

        // Build JDBC URL
        jdbcUrl = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=10000",
            host, port, database
        );

        // Explicitly register MySQL JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found on classpath!", e);
        }
    }

    private static String getEnvOrProp(String envKey, String propKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        String sysProp = System.getProperty(propKey);
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return sysProp.trim();
        }
        String fileProp = PROPS.getProperty(propKey);
        if (fileProp != null && !fileProp.trim().isEmpty()) {
            return fileProp.trim();
        }
        return defaultValue;
    }

    /**
     * Obtains a new database connection from MySQL Server.
     * Caller is responsible for closing the connection (try-with-resources).
     *
     * @return Connection to subsea_pipeline database
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to MySQL database at {0}:{1}/{2}: {3}",
                    new Object[]{host, port, database, e.getMessage()});
            throw e;
        }
    }

    /**
     * Verifies that the database host is reachable and authentication succeeds.
     *
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Connection test failed: {0}", e.getMessage());
            return false;
        }
    }

    public static String getHost() { return host; }
    public static int getPort() { return port; }
    public static String getDatabase() { return database; }
    public static String getUsername() { return username; }
    public static String getJdbcUrl() { return jdbcUrl; }
}
