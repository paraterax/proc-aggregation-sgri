package com.paratera.sgri.proc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.paratera.sgri.util.NumberUtils;

/**
 * 进程数据解析服务类
 */
public class ProcParserService {
    /**
     * 日志实例.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcParserService.class);

    /**
     * 项目名称匹配
     */
    private static final Pattern PROJECT_PATTERN = Pattern.compile("_ommain.*-project\\s+(\\w+)\\s+([0-9]+$)");

    /**
     * 作业名称匹配
     */
    private static final Pattern JOB_PATTERN = Pattern.compile(
            "^oci_command_server_container -mrID.*-record /scr01/om2scr/(\\w+)_([0-9]+)_computenode_[0-9]+/.*");

    /**
     * 集群名
     */
    private String clusterName;

    /**
     * 日期
     */
    private String day;

    public ProcParserService(String clusterName, String day) {
        this.clusterName = clusterName;
        this.day = day;
    }

    /**
     * 解析项目字段, regex参见PROJECT_PATTERN _ommain omega_runjava.so -XX:+UseCompressedOops -Xmx320M -Domega.launcher.log=proc -Domega.launcher.port=2134
     * -Domega.jni.nci=0 com.westerngeco.omega.launcher.RMIIIOPServiceContainer com.westerngeco.omega.flow.driver.RemoteFlowDriverService 860753830
     * &session=Session_xuying_1482467161&installation=2014.1extp&baseline=2014.1ext&site=Local&host=sghplg01.sgri.sinopec.com&ORBInitialHost=sgibwg2014&ORBInitialPort=2003&display=%3A40&memory=-1
     * -project chuli_2016TH67Q 36006
     * 
     * @see ProcParserService#PROJECT_PATTERN
     */
    private String[] parseProjectField(String cmdline) {
        String[] projectInfo = null;
        if (cmdline != null) {
            Matcher matcher = PROJECT_PATTERN.matcher(cmdline);
            if (matcher.find()) {
                projectInfo = new String[2]; // projectName jobNumber
                projectInfo[0] = matcher.group(1);
                projectInfo[1] = matcher.group(2);
            }
        }
        return projectInfo;
    }

    /**
     * 解析作业名字段, regex参见JOB_PATTERN oci_command_server_container -mrID 192.168.132.95#192.168.128.95,58479,2,25,551 -record
     * /scr01/om2scr/OCI_t106A_psdm_Vupd05it7JBedit101H03H04_a_VoL_off3420_36006_computenode_1/oci_sfm_migr_kirch_collect_distributedReductionServer19.rec
     * oci_sfm_migr_kirch_collect_distributedReductionServer
     * 
     * @see ProcParserService#JOB_PATTERN
     */
    private String[] parseJobField(String cmdline) {
        String[] jobInfo = null;
        if (cmdline != null) {
            Matcher matcher = JOB_PATTERN.matcher(cmdline);
            if (matcher.find()) {
                jobInfo = new String[2]; // jobName jobNumber;
                jobInfo[0] = matcher.group(1);
                jobInfo[1] = matcher.group(2);
            }
        }
        return jobInfo;
    }

    /**
     * 解析cpu占用时间 0:10.36 -> 10.36 s
     */
    private double getCpuTime(String strTime) {
        double cpuTime = 0.0D;
        if (strTime == null || strTime.length() < 1 || strTime.indexOf(":") < 0) {
            LOGGER.warn("str_time字段不正确, {}", strTime);
            return 0.0D;
        }
        String[] times = strTime.split(":");
        if (times.length != 2) {
            LOGGER.warn("str_time字段不正确, {}", strTime);
            return 0.0D;
        } else {
            cpuTime = Double.valueOf(times[0]) * 60 + Double.valueOf(times[1]);
        }
        return NumberUtils.toFixed(2, cpuTime);
    }

