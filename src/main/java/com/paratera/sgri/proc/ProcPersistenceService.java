package com.paratera.sgri.proc;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.paratera.sgri.config.ConfigParams;
import com.paratera.sgri.db.DBTools;
import com.paratera.sgri.util.FileUtils;
import com.paratera.sgri.util.SqlSpecialChar;

/**
 * 进程持久化服务类
 */
public class ProcPersistenceService {

    /**
     * 日志实例.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcPersistenceService.class);

    private static final String SEP = ",";

    /**
     * 将进程数据保存到数据库中 １. 创建表 2. 刪除老数据 3. 插入数据
     */
    public void saveProcData(String clusterName, String day, JSONObject procAggsDataMap) throws SQLException {
        createTable(clusterName, day);
        deleteOldData(clusterName, day);
        insertData(clusterName, day, procAggsDataMap);
    }

    private void createTable(String clusterName, String day) {
        String tableName = clusterName + "_proctime_" + day.substring(0, 6);
        LOGGER.info("cluster[{}], day {}, create table, table[{}].", clusterName, day, tableName);
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE " + tableName + " (\n");
        sb.append("auto_id bigint(20) NOT NULL AUTO_INCREMENT,\n");
        sb.append("uuid char(32) NOT NULL,\n");
        sb.append("hostname varchar(255) DEFAULT NULL,\n");
        sb.append("comm varchar(255) DEFAULT NULL,\n");
        sb.append("cmdline text,\n");
        sb.append("cpu_time double(120,4) DEFAULT NULL,\n");
        sb.append("p_time double(120,4) DEFAULT NULL,\n");
        sb.append("ppid bigint(20) DEFAULT NULL,\n");
        sb.append("pid bigint(20) DEFAULT NULL,\n");
        sb.append("pcpu double(120,4) DEFAULT NULL,\n");
        sb.append("pmem double(120,4) DEFAULT NULL,\n");
        sb.append("run_time double(120,3) DEFAULT NULL,\n");
        sb.append("str_start_time varchar(32) DEFAULT NULL,\n");
        sb.append("start_time_unix bigint(20) DEFAULT NULL,\n");
        sb.append("first_time bigint(20) DEFAULT NULL,\n");
        sb.append("last_time bigint(20) DEFAULT NULL,\n");
        sb.append("doc_count bigint(20) DEFAULT NULL,\n");
        sb.append("user_name varchar(255) DEFAULT NULL,\n");
        sb.append("project_name varchar(255) DEFAULT NULL,\n");
        sb.append("job_name varchar(255) DEFAULT NULL,\n");
        sb.append("job_number bigint(20) DEFAULT NULL,\n");
        sb.append("time_range varchar(16) DEFAULT NULL,\n");
        sb.append("PRIMARY KEY (auto_id)\n");
        sb.append(");\n");

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DBTools.getInstance().getConnection();
            stmt = conn.createStatement();
            stmt.execute(sb.toString());
            conn.close();
        } catch (SQLException e) {
            if (e.toString().contains("already exists")) {
            } else {
                LOGGER.error("cluster[{}], day {}, table[{}], create table failed.", clusterName, day, tableName);
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void deleteOldData(String clusterName, String day) {
        String tableName = clusterName + "_proctime_" + day.substring(0, 6);
        LOGGER.info("cluster[{}], day {}, delete old data table[{}].", clusterName, day, tableName);
        String sql = "delete from " + tableName + " where time_range = '" + day + "'";
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DBTools.getInstance().getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
            conn.close();
        } catch (SQLException e) {
            LOGGER.error("cluster[{}], day {}, delete old data failed, table[{}].", clusterName, day, tableName);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void insertData(String clusterName, String day, JSONObject procAggsDataMap) throws SQLException {
        String tableName = clusterName + "_proctime_" + day.substring(0, 6);
        String[] fields = new String[]{"uuid", "hostname", "comm", "cmdline", "cpu_time", "p_time", "ppid", "pid",
                "pcpu", "pmem", "run_time", "str_start_time", "start_time_unix", "first_time", "last_time", "doc_count",
                "user_name", "project_name", "job_name", "job_number", "time_range"};
        String dml = "insert into " + tableName + "(" + StringUtils.join(fields, ",") + ") values";
        Connection connection = DBTools.getInstance().getConnection();
        long start = System.currentTimeMillis();
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        StringBuilder sqlValues = new StringBuilder();
        int counter = 0;
        Set<String> keys = procAggsDataMap.keySet();
        for (String key : keys) {
            JSONObject o = procAggsDataMap.getJSONObject(key);
            sqlValues.append("(");
            sqlValues.append("'").append(o.getString("id")).append("',"); // uuid
            String hostName = o.getString("hostname");
            if (hostName == null) {
                sqlValues.append("null,");
            } else {
                sqlValues.append("'").append(SqlSpecialChar.replace(hostName, "write")).append("',");
            }
            String comm = o.getString("comm");
            if (comm == null) {
                sqlValues.append("null,");
            } else {
                sqlValues.append("'").append(SqlSpecialChar.replace(comm, "write")).append("',");
            }
            String cmdline = o.getString("cmdline");
            if (cmdline == null) {
                sqlValues.append("null,");
            } else {
                sqlValues.append("'").append(SqlSpecialChar.replace(cmdline, "write")).append("',");
            }
            sqlValues.append(o.getDouble("cpu_time")).append(",");
            sqlValues.append(o.getDouble("p_time")).append(",");
            sqlValues.append(o.getLong("ppid")).append(",");
            sqlValues.append(o.getLong("pid")).append(",");
            sqlValues.append(o.getDouble("pcpu")).append(",");
            sqlValues.append(o.getDouble("pmem")).append(",");
            sqlValues.append(o.getDouble("run_time")).append(",");
            String str_start_time = o.getString("str_start_time");
            if (str_start_time == null) {
                sqlValues.append("null,");
            } else {
                sqlValues.append("'").append(SqlSpecialChar.replace(str_start_time, "write")).append("',");
            }
            sqlValues.append(o.getLong("start_time_unix")).append(",");
            sqlValues.append(o.getLong("first_time")).append(",");
            sqlValues.append(o.getLong("last_time")).append(",");
            sqlValues.append(o.getLong("doc_count")).append(",");
            String user_name = o.getString("user_name");
            if (user_name == null) {
                sqlValues.append("null,");
            } else {
                sqlValues.append("'").append(SqlSpecialChar.replace(user_name, "write")).append("',");
            }
            String project_name = o.getString("project_name");
            if (project_name == null) {
                sqlValues.append("null,");
            } else {
                sqlValues.append("'").append(SqlSpecialChar.replace(project_name, "write")).append("',");
            }
            String job_name = o.getString("job_name");
            if (job_name == null) {
                sqlValues.append("null,");
            } else {
                sqlValues.append("'").append(SqlSpecialChar.replace(job_name, "write")).append("',");
            }
            sqlValues.append(o.getLong("job_number")).append(",");
            sqlValues.append(day);
            sqlValues.append("),");
            counter++;
            // 一千条一次插入吧
            if (counter >= 1000) {
                sqlValues.setLength(sqlValues.length() - 1);
                String sql = dml + sqlValues.toString();
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.executeUpdate(sql);
                connection.commit();
                counter = 0;
                sqlValues.setLength(0);
            }
        }

        if (counter > 0) {
            sqlValues.setLength(sqlValues.length() - 1);
            String sql = dml + sqlValues.toString();
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.executeUpdate(sql);
        }

        connection.setAutoCommit(autoCommit);
        connection.close();
        long cost = System.currentTimeMillis() - start;
        LOGGER.info("cluster[{}], day {}, table {}, data to mysql, data size {}, cost {} ms, {} s.", clusterName, day,
                tableName, procAggsDataMap.size(), cost, cost / 1000);
    }

    /**
     * 获取机时统计-按月
     */
    public void getCpuTimeAggs(String[] clusters, String time) {
        String month = time.matches("201\\d{3,3}") ? time : time.substring(0, 6);
        // 緩存存在的表
        Set<String> tableSet = new HashSet<String>();
        String tpl =
                "select fieldName, round(sum(cpu_time)/3600, 2) cpu_time, round(sum(p_time)/3600, 2) p_time from tableName";
        // 排除非ommain的进程, 以job_number为空表示非ommain的进程
        // tpl += " where job_number > 0";
        // tpl += " WHERE time_range BETWEEN '20170320' AND '20170324'";
        tpl += " group by fieldName order by cpu_time desc";
        String[] groupBy = new String[]{"project_name", "user_name"};
        for (String clusterName : clusters) {
            String tableName = clusterName + "_proctime_" + month;
            for (String gb : groupBy) {
                String sql = tpl.replace("fieldName", gb).replace("tableName", tableName);
                LOGGER.info("cluster[{}], month {}, proc time aggs, sql: {}.", clusterName, month, sql);
                try {
                    String data = getCpuCoreTimeBySql(sql);
                    tableSet.add(tableName);
                    String fileName = ConfigParams.DATA_DIR + File.separator + "cpu_core_time_" + month + File.separator
                            + clusterName + "_" + gb.replaceAll("_name", "") + "_" + month + ".csv";
                    FileUtils.writeFile(fileName, data);
                } catch (Exception e) {
                    LOGGER.error(e.toString());
                }
            }
        }
        getTotalCpuCoreTime(groupBy, tableSet, month);
    }

    private void getTotalCpuCoreTime(String[] fields, Set<String> tableSet, String month) {
        LOGGER.info("获取所有集群的统计结果");
        for (String field : fields) {
            StringBuilder sb = new StringBuilder();
            for (String table : tableSet) {
                StringBuilder sql = new StringBuilder();
                sql.append("select " + field
                        + ", round(sum(cpu_time)/3600, 2) cpu_time, round(sum(p_time)/3600, 2) p_time from ");
                sql.append(table);
                // 排除非ommain的进程, 以job_number为空表示非ommain的进程
                // sql.append(" where job_number > 0");
                // sql.append(" WHERE time_range BETWEEN '20170320' AND '20170324'");
                sql.append(" group by " + field);
                sb.append(sql).append("\nunion all\n");
            }
            sb.setLength(sb.length() - "\nunion all\n".length());

            String q = "select " + field + ", sum(cpu_time) cpu_time, sum(p_time) p_time from (\n" + sb.toString()
                    + "\n) t group by " + field + " order by cpu_time desc";
            try {
                LOGGER.info("get total cpu core time, sql {}", q);
                String data = getCpuCoreTimeBySql(q);
                String fileName = ConfigParams.DATA_DIR + File.separator + "cpu_core_time_" + month + File.separator
                        + "all_" + field.replaceAll("_name", "") + "_" + month + ".csv";
                FileUtils.writeFile(fileName, data);
            } catch (Exception e) {
                LOGGER.error(e.toString());
            }
        }
    }

    private String getCpuCoreTimeBySql(String sql) throws SQLException {
        Connection connection = DBTools.getInstance().getConnection();
        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        StringBuilder data = new StringBuilder();
        int cnt = stmt.getMetaData().getColumnCount();
        for (int i = 0; i < cnt; i++) {
            data.append(stmt.getMetaData().getColumnLabel(i + 1)).append(SEP);
        }
        data.setLength(data.length() - 1);
        data.append('\n');

        while (rs.next()) {
            data.append(rs.getObject(1)).append(SEP).append(rs.getObject(2)).append(SEP).append(rs.getObject(3) + "\n");
        }
        rs.close();
        connection.close();
        return data.toString();
    }
}
