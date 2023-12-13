package pms.database;

import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import pms.common.util.ResourceUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

import static pms.system.PMSManager.applicationProperties;

/**
 * Connection Pool
 * <p>
 * - 데이터베이스 Connection Pool 관리
 */
public class ConnectionPool {
    private static final String POOL_DRIVER_NAME = "jdbc:apache:commons:dbcp:";
    private static final String POOL_NAME = "pms";
    private static final String POOL_JDBC_NAME = POOL_DRIVER_NAME + POOL_NAME;
    private static final Long TEST_TIME = 1000L * 60L * 3L;
    private static final int MIN_IDLE = 10;
    private static final int MAX_IDLE = 30;
    private static final int MAX_TOTAL = 200;
    private static final Long MAX_WAIT_TIME = 1000L * 3L;
    private static final int MAX_WAIT_COUNT = 20;
    private static final Long CONNECTION_MAX_LIFE_TIME = 1000L * 30L;
    private static final Map<Connection, Date> connectionMap = new HashMap<>();
    private static GenericObjectPool<PoolableConnection> connectionPool;

    /**
     * Database Connection Pool Initialization
     */
    public static void initConnectionPool() {
        try {
            ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
                    applicationProperties.getProperty("db.url"),
                    applicationProperties.getProperty("db.user"),
                    applicationProperties.getProperty("db.password")
            );

            PoolableConnectionFactory poolConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
            poolConnectionFactory.setValidationQuery("SELECT 1 FROM DUAL");

            GenericObjectPoolConfig<PoolableConnection> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setTestWhileIdle(true);
            poolConfig.setTimeBetweenEvictionRunsMillis(TEST_TIME);
            poolConfig.setMinIdle(MIN_IDLE);
            poolConfig.setMaxIdle(MAX_IDLE);
            poolConfig.setMaxTotal(MAX_TOTAL);
            poolConfig.setBlockWhenExhausted(true);
            poolConfig.setMaxWaitMillis(MAX_WAIT_TIME);

            connectionPool = new GenericObjectPool<>(poolConnectionFactory, poolConfig);
            poolConnectionFactory.setPool(connectionPool);

            Class.forName("org.apache.commons.dbcp2.PoolingDriver");

            PoolingDriver driver = (PoolingDriver) DriverManager.getDriver(POOL_DRIVER_NAME);
            driver.registerPool(POOL_NAME, connectionPool);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Idle Connection Check
     */
    private static synchronized void checkIdleConnection() {
        Set<Connection> connectionSet = connectionMap.keySet();
        Iterator<Connection> connectionIterator = connectionSet.iterator();
        Long currentTime = new Date().getTime();

        while (connectionIterator.hasNext()) {
            Connection connection = connectionIterator.next();
            Long connectionTime = connectionMap.get(connection).getTime();

            if ((currentTime - connectionTime) > CONNECTION_MAX_LIFE_TIME) {
                connectionIterator.remove();
                removeConnection(connection);
            }
        }
    }

    /**
     * Connection Check
     *
     * @param connection Connection
     * @return Connection
     * @throws SQLException SQLException
     */
    public static Connection checkConnection(Connection connection) throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                return getConnection();
            }

            addConnection(connection);

            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Connection Add
     *
     * @param connection Connection
     */
    private static synchronized void addConnection(Connection connection) {
        try {
            if ((connection == null) || (connection.isClosed())) {
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        connectionMap.put(connection, new Date());
    }

    /**
     * Get New Database Connect
     *
     * @return Connection
     */
    private static Connection getDbConnection() {
        if (connectionPool.getCreatedCount() >= MAX_TOTAL) {
            checkIdleConnection();
        }

        try {
            Connection connection = DriverManager.getConnection(POOL_JDBC_NAME);
            addConnection(connection);

            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get Connection
     *
     * @return Connection
     * @throws SQLException SQLException
     */
    public static Connection getConnection() throws SQLException {
        Connection connection = getDbConnection();

        while ((connection == null) || connection.isClosed()) {
            connection = getDbConnection();
        }

        return connection;
    }

    /**
     * Remove Connection
     *
     * @param connection Connection
     */
    private static synchronized void removeConnection(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            connectionMap.remove(connection);
        }
    }

    /**
     * Close Connection
     *
     * @param connection connection
     */
    public static void closeConnection(Connection connection) {
        removeConnection(connection);
    }
}