    /**
     * 解析数据的时候, 一定要保证数据类型正确
     */
    public JSONObject parseProcAggsData(String json, long startTimeOfToday) {
        long t1 = System.currentTimeMillis();
        // 临时存放数据 job_number和project_name, job_number和job_name 的对应关系
        Map<Long, Set<String>> jobNumberProjectNameRelation = new HashMap<Long, Set<String>>();
        Map<Long, Set<String>> jobNumberNameRelation = new HashMap<Long, Set<String>>();
        JSONObject aggsDataMap = new JSONObject();
        long start = System.currentTimeMillis();
        JSONObject data = JSON.parseObject(json);
        long cost = System.currentTimeMillis() - start + 1;
        LOGGER.info("cluster[{}], day {}, JSON.parseObject, size {} MB, cost {} ms, {} s.", clusterName, day,
                NumberUtils.toFixed(4, json.getBytes().length / 1024.0 / 1024.0), cost, cost / 1000);

        json = null;
        System.gc();
        String[] user_name = {"sshd ", "apache", "avahi", "gdm", "haldeamon", "jssmgr", "lava", "mysql", "nobody",
                "nscd", "nslcd", "ntp", "oracle", "qpidd", "root", "rpc", "rpcuser", "stap-server", "tcpdump",
                "telegraf", "xfs"};
        List<String> userList = Arrays.asList(user_name);
        if (data.get("error") == null) {
            JSONArray hostDataList = data.getJSONObject("aggregations").getJSONObject("aggs").getJSONArray("buckets");
            int hostSize = hostDataList.size();
            for (int i = 0; i < hostSize; i++) {
                JSONObject hostData = hostDataList.getJSONObject(i);
                String hostName = hostData.getString("key");
                JSONArray pidDataList = hostData.getJSONObject("aggs").getJSONArray("buckets");
                int pidSize = pidDataList.size();
                for (int j = 0; j < pidSize; j++) {
                    JSONObject pidData = pidDataList.getJSONObject(j);
                    long pid = pidData.getLongValue("key");
                    JSONArray startTimeDataList = pidData.getJSONObject("aggs").getJSONArray("buckets");
                    int startTimeDataSize = startTimeDataList.size();
                    for (int k = 0; k < startTimeDataSize; k++) {
                        JSONObject startTimeData = startTimeDataList.getJSONObject(k);
                        long startTime = startTimeData.getLongValue("key");
                        JSONObject topHits = startTimeData.getJSONObject("top_hits_asc").getJSONObject("hits")
                                .getJSONArray("hits").getJSONObject(0).getJSONObject("_source");
                        long docCount = startTimeData.getLongValue("doc_count");
                        String user = (String) topHits.get("user_name");
                        if (userList.contains(user)) {
                            continue;
                        } else {
                            Double avgPcpu =
                                    NumberUtils.toFixed(4, startTimeData.getJSONObject("avg_pcpu").getDouble("value"));
                            Double avgPmem =
                                    NumberUtils.toFixed(4, startTimeData.getJSONObject("avg_pmem").getDouble("value"));
                            JSONObject topHitsDesc = startTimeData.getJSONObject("top_hits_desc").getJSONObject("hits")
                                    .getJSONArray("hits").getJSONObject(0).getJSONObject("_source");
                            JSONObject aggsData = new JSONObject();
                            aggsData.put("doc_count", docCount);
                            aggsData.put("hostname", hostName);
                            aggsData.put("pid", pid);
                            aggsData.put("start_time_unix", startTime);
                            // unique identifier
                            String uuid = DigestUtils.md5Hex(hostName + "_" + pid + "_" + startTime);
                            aggsData.put("id", uuid);
                            aggsData.put("pcpu", avgPcpu);
                            aggsData.put("pmem", avgPmem);
                            long firstTime = topHits.getLongValue("timestamp");
                            aggsData.put("first_time", firstTime);
                            long lastTime = topHitsDesc.getLongValue("timestamp");
                            aggsData.put("last_time", lastTime);
                            aggsData.put("ppid", topHits.get("ppid"));
                            aggsData.put("comm", topHits.get("comm"));
                            String cmdline = topHits.getString("cmdline");
                            aggsData.put("cmdline", cmdline);
                            aggsData.put("user_name", topHits.get("user_name"));

                            String[] projectInfo = parseProjectField(cmdline);
                            String[] jobInfo = parseJobField(cmdline);
                            if (projectInfo != null) {
                                String projectName = projectInfo[0];
                                Long jobNumber = Long.valueOf(projectInfo[1]);
                                aggsData.put("project_name", projectName);
                                aggsData.put("job_number", jobNumber);
                                Set<String> projectNames = jobNumberProjectNameRelation.get(jobNumber);
                                if (projectNames == null) {
                                    projectNames = new HashSet<String>();
                                    jobNumberProjectNameRelation.put(jobNumber, projectNames);
                                }
                                projectNames.add(projectName);
                            }
                            if (jobInfo != null) {
                                String jobName = jobInfo[0];
                                Long jobNumer = Long.valueOf(jobInfo[1]);
                                aggsData.put("job_name", jobName);
                                aggsData.put("job_number", jobNumer);
                                Set<String> jobNames = jobNumberNameRelation.get(jobNumer);
                                if (jobNames == null) {
                                    jobNames = new HashSet<String>();
                                    jobNumberNameRelation.put(jobNumer, jobNames);
                                }
                                jobNames.add(jobName);
                            }
                            aggsData.put("str_start_time", topHits.get("str_start_time"));

                            // 修正cpu_time
                            aggsData.put("cpu_time", getCpuTime(topHitsDesc.getString("str_time"))
                                    - getCpuTime(topHits.getString("str_time")));

                            // 运行时间 换算成（秒）
                            // 这里es中的数据不正确, 数据的采集时间比程序的开始运行时间都要早
                            double runTime = NumberUtils.toFixed(2,
                                    ( lastTime - ( startTime < startTimeOfToday ? startTimeOfToday : startTime ) + 1 )
                                            / 1000.0);
                            runTime = runTime > 0D ? runTime : 0D;
                            aggsData.put("run_time", runTime);
                            // 计算 核时 = cpu平均利用率 * 程序运行时间
                            double pTime = NumberUtils.toFixed(2, runTime * avgPcpu);
                            aggsData.put("p_time", pTime);
                            aggsDataMap.put(uuid, aggsData);
                        }
                    }
                }
            }
        } else if (data.get("error") != null) {
            LOGGER.error(data.toJSONString());
        }
        data = null;
        System.gc();

        // 解析完所有的project_name, job_name, job_number后, 将每条数据填充这几个字段
        Set<String> keys = aggsDataMap.keySet();
        for (String key : keys) {
            JSONObject o = aggsDataMap.getJSONObject(key);
            String projectName = o.getString("project_name");
            Long jobNubmer = o.getLong("job_number");
            String jobName = o.getString("job_name");
            if (projectName != null && jobNubmer != null) { // 更新作业名称
                Set<String> jobNames = jobNumberNameRelation.get(jobNubmer);
                if (jobNames != null && jobNames.size() > 0) {
                    o.put("job_name", jobNames.iterator().next());
                }
            }
            if (jobName != null && jobNubmer != null) { // 更新项目名称
                Set<String> projectNames = jobNumberProjectNameRelation.get(jobNubmer);
                if (projectNames != null && projectNames.size() > 0) {
                    o.put("project_name", projectNames.iterator().next());
                }
            }
        }
        long t2 = System.currentTimeMillis() - t1 + 1;
        LOGGER.info("cluster[{}], day {}, data process, cost {}ms, {}s.", clusterName, day, t2, t2 / 1000);
        return aggsDataMap;
    }
}
