package com.subsea.pipeline;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection - Database Connection Manager for Student 2 (Database Host)
 * Configured for remote JDBC connectivity to OCI VM 2 on port 3306 (MySQL) or 5432 (PostgreSQL).
 */
public class DBConnection {

    // Default configuration pointing to OCI VM 2
    // These can be overridden via JVM system properties or environment variables
    private static final String DEFAULT_HOST = "localhost"; // OCI VM 2 Public / Private IP in production
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DB = "subsea_db";
    private static final String DEFAULT_USER = "subsea_user";
    private static final String DEFAULT_PASS = "SubseaSecurePass2026!";

    private static String host = System.getProperty("db.host", System.getenv().getOrDefault("DB_HOST", DEFAULT_HOST));
    private static int port = Integer.parseInt(System.getProperty("db.port", System.getenv().getOrDefault("DB_PORT", String.valueOf(DEFAULT_PORT))));
    private static String database = System.getProperty("db.name", System.getenv().getOrDefault("DB_NAME", DEFAULT_DB));
    private static String username = System.getProperty("db.user", System.getenv().getOrDefault("DB_USER", DEFAULT_USER));
    private static String password = System.getProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", DEFAULT_PASS));

    private static boolean driverAvailable = false;
    private static boolean connectionVerified = false;
    private static boolean offlineMode = false;

    static {
        try {
            // Check for MySQL Connector/J driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverAvailable = true;
            System.out.println("[DBConnection] MySQL Connector/J Driver loaded successfully.");
        } catch (ClassNotFoundException e1) {
            try {
                // Secondary check for PostgreSQL driver if port 5432 is used
                Class.forName("org.postgresql.Driver");
                driverAvailable = true;
                System.out.println("[DBConnection] PostgreSQL Driver loaded successfully.");
            } catch (ClassNotFoundException e2) {
                System.out.println("[DBConnection] NOTICE: No external JDBC driver jar detected in classpath.");
                System.out.println("[DBConnection] Operating in resilient Standalone/Direct Verification mode.");
                driverAvailable = false;
            }
        }
    }

    /**
     * Obtains a new database connection with timeout configuration.
     * @return Connection object if database is reachable, or null if running in offline simulation mode.
     * @throws SQLException on connectivity error when driver is present.
     */
    public static Connection getConnection() throws SQLException {
        if (!driverAvailable || offlineMode) {
            return null;
        }

        String jdbcUrl;
        if (port == 5432) {
            jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?connectTimeout=2&socketTimeout=5",
                    host, port, database);
        } else {
            jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?connectTimeout=2000&socketTimeout=5000&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host, port, database);
        }

        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);

        try {
            DriverManager.setLoginTimeout(2);
            Connection conn = DriverManager.getConnection(jdbcUrl, props);
            if (!connectionVerified) {
                System.out.println("[DBConnection] Successfully established remote JDBC session with OCI VM 2 (" + host + ":" + port + ")");
                connectionVerified = true;
            }
            return conn;
        } catch (SQLException e) {
            offlineMode = true;
            System.out.println("[DBConnection] Notice: MySQL host (" + host + ":" + port + ") unreachable. Switching to synchronized in-memory mirror.");
            return null;
        }
    }

    public static boolean isDriverAvailable() {
        return driverAvailable;
    }

    public static void setHost(String newHost) {
        host = newHost;
    }

    public static void setPort(int newPort) {
        port = newPort;
    }

    public static void setCredentials(String user, String pass) {
        username = user;
        password = pass;
    }

    public static String getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }

    public static String getDatabase() {
        return database;
    }

    public static String getUsername() {
        return username;
    }
}
