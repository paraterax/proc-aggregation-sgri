package com.paratera.sgri.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * 获取数据库连接池配置参数, 从dbtool.properties.
 */
public class DsConfig {
	/**
	 * 默认构造函数.
	 */
	private DsConfig() {
	}

	private static Configuration conf;

	static {
		try {
			conf = new PropertiesConfiguration("dbtool.properties");
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 数据源配置参数
	 */
	public static String url = conf.getString("ds.url");

	public static String validationQuery = conf.getString("ds.validationQuery");

	public static String username = conf.getString("ds.username");

	public static String password = conf.getString("ds.password");

	public static String initialSize = conf.getString("ds.initialSize");

	public static String maxActive = conf.getString("ds.maxActive");

	public static String minIdle = conf.getString("ds.minIdle");

	public static String maxWait = conf.getString("ds.maxWait");

	public static String testOnBorrow = conf.getString("ds.testOnBorrow");

	public static String testOnReturn = conf.getString("ds.testOnReturn");

	public static String testWhileIdle = conf.getString("ds.testWhileIdle");

	public static String timeBetweenEvictionRunsMillis = conf.getString("ds.timeBetweenEvictionRunsMillis");

	public static String minEvictableIdleTimeMillis = conf.getString("ds.minEvictableIdleTimeMillis");

	public static String removeAbandoned = conf.getString("ds.removeAbandoned");

	public static String removeAbandonedTimeout = conf.getString("ds.removeAbandonedTimeout");

	public static String logAbandoned = conf.getString("ds.logAbandoned");

	public static String filters = conf.getString("ds.filters");

	/**
	 * 根据key从dbtool.properties中获取配置项.
	 */
	public static Object getParam(String key) {
		return conf.getProperty(key);
	}

	/**
	 * 判断某个配置参数中是否有某个值
	 */
	public static boolean isValueExist(String key, String value) {
		boolean ret = false;
		List<Object> values = conf.getList(key);
		for (Object o : values) {
			if (ret) {
				break;
			}

			ret = value.equals(o.toString());
		}
		return ret;
	}

	public static Map<String, Object> getJDBCConfigMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("url", url);
		map.put("validationQuery", validationQuery);
		map.put("username", username);
		map.put("password", password);
		map.put("initialSize", initialSize);
		map.put("maxActive", maxActive);
		map.put("minIdle", minIdle);
		map.put("maxWait", maxWait);
		map.put("testOnBorrow", testOnBorrow);
		map.put("testOnReturn", testOnReturn);
		map.put("testWhileIdle", testWhileIdle);
		map.put("timeBetweenEvictionRunsMillis", timeBetweenEvictionRunsMillis);
		map.put("minEvictableIdleTimeMillis", minEvictableIdleTimeMillis);
		map.put("removeAbandoned", removeAbandoned);
		map.put("removeAbandonedTimeout", removeAbandonedTimeout);
		map.put("logAbandoned", logAbandoned);
		map.put("filters", filters);
		return map;
	}
}