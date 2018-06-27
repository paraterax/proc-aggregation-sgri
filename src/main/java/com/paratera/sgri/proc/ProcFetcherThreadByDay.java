package com.paratera.sgri.proc;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.paratera.sgri.config.ConfigParams;
import com.paratera.sgri.http.HttpClient;
import com.paratera.sgri.http.HttpResponse;
import com.paratera.sgri.util.DayUtils;
import com.paratera.sgri.util.NumberUtils;

/**
 * 按天聚合进程数据
 */
public class ProcFetcherThreadByDay implements Callable<Long> {

    /**
     * 日志实例.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcFetcherThreadByDay.class);

    /**
     * 集群名
     */
    private String clusterName;

    /**
     * 日期
     */
    private String day;

    private HttpClient client;

    private String dsl;

    private ProcParserService service;

    private ProcPersistenceService pService;

    public ProcFetcherThreadByDay(String clusterName, String day, String dsl) {
        this.clusterName = clusterName;
        this.day = day;
        this.dsl = dsl;
        client = HttpClient.getInstance();
        service = new ProcParserService(clusterName, day);
        pService = new ProcPersistenceService();
    }

    /**
     * 读取本地文件或者网络下载文件.
     */
    private String getProcAggsData() {
        String file = ConfigParams.DATA_DIR + File.separator + ConfigParams.CLUSTER + "-" + day + "-proc.json";
        String procJsonData = "";
        if (ConfigParams.DATA_SOURCE.equals(ConfigParams.DATA_SOURCE_LOCAL)) { // 使用本地数据文件
            LOGGER.info("cluster[{}], day {}, reading local data file, {}.", ConfigParams.CLUSTER, day, file);
            try {
                procJsonData = FileUtils.readFileToString(new File(file));
            } catch (IOException e) {
                LOGGER.warn("cluster[{}], day {}, reading local data file failed, {}", ConfigParams.CLUSTER, day, e);
            }
        } else if (ConfigParams.DATA_SOURCE.equals(ConfigParams.DATA_SOURCE_NETWORK)) { // 从网络上下载数据
            LOGGER.debug("cluster[{}], day {}, HttpClient.get starting download.", ConfigParams.CLUSTER, day);
            String searchIndex = ConfigParams.CLUSTER + "-proc-" + day;
            String searchUrl = ConfigParams.ES_SERVER + searchIndex + "/proclist/_search";
            long start = System.currentTimeMillis();
            HttpResponse rsp = client.post(searchUrl, dsl);
            procJsonData = rsp.getData();
            long cost = System.currentTimeMillis() - start + 1;
            if ("".equals(procJsonData) || null == procJsonData || procJsonData.length() < 0) {
                LOGGER.info("集群{}在{}的数据为空。", clusterName, day);
            } else {
                LOGGER.info("cluster[{}], day {}, HttpClient.get data download, {}s, data size {}MB.", clusterName, day,
                        cost / 1000, NumberUtils.toFixed(4, procJsonData.getBytes().length / 1024.0 / 1024));
            }
            if (rsp.isSuccess()) {
                if (ConfigParams.TOFILE_ON) {
                    try {
                        FileUtils.writeStringToFile(new File(file), procJsonData);
                    } catch (IOException e) {
                        LOGGER.warn("cluster[{}], day {}, write data to local file failed, {}", clusterName, day, e);
                    }
                }
            } else {
                LOGGER.error("cluster[{}], day {}, errMsg: {}, errData: {}", clusterName, day, rsp.getErrorMsg(),
                        rsp.getData());
            }
        }
        return procJsonData;
    }


    @Override
    public Long call() throws Exception {
        long startTime = System.currentTimeMillis();
        long[] timesRange = DayUtils.getDayRange(day, "yyyyMMdd");
        // 获取进程数据
        String procData = getProcAggsData();
        // 解析进程数据
        if (procData != null && procData.length() > 0) {
            JSONObject procDataMap = service.parseProcAggsData(procData, timesRange[0]);
            try {
                if (ConfigParams.PERSISTENCE_ON) {
                    pService.saveProcData(clusterName, day, procDataMap);
                }
            } catch (Exception e) {
                LOGGER.error("save data to mysql failed. please check data, if there is any special character, ", e);
            }
        }
        long totalCost = System.currentTimeMillis() - startTime;
        LOGGER.info("cluster[{}], day {}, 数据处理耗时 {}s, {}min.", clusterName, day, totalCost / 1000.0,
                NumberUtils.toFixed(2, totalCost / 1000.0 / 60.0));
        return totalCost;
    }

    // 等待索引的状态变为green
    public static void waitingForGreenOrYellowState(String indexName, long indexOpenTime) {
        int times = (int) ( ConfigParams.ELASTICSEARCH_INDEX_OPEN_TIMEOUT
                / ConfigParams.ELASTICSEARCH_INDEX_STATE_INTERVAL );
        LOGGER.info("waiting for green/yellow state for index: {}.", indexName);
        String health = "red";
        for (int i = 0; i < times; i++) {
            try {
                String getUrl = ConfigParams.ES_SERVER + "/_cat/indices/" + indexName;
                HttpResponse rd = HttpClient.getInstance().get(getUrl);
                if (rd.isSuccess()) {
                    String data = rd.getData();
                    if (data.indexOf("index_not_found_exception") > 0) {
                        break;
                    }
                    String[] fields = data.split("\\s+");
                    health = fields[0].trim();
                    if ("green".equals(health) || "yellow".equals(health)) {
                        LOGGER.debug("index is ready, index: {}, state: {}", indexName, health);
                        break;
                    } else {
                        LOGGER.debug("try {} times, index not ready, index: {}, state: {}", i + 1, indexName, health);
                    }
                }
                Thread.sleep(ConfigParams.ELASTICSEARCH_INDEX_STATE_INTERVAL);
            } catch (Exception e) {
                LOGGER.error("Exception", e);
            }
        }
        double cost = ( System.currentTimeMillis() - indexOpenTime ) / 1000.0;
        if ("green".equals(health) || "yellow".equals(health)) {
            LOGGER.debug("successfully: waiting for green/yellow state for index: {}, state: {}, cost: {} s.",
                    indexName, health, cost);
        } else {
            LOGGER.warn("failed: waiting for green/yellow state for index: {}, state: {}, cost: {} s.", indexName,
                    health, cost);
        }
    }
}
