package org.dataprocessing.backend.database;

import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

/**
 * Class that runs and connects to the POR SQL Server
 *
 * @author Nicholas Curl
 */
public class SqlServer {

    /**
     * The instance of the logger
     */
    private static final Logger     logger         = LogManager.getLogger();
    /**
     * The connection string for the database
     */
    private static final String     dbString       = "jdbc:sqlserver://localhost\\SQLEXPRESS;database=POR3";
    /**
     * The username to login into the server
     */
    private static final String     user           = "dataprocessing";
    /**
     * The password to login into the server
     */
    private static final String     pwd            = "dataprocessing";
    /**
     * Create a static instance
     */
    private static final SqlServer  serverInstance = new SqlServer();
    /**
     * The connection to the server
     */
    private              Connection connection     = null;
    private              boolean    closed;

    /**
     * The constructor for the class that registers the Microsoft SQL Server driver
     */
    public SqlServer() {
        closed = true;
        try {
            DriverManager.registerDriver(new SQLServerDriver());
        }
        catch (SQLException e) {
            logger.fatal("Unable to register driver", e);
            System.exit(-1);
        }
    }

    /**
     * Get the instance of the class
     *
     * @return The instance of the class
     */
    public static SqlServer getInstance() {
        return serverInstance;
    }

    /**
     * Closes the server connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                logger.debug("Closed");
                connection.close();
                closed = true;
            }
        }
        catch (SQLException e) {
            logger.fatal("Unable to close server connection", e);
            System.exit(-1);
        }
    }

    /**
     * Gets the server connection, if one does not exist connect to the server
     *
     * @return The connection to the server
     */
    public Connection getConnection() {
        if (connection == null) {
            return connectToServer();
        }
        else {
            return connection;
        }
    }

    /**
     * Connects to the SQL Server
     *
     * @return The connection to the SQL Server
     */
    public Connection connectToServer() {
        try {
            connection = DriverManager.getConnection(dbString, user, pwd);
            if (connection != null) {
                String connectionString = "Connected\n";
                DatabaseMetaData dm = connection.getMetaData();
                connectionString += "Driver name: " + dm.getDriverName() + "\n";
                connectionString += "Driver version: " + dm.getDriverVersion() + "\n";
                connectionString += "Product name: " + dm.getDatabaseProductName() + "\n";
                connectionString += "Product version: " + dm.getDatabaseProductVersion();
                logger.debug(connectionString);
                closed = false;
                return connection;
            }
            else {
                return null;
            }
        }
        catch (SQLException e) {
            logger.fatal("Unable to to connect to server.", e);
            System.exit(-1);
            return null;
        }
    }

    /**
     * Gets the progress update value of the SQL query
     *
     * @param sql The SQL query
     *
     * @return The value of the progress update based off of row and column count
     */
    public double getLocalProgressUpdate(String sql) {
        ResultSet table = queryServer(sql);
        int rowCount = getQueryCount(sql);
        double progressUpdate = 1.0;
        try {
            ResultSetMetaData tableMeta = table.getMetaData();
            int colCount = tableMeta.getColumnCount();
            progressUpdate = (1 / (double) rowCount) * (1 / (double) colCount);
        }
        catch (SQLException e) {
            logger.fatal("Unable to get local progress.", e);
            System.exit(-1);
        }
        return progressUpdate;
    }

    /**
     * Query the server with the SQL query
     *
     * @param sql The SQL query
     *
     * @return The results of the query
     */
    public ResultSet queryServer(String sql) {
        try {
            Statement statement = connection.createStatement();
            return statement.executeQuery(sql);
        }
        catch (SQLException e) {
            logger.fatal("Unable to process query.", e);
            System.exit(-1);
            return null;
        }
    }

    /**
     * Gets the count of the SQL query
     *
     * @param sql The SQL Query
     *
     * @return The number of results from the query
     */
    public int getQueryCount(String sql) {
        String[] split = sql.split("(?<=SELECT)(.*?)(?=FROM)", 2);
        String countSql = split[0] + " count(*) " + split[1];
        try {
            ResultSet resultSet = queryServer(countSql);
            resultSet.next();
            return resultSet.getInt(1);
        }
        catch (SQLException e) {
            logger.fatal("Unable to get count of query.", e);
            System.exit(-1);
            return -1;
        }
    }

    /**
     * Gets the vendor's name based off of the vendor's ID
     *
     * @param vendNum The vendor's ID
     *
     * @return The vendor's name
     */
    public String getVendorName(int vendNum) {
        String sql = "SELECT VendorName FROM VendorFile WHERE VendorNumber='" + vendNum + "'";
        try {
            ResultSet resultSet = queryServer(sql);
            resultSet.next();
            return resultSet.getString(1);
        }
        catch (SQLException e) {
            logger.fatal("Unable to get vendor name.", e);
            System.exit(-1);
            return null;
        }
    }

    public boolean isClosed() {
        return closed;
    }

}
