package com.paratera.sgri.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.paratera.sgri.config.ConfigParams;

public final class DayUtils {
    
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");
    
    /**
     * 默认构造函数.
     */
    private DayUtils() {
    }

    /**
     * day: 20170101
     * return: [０点的开始时间, 0点的结束时间, １点的开始时间, 1点的结束时间, ...], 闭区间 [00:00:00.000, 00:59:59:999]
     * @throws ParseException 
     */
    public static long[] getHoursRange(String day, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        long time = sdf.parse(day).getTime();
        long[] times = new long[48];
        for (int i = 0; i < times.length; i += 2) {
            times[i] = time;
            time += 3600000;
            times[i + 1] = time - 1;
        }
        return times;
    }

    /**
     * day: 20170101
     * return: [０点的开始时间, 23点的结束时间]
     */
    public static long[] getDayRange(String day, String format) throws ParseException {
        long[] times = new long[2];
        times[0] = getDayTimestamp(day, format);
        times[1] = times[0] + 24 * 60 * 60 * 1000 - 1;
        return times;
    }

    /**
     * 将日期转换为时间戳
     */
    public static long getDayTimestamp(String day, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.parse(day).getTime();
    }

    /**
     * 获取时间天列表
     * @param dayOrMonth
     * @return
     * @throws ParseException
     */
    public static List<String> getDayList(String dayOrMonth) throws ParseException {
        List<String> dayList = new ArrayList<String>();
        if (ConfigParams.TIME_RANGE.equals(ConfigParams.TIME_RANGE_BYDAY)) {
            // 按天
            if (dayOrMonth.matches("201\\d{5,5}")) {
                dayList.add(dayOrMonth);
            } else {
                throw new RuntimeException("日期格式不正确, 格式如20170101!");
            }
        } else if (ConfigParams.TIME_RANGE.equals(ConfigParams.TIME_RANGE_BYMONTH)) {
            // 按月
            if (dayOrMonth.matches("201\\d{3,3}")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
                Date beGindate = sdf.parse(dayOrMonth); //本月 第一天
                Date tmpDate = DateUtils.addDays(beGindate, 32); // 肯定是下月了
                tmpDate = DateUtils.setDays(tmpDate, 1); // 下月第一天
                Date endDate = DateUtils.addDays(tmpDate, -1); //本月最後一天
                int i = 0;
                tmpDate = beGindate;
                while (tmpDate.getTime() < endDate.getTime()) {
                    tmpDate = DateUtils.addDays(beGindate, i++);
                    String day = DateFormatUtils.format(tmpDate, "yyyyMMdd");
                    dayList.add(day);
                }
            } else {
                throw new RuntimeException("日期格式不正确, 格式如201701!");
            }
        }
        return dayList;
    }
    
    /**
     * 获取开始时间-结束时间之间的天列表
     * 
     * @throws ParseException
     */
    public static List<String> getDayList(String startDay, String endDay) throws ParseException {
        Date start = SDF.parse(startDay);
        Date end = SDF.parse(endDay);
        List<String> dayList = new ArrayList<String>();
        Date curr = new Date(start.getTime());
        while (curr.getTime() <= end.getTime()) {
            dayList.add(DateFormatUtils.format(curr, "yyyyMMdd"));
            curr = org.apache.commons.lang3.time.DateUtils.addDays(curr, 1);
        }
        return dayList;
    }
    
    /**
     * 增加或者减少的天数
     */
    public static Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }
}