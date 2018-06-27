package com.paratera.sgri.db;

import java.sql.Connection;

public final class DBTools {
	private static DBTools ins = null;
	private static ConnectionPool pool = null;

	/**
	 * 默认构造函数.
	 */
	private DBTools() {
		pool = ConnectionPool.getInstance();
	}

	public static DBTools getInstance() {
		if (ins == null) {
			synchronized (DBTools.class) {
				if (ins == null) {
					ins = new DBTools();
				}
			}
		}
		return ins;
	}

	public Connection getConnection() {
		return pool.getConnection();
	}
}
