package com.rental.repository.sqlserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Connection settings for SQL Server, read from the file db.properties
 * (placed in the project root folder, next to "src"). See db.properties.example.
 */
public class SqlServerConfig {

    private final String server;
    private final String instance;
    private final String port;
    private final String database;
    private final String user;
    private final String password;
    private final String explicitUrl;
    private final boolean windowsAuth;

    public SqlServerConfig(String server, String instance, String port, String database,
                           String user, String password, String explicitUrl) {
        this(server, instance, port, database, user, password, explicitUrl, false);
    }

    public SqlServerConfig(String server, String instance, String port, String database,
                           String user, String password, String explicitUrl, boolean windowsAuth) {
        this.windowsAuth = windowsAuth;
        this.server = server;
        this.instance = instance;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.explicitUrl = explicitUrl;
        if (!database.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Database name may contain only letters, digits and _");
        }
    }

    /** Loads db.properties from the given file path (default: db.properties in the working folder). */
    public static SqlServerConfig load(String path) {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path + " - copy db.properties.example to "
                    + "db.properties (project root folder) and fill in your SQL Server login", e);
        }
        return new SqlServerConfig(
                p.getProperty("server", "localhost").trim(),
                p.getProperty("instance", "").trim(),
                p.getProperty("port", "1433").trim(),
                p.getProperty("database", "VehicleRental").trim(),
                p.getProperty("user", "sa").trim(),
                p.getProperty("password", ""),
                p.getProperty("url", "").trim(),
                "windows".equalsIgnoreCase(p.getProperty("auth", "sql").trim()));
    }

    /** JDBC url of the application database. */
    public String url() {
        return buildUrl(database);
    }

    /** JDBC url of the "master" database (used only to create our database the first time). */
    public String masterUrl() {
        return buildUrl("master");
    }

    private String buildUrl(String databaseName) {
        if (!explicitUrl.isEmpty()) {
            // a full url was given; only the database name part is replaced
            String base = explicitUrl.replaceAll("(?i);databaseName=[^;]*", "");
            return base + ";databaseName=" + databaseName + (windowsAuth ? ";integratedSecurity=true" : "");
        }
        StringBuilder sb = new StringBuilder("jdbc:sqlserver://").append(server);
        if (!instance.isEmpty()) {
            sb.append('\\').append(instance);       // named instance, e.g. localhost\SQLEXPRESS
        } else {
            sb.append(':').append(port);
        }
        sb.append(";databaseName=").append(databaseName);
        sb.append(";encrypt=true;trustServerCertificate=true");
        if (windowsAuth) {
            sb.append(";integratedSecurity=true");   // log in as the current Windows user
        }
        return sb.toString();
    }

    /** True when the Windows login of the current user is used (no user name / password). */
    public boolean isWindowsAuth() { return windowsAuth; }

    public String getDatabase() { return database; }
    public String getUser() { return user; }
    public String getPassword() { return password; }
}
