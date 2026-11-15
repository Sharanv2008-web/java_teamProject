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
    private static final String DEFAULT_HOST = "localhost"; // OCI VM 2 Public / Private IP or Friend's LAN IP
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DB = "subsea_db";
    private static final String DEFAULT_USER = "subsea_user";
    private static final String DEFAULT_PASS = "SubseaSecurePass2026!";

    private static String host = DEFAULT_HOST;
    private static int port = DEFAULT_PORT;
    private static String database = DEFAULT_DB;
    private static String username = DEFAULT_USER;
    private static String password = DEFAULT_PASS;

    private static boolean driverAvailable = false;
    private static boolean connectionVerified = false;

    static {
        loadConfiguration();

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

    public static synchronized void loadConfiguration() {
        // 1. Try reading from config/application.properties
        try {
            java.io.File confFile = new java.io.File("config/application.properties");
            if (confFile.exists()) {
                Properties p = new Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(confFile)) {
                    p.load(fis);
                    if (p.getProperty("db.host") != null && !p.getProperty("db.host").trim().isEmpty()) {
                        host = p.getProperty("db.host").trim();
                    }
                    if (p.getProperty("db.port") != null && !p.getProperty("db.port").trim().isEmpty()) {
                        port = Integer.parseInt(p.getProperty("db.port").trim());
                    }
                    if (p.getProperty("db.name") != null && !p.getProperty("db.name").trim().isEmpty()) {
                        database = p.getProperty("db.name").trim();
                    }
                    if (p.getProperty("db.user") != null && !p.getProperty("db.user").trim().isEmpty()) {
                        username = p.getProperty("db.user").trim();
                    } else if (p.getProperty("db.username") != null && !p.getProperty("db.username").trim().isEmpty()) {
                        username = p.getProperty("db.username").trim();
                    }
                    if (p.getProperty("db.password") != null && !p.getProperty("db.password").trim().isEmpty()) {
                        password = p.getProperty("db.password").trim();
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. Environment variables take precedence
        if (System.getenv("DB_HOST") != null && !System.getenv("DB_HOST").trim().isEmpty()) {
            host = System.getenv("DB_HOST").trim();
        }
        if (System.getenv("DB_PORT") != null && !System.getenv("DB_PORT").trim().isEmpty()) {
            port = Integer.parseInt(System.getenv("DB_PORT").trim());
        }
        if (System.getenv("DB_NAME") != null && !System.getenv("DB_NAME").trim().isEmpty()) {
            database = System.getenv("DB_NAME").trim();
        }
        if (System.getenv("DB_USER") != null && !System.getenv("DB_USER").trim().isEmpty()) {
            username = System.getenv("DB_USER").trim();
        }
        if (System.getenv("DB_PASSWORD") != null && !System.getenv("DB_PASSWORD").trim().isEmpty()) {
            password = System.getenv("DB_PASSWORD").trim();
        }

        // 3. JVM System properties take highest precedence
        if (System.getProperty("db.host") != null && !System.getProperty("db.host").trim().isEmpty()) {
            host = System.getProperty("db.host").trim();
        }
        if (System.getProperty("db.port") != null && !System.getProperty("db.port").trim().isEmpty()) {
            port = Integer.parseInt(System.getProperty("db.port").trim());
        }
        if (System.getProperty("db.name") != null && !System.getProperty("db.name").trim().isEmpty()) {
            database = System.getProperty("db.name").trim();
        }
        if (System.getProperty("db.user") != null && !System.getProperty("db.user").trim().isEmpty()) {
            username = System.getProperty("db.user").trim();
        }
        if (System.getProperty("db.password") != null && !System.getProperty("db.password").trim().isEmpty()) {
            password = System.getProperty("db.password").trim();
        }
    }

    /**
     * Obtains a new database connection with timeout configuration.
     * @return Connection object if database is reachable, or null if running in offline simulation mode.
     * @throws SQLException on connectivity error when driver is present.
     */
    public static Connection getConnection() throws SQLException {
        if (!driverAvailable) {
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
            DriverManager.setLoginTimeout(5);
            Connection conn = DriverManager.getConnection(jdbcUrl, props);
            if (!connectionVerified) {
                System.out.println("[DBConnection] Successfully established remote JDBC session with Database Host (" + host + ":" + port + ")");
                connectionVerified = true;
            }
            return conn;
        } catch (SQLException e) {
            System.err.println("[DBConnection] Connection attempt to MySQL at " + host + ":" + port + "/" + database + " (user: " + username + ") failed: " + e.getMessage());
            System.out.println("[DBConnection] Operating with synchronized in-memory mirror. (To connect to remote laptop, verify IP, firewall port 3306, and MySQL user grants)");
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
