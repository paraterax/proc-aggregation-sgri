package com.paratera.sgri.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

/**
 * 连接池管理.
 */
public class ConnectionPool {
	/**
	 * 日志实例.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPool.class);

	/**
	 * 数据源.
	 */
	private static DruidDataSource dataSource;

	/**
	 * 单例模式.
	 */
	private static ConnectionPool ins;

	/**
	 * 默认构造函数.
	 */
	private ConnectionPool() throws Exception {
		init();
	}

	/**
	 * 获取连接池示例, 若初始化异常, 则返回null.
	 */
	public static ConnectionPool getInstance() {
		if (ins == null) {
			synchronized (ConnectionPool.class) {
				if (ins == null) {
					try {
						ins = new ConnectionPool();
					} catch (Exception e) {
						if (dataSource != null) {
							dataSource.close();
						}
						LOGGER.error("数据源初始化失败.");
						ins = null;
					}
				}
			}
		}
		return ins;
	}

	/**
	 * 获取数据库连接.
	 */
	public Connection getConnection() {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
		} catch (SQLException e) {
			LOGGER.error("获取数据库连接错误， 请检查数据库配置参数." + e.toString());
		}
		return conn;
	}

	/**
	 * 关闭连接池.
	 */
	public void close() {
		dataSource.close();
	}

	/**
	 * 初始化数据源.
	 */
	private void init() throws Exception {
		dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(DsConfig.getJDBCConfigMap());
	}
}