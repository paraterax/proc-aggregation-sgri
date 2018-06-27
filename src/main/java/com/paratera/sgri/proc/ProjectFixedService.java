package com.paratera.sgri.proc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paratera.sgri.db.DBTools;
import com.paratera.sgri.pojo.ProcPOJO;

/**
 * 项目名称　作业名称修复
 */
public class ProjectFixedService {

    /**
     * 日志实例.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectFixedService.class);

    /**
     * 读取含有项目或者作业信息的顶级进程
     * 第一步 读取job_number > 0 的进程信息
     * 输入: 集群　天
     * 输入：集群　天　主机(读取太频繁, 暂不划分的这么细)
     * @return 
     * @throws SQLException 
     */
    private List<ProcPOJO> getProcWithProcject(String cluster, String day) throws SQLException {
        List<ProcPOJO> procList = new ArrayList<ProcPOJO>();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT auto_id, hostname, comm, cmdline, ppid, pid, project_name, job_name, job_number, start_time_unix, first_time, last_time");
        sb.append(" FROM ").append(cluster).append("_proctime_").append(day.substring(0, 6));
        sb.append(" WHERE job_number > 0 AND ppid = 1 AND time_range = '" + day + "'");
        sb.append(" AND comm ='_ommain'");
        sb.append(" ORDER BY comm, hostname, ppid, pid");
        LOGGER.info("cluster[{}], day {}, get proc project job info, sql: {}.", cluster, day, sb.toString());
        Connection conn = DBTools.getInstance().getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sb.toString());
        while (rs.next()) {
            ProcPOJO p = new ProcPOJO();
            p.setAutoId(rs.getLong(1));
            p.setHostName(rs.getString(2));
            p.setComm(rs.getString(3));
            p.setCmdline(rs.getString(4));
            p.setPpid(rs.getLong(5));
            p.setPid(rs.getLong(6));
            p.setProjectName(rs.getString(7));
            p.setJobName(rs.getString(8));
            p.setJobNumber(rs.getLong(9));
            p.setStartTimeUnix(rs.getLong(10));
            p.setFirstTime(rs.getLong(11));
            p.setLastTime(rs.getLong(12));
            p.setTimeRange(day);
            p.setCluster(cluster);
            procList.add(p);
        }
        rs.close();
        stmt.close();
        conn.close();
        return procList;
    }

    /**
     * 根据父进程查询子进程
     * @throws SQLException 
     */
    private List<Long> getChildProcIdsByParentProc(ProcPOJO parent) throws SQLException {
        List<Long> idList = new ArrayList<Long>();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT auto_id FROM ");
        sb.append(parent.getCluster() + "_proctime_" + parent.getTimeRange().substring(0, 6));
        sb.append(" WHERE hostname = '" + parent.getHostName() + "' AND time_range = '" + parent.getTimeRange() + "'");
        sb.append(" AND ppid = " + parent.getPid());
        LOGGER.info("cluster[{}], day {}, get child proc, sql: {}.", parent.getCluster(), parent.getTimeRange(), sb.toString());
        // sb.append(" AND comm = 'ommain'");
        // sb.append(" AND (first_time BETWEEN " + (parent.getFirstTime() - (30 * 1000)) + " AND " + (parent.getLastTime() + (30 * 1000)) + ")");
        // sb.append(" AND (last_time BETWEEN " + (parent.getFirstTime() - (30 * 1000)) + " AND " + (parent.getLastTime() + (30 * 1000)) + ")");
        // sb.append(" AND start_time_unix >= " + (parent.getStartTimeUnix() - 30 * 1000));
        Connection conn = DBTools.getInstance().getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sb.toString());
        while (rs.next()) {
            idList.add(rs.getLong(1));
        }
        rs.close();
        stmt.close();
        conn.close();
        return idList;
    }

    private void batchUpdate(String cluster, String month, List<ProcPOJO> procList) throws SQLException {
        Connection conn = DBTools.getInstance().getConnection();
        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE " + cluster + "_proctime_" + month + " SET project_name = ?, job_name = ?, job_number = ?");
        sb.append(" WHERE auto_id = ?");
        PreparedStatement ps = conn.prepareStatement(sb.toString());
        for (ProcPOJO p : procList) {
            ps.setString(1, p.getProjectName());
            ps.setString(2, p.getJobName());
            ps.setLong(3, p.getJobNumber());
            ps.setLong(4, p.getAutoId());
            ps.addBatch();
        }
        ps.executeBatch();
        conn.commit();
        conn.setAutoCommit(autoCommit);
        ps.close();
        conn.close();
    }

    public void fixProcInfo(String cluster, String day) throws SQLException, ParseException {
        String month = day.substring(0, 6);
        List<ProcPOJO> updateProcList = new ArrayList<ProcPOJO>();
        List<ProcPOJO> procList = getProcWithProcject(cluster, day);
        for (ProcPOJO parent : procList) {
            List<Long> ids = getChildProcIdsByParentProc(parent);
            for (Long id : ids) {
                ProcPOJO p = new ProcPOJO();
                p.setAutoId(id);
                p.setProjectName(parent.getProjectName());
                p.setJobName(parent.getJobName());
                p.setJobNumber(parent.getJobNumber());
                updateProcList.add(p);
            }
        }
        int size = updateProcList.size();
        if (size > 0) {
            LOGGER.info("cluster[{}], day {}, update proc project job info, size {}.", cluster, day, size);
            batchUpdate(cluster, month, updateProcList);
        }
    }
}
