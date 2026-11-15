package com.subsea.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DBConnection provides centralized and robust JDBC database connectivity for the
 * Subsea Pipeline Integrity Monitoring System.
 *
 * Supports:
 * 1. Remote / Localhost MySQL Server on port 3306.
 * 2. Automatic, seamless In-Memory Database fallback for standalone localhost execution.
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

    private static volatile boolean useInMemoryFallback = false;
    private static final String IN_MEMORY_URL = "jdbc:h2:mem:subsea_pipeline;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static boolean inMemoryInitialized = false;

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
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000",
            host, port, database
        );

        // Explicitly register MySQL and H2 JDBC drivers
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.INFO, "MySQL JDBC Driver not detected. Checking secondary embedded driver.");
        }

        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {}
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
     * Obtains a database connection.
     * Tries live MySQL first; if unreachable, gracefully connects to in-memory engine.
     */
    public static Connection getConnection() throws SQLException {
        if (!useInMemoryFallback) {
            try {
                DriverManager.setLoginTimeout(5);
                return DriverManager.getConnection(jdbcUrl, username, password);
            } catch (Exception e) {
                // MySQL is offline or not reachable
                useInMemoryFallback = true;
                System.out.println("[DBConnection] Notice: MySQL on " + host + ":" + port + " is unreachable: " + e.getMessage());
                System.out.println("[DBConnection] Activating Resilient In-Memory Subsea Database for local execution.");
            }
        }

        Connection conn = DriverManager.getConnection(IN_MEMORY_URL, "sa", "");
        if (!inMemoryInitialized) {
            initializeInMemorySchema(conn);
            inMemoryInitialized = true;
        }
        return conn;
    }

    private static synchronized void initializeInMemorySchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS pipe_segments (" +
                    "segment_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "segment_code VARCHAR(30) UNIQUE NOT NULL, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "subsea_depth_m DECIMAL(10,2) NOT NULL, " +
                    "wall_thickness_mm DECIMAL(10,2) NOT NULL, " +
                    "fluid_type VARCHAR(50) NOT NULL, " +
                    "maop_psi DECIMAL(10,2) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS pig_inspections (" +
                    "inspection_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "segment_id INT NOT NULL, " +
                    "wall_loss_percent DECIMAL(5,2) NOT NULL, " +
                    "cathodic_voltage_v DECIMAL(5,3) NOT NULL, " +
                    "internal_pressure_psi DECIMAL(10,2) NOT NULL, " +
                    "inspection_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS corrosion_audits (" +
                    "audit_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "segment_id INT NOT NULL, " +
                    "upstream_flow_bpd DECIMAL(12,2) NOT NULL, " +
                    "downstream_flow_bpd DECIMAL(12,2) NOT NULL, " +
                    "acoustic_leak_hz DECIMAL(10,2) NOT NULL, " +
                    "hydrophone_noise_score DECIMAL(5,2) NOT NULL, " +
                    "audit_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS repair_orders (" +
                    "repair_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "segment_id INT NOT NULL, " +
                    "hazard_code VARCHAR(100) NOT NULL, " +
                    "integrity_score DECIMAL(5,2) NOT NULL, " +
                    "integrity_grade VARCHAR(10) NOT NULL, " +
                    "rupture_risk_factor DECIMAL(10,4) NOT NULL, " +
                    "action_required VARCHAR(255) NOT NULL, " +
                    "repair_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Populate baseline seed data
            stmt.execute("MERGE INTO pipe_segments KEY(segment_id) VALUES " +
                    "(1, 'PIPE-SUB-00001', 'HIGH_PRESSURE_DEEPWATER', 2500.00, 35.50, 'CRUDE_OIL', 5000.00, CURRENT_TIMESTAMP), " +
                    "(2, 'PIPE-SUB-00002', 'SHALLOW_COASTAL_TRUNK',   150.00,  28.00, 'NATURAL_GAS', 3000.00, CURRENT_TIMESTAMP), " +
                    "(3, 'PIPE-SUB-00003', 'LIQUEFIED_GAS_FEEDER',    800.00,  32.00, 'LIQUEFIED_GAS', 4000.00, CURRENT_TIMESTAMP)");

            stmt.execute("MERGE INTO pig_inspections KEY(inspection_id) VALUES " +
                    "(1, 1, 10.00, 1.050, 3500.00, CURRENT_TIMESTAMP), " +
                    "(2, 2, 20.00, 0.900, 2200.00, CURRENT_TIMESTAMP), " +
                    "(3, 3, 15.00, 1.100, 2800.00, CURRENT_TIMESTAMP)");

            stmt.execute("MERGE INTO corrosion_audits KEY(audit_id) VALUES " +
                    "(1, 1, 10000.00, 9800.00, 120.00, 20.00, CURRENT_TIMESTAMP), " +
                    "(2, 2, 8000.00, 7500.00, 250.00, 40.00, CURRENT_TIMESTAMP), " +
                    "(3, 3, 9000.00, 8900.00, 100.00, 15.00, CURRENT_TIMESTAMP)");

            System.out.println("[DBConnection] In-Memory schema and seed records initialized successfully.");
        } catch (SQLException e) {
            System.err.println("[DBConnection] In-memory initialization notice: " + e.getMessage());
        }
    }

    /**
     * Verifies that the database host is reachable or resilient in-memory mode is ready.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Connection test failed: {0}", e.getMessage());
            return false;
        }
    }

    public static String getHost() { return useInMemoryFallback ? "localhost (in-memory)" : host; }
    public static int getPort() { return port; }
    public static String getDatabase() { return database; }
    public static String getUsername() { return username; }
    public static String getJdbcUrl() { return useInMemoryFallback ? IN_MEMORY_URL : jdbcUrl; }
    public static boolean isInMemory() { return useInMemoryFallback; }
}
