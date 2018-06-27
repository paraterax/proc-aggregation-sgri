package com.paratera.sgri.config;

import java.util.TimeZone;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置参数常量类
 */
public final class ConfigParams {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigParams.class);

    /**
     * 默认构造函数.
     */
    private ConfigParams() {
    }

    private static Configuration conf;

    static {
        String fileName = "config.properties";
        try {
            conf = new PropertiesConfiguration(fileName);
        } catch (ConfigurationException e) {
            LOGGER.warn("can't load config file, {}", fileName);
            conf = new PropertiesConfiguration();
        }
    }

    public static final long ELASTICSEARCH_INDEX_OPEN_TIMEOUT =
            conf.getLong("elasticsearch.index.open.timeout", 5 * 60 * 1000);

    public static final long ELASTICSEARCH_INDEX_STATE_INTERVAL =
            conf.getLong("elasticsearch.index.state.interval", 5 * 1000);
    
    /**
    * 连接池最大连接数
    */
    public static final int MAX_TOTAL_CONNECTIONS = conf.getInt("http.conn-manager.max-total", 1024);

    /**
     * 从连接池中获取连接的超时时间
     */
    public static final int CONN_MANAGER_TIMEOUT = conf.getInt("http.conn-manager.timeout", 60000);

    /**
     * 连接服务器超时时间
     */
    public static final int CONNECT_TIMEOUT = conf.getInt("http.client-conn.timeout", 60000);

    /**
     * 读取数据超时时间
     */
    public static final int SO_TIMEOUT = conf.getInt("http.client-so.timeout", 6000000);

    /**
     * 指定时区
     */
    public static final String TIME_ZONE = conf.getString("time.zone", TimeZone.getDefault().getID());

    /**
     * ES服务器地址
     */
    public static final String ES_SERVER = conf.getString("es.server", "http://127.0.0.1:9200/");

    /**
     * 集群名称
     */
    public static final String[] CLUSTER_NAME = conf.getStringArray("cluster.name");
    
    /**
     * 新的ES索引名称
     */
    public static final String CLUSTER = conf.getString("cluster");

    /**
     * 开始时间
     */
    public static final String START = conf.getString("report.start");
    
    /**
     * 结束时间
     */
    public static final String END = conf.getString("report.end");
    
    /**
     * 每天定时执行
     */
    public static final int STARTTIME = conf.getInt("report.startTime");
    
    /**
     * 隔多久执行一次
     */
    public static final long SPACETIME =conf.getLong("report.spaceTime");
    
    /**
     * byDay输入参数为某天, byMonth输入参数为月
     */
    public static final String TIME_RANGE_BYDAY = "byDay";
    public static final String TIME_RANGE_BYMONTH = "byMonth";
    public static final String TIME_RANGE = conf.getString("time.range", TIME_RANGE_BYDAY);

    /**
     * 数据来源, 网络network, 本地文件local, 不处理数据none
     */
    public static final String DATA_SOURCE = conf.getString("data.source", "none");
    public static final String DATA_SOURCE_NETWORK = "network";
    public static final String DATA_SOURCE_LOCAL = "local";
    public static final String DATA_SOURCE_NONE = "none";

    /**
     * 数据存放目录
     */
    public static final String DATA_DIR = conf.getString("data.dir", "~/");

    /**
     * 是否将文件存到文件
     */
    public static final Boolean TOFILE_ON = conf.getBoolean("tofile.on", true);

    /**
     * 是否持久化到数据库
     */
    public static final Boolean PERSISTENCE_ON = conf.getBoolean("persistence.on", false);

    /**
     * 根据pid ppid更新project_name, job_name, job_number
     */
    public static final Boolean UPDATE_PROJECT_ON = conf.getBoolean("update.project.on", false);

    /**
    * 是否统计机时
    */
    public static final Boolean PROC_AGGS_ON = conf.getBoolean("proc.aggs.on", false);
}
