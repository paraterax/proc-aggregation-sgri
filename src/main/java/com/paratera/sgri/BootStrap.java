
package com.paratera.sgri;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paratera.sgri.config.ConfigParams;
import com.paratera.sgri.proc.ProcFetcherThreadByDay;
import com.paratera.sgri.proc.ProcPersistenceService;
import com.paratera.sgri.proc.ProjectFixedService;
import com.paratera.sgri.util.DayUtils;
import com.paratera.sgri.util.ESUtils;
import com.paratera.sgri.util.FileUtils;
import com.paratera.sgri.util.NumberUtils;

/**
 * 程序入口
 */
public class BootStrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootStrap.class);
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");

    public static void main(String[] args)
            throws InterruptedException, ExecutionException, IOException, ParseException, SQLException {
        if (StringUtils.isNotBlank(ConfigParams.START) && StringUtils.isNotEmpty(ConfigParams.START)
                && StringUtils.isNotBlank(ConfigParams.END) && StringUtils.isNotEmpty(ConfigParams.END)) {
            if (!ConfigParams.END.matches("201\\d{5,5}") || !ConfigParams.START.matches("201\\d{5,5}")) {
                LOGGER.error("开始时间或者结束时间不正确。[{},{}].", ConfigParams.START, ConfigParams.END);
                System.exit(0);
            }
            Date start = SDF.parse(ConfigParams.START);
            Date end = SDF.parse(ConfigParams.END);
            if (end.before(start)) {
                LOGGER.error("开始时间应小于结束时间。[{},{}].", ConfigParams.START, ConfigParams.END);
                System.exit(0);
            }
            List<String> dayList = DayUtils.getDayList(ConfigParams.START, ConfigParams.END);
            parseData(dayList);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, ConfigParams.STARTTIME);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date time = calendar.getTime();
            Timer timer = new Timer();
            if (time.before(new Date())) {
                time = DayUtils.addDay(time, 1);
            }
            timer.schedule(new TimerTask() {
                public void run() {
                    try {
                        Date start = DayUtils.addDay(new Date(), -1);
                        String day = new SimpleDateFormat("yyyyMMdd").format(start);
                        LOGGER.info("查询日期为:{}.", day);
                        List<String> list = new ArrayList<String>();
                        list.add(day);
                        parseData(list);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, time, ConfigParams.SPACETIME);
        }
    }

    public static void parseData(List<String> dayList) {
        long totalCost = 0;
        for (String day : dayList) {
            String searchIndex = ConfigParams.CLUSTER + "-proc-" + day;
            boolean exists = ESUtils.isIndexExists(searchIndex);
            if (exists) {
                // 判断索引是否关闭
                boolean closed = ESUtils.isIndexClosed(searchIndex);
                // 如果关闭执行打开操作
                if (closed) {
                    LOGGER.info("open index, index: {}.", searchIndex);
                    ESUtils.openIndex(searchIndex);
                }
                long indexOpenTime = System.currentTimeMillis();
                // 等待索引的状态变为green或者黄色
                ProcFetcherThreadByDay.waitingForGreenOrYellowState(searchIndex, indexOpenTime);
                ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
                try {
                    int clusterSize = ConfigParams.CLUSTER_NAME.length;
                    List<Future<Long>> resultList = new ArrayList<Future<Long>>();
                    String classpath = "/dsl/unique_proc.dsl";
                    for (int i = 0; i < clusterSize; i++) {
                        String query1 =
                                "{\"size\": 0,\"query\": {\"filtered\": {\"query\": {\"query_string\": {\"query\": \"subcid:"
                                        + ConfigParams.CLUSTER_NAME[i] + "\",\"analyze_wildcard\": true}}}},";
                        String query2 = FileUtils.getContent(classpath);
                        String query = query1 + query2;
                        Future<Long> future =
                                pool.submit(new ProcFetcherThreadByDay(ConfigParams.CLUSTER_NAME[i], day, query));
                        resultList.add(future);
                    }
                    for (Future<Long> future : resultList) {
                        totalCost += future.get();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    pool.shutdown();
                }
            } else {
                LOGGER.warn("index_not_found, index: {}.", searchIndex);
            }
        }
        LOGGER.info("运行共计耗时, " + totalCost + "ms, " + totalCost / 1000.0 + "s, "
                + NumberUtils.toFixed(2, totalCost / 1000.0 / 60.0) + "min.");
        if (ConfigParams.UPDATE_PROJECT_ON) {
            long start = System.currentTimeMillis();
            LOGGER.info("更新进程中的project, job等信息");
            ProjectFixedService service = new ProjectFixedService();
            for (String day : dayList) {
                for (String clusterName : ConfigParams.CLUSTER_NAME) {
                    try {
                        service.fixProcInfo(clusterName, day);
                    } catch (Exception e) {
                        LOGGER.error(e.toString());
                    }
                }
            }
            long total = System.currentTimeMillis() - start;
            LOGGER.info("update proc info, 运行共计耗时, " + total + "ms, " + total / 1000.0 + "s, "
                    + NumberUtils.toFixed(2, total / 1000.0 / 60.0) + "min.");
        }

        if (ConfigParams.PROC_AGGS_ON) {
            for (String day : dayList) {
                long start = System.currentTimeMillis();
                ProcPersistenceService service = new ProcPersistenceService();
                service.getCpuTimeAggs(ConfigParams.CLUSTER_NAME, day);
                long total = System.currentTimeMillis() - start;
                LOGGER.info("get cpu core time, 运行共计耗时, " + total + "ms, " + total / 1000.0 + "s, "
                        + NumberUtils.toFixed(2, total / 1000.0 / 60.0) + "min.");
            }
        }
    }
}
