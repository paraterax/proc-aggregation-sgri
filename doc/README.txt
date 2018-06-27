程序实现的功能：（离线统计，非实时统计）
	计算某一天中有哪些进程在运行，　并计算这些进程在该天中占用的核时
	某一天指：　至少昨天
	核时计算：
		p_time　= 进程在某天内的平均cpu利用率*进程在某天内的运行时间
		cpu_time = 某个进程在某天内的最后一条数据的str_time　- 某个进程在某天内的第一条数据的str_time

查看集群, 可能有新增的集群
curl 172.16.203.3:9334/_cat/indices|grep proc|grep 2017 >> c
more c|awk '{print $3}'|cut -f1 -d \-|sort -ru
more c|awk '{print $2}'|cut -f1 -d \-|sort -ru
		
ALTER TABLE `sghpdw_proctime_201702` ADD INDEX `hostname` (`hostname`) ;
ALTER TABLE `sghpdw_proctime_201702` ADD INDEX `time_range` (`time_range`) ;

ALTER TABLE `sghpew_proctime_201702` ADD INDEX `hostname` (`hostname`) ;
ALTER TABLE `sghpew_proctime_201702` ADD INDEX `time_range` (`time_range`) ;

ALTER TABLE `sgibdw_proctime_201702` ADD INDEX `hostname` (`hostname`) ;
ALTER TABLE `sgibdw_proctime_201702` ADD INDEX `time_range` (`time_range`) ;

ALTER TABLE `sgibew_proctime_201702` ADD INDEX `hostname` (`hostname`) ;
ALTER TABLE `sgibew_proctime_201702` ADD INDEX `time_range` (`time_range`) ;

ALTER TABLE `sginbw_proctime_201702` ADD INDEX `hostname` (`hostname`) ;
ALTER TABLE `sginbw_proctime_201702` ADD INDEX `time_range` (`time_range`) ;

ALTER TABLE `sgincw_proctime_201702` ADD INDEX `hostname` (`hostname`) ;
ALTER TABLE `sgincw_proctime_201702` ADD INDEX `time_range` (`time_range`) ;

ALTER TABLE `sgindw_proctime_201702` ADD INDEX `hostname` (`hostname`) ;
ALTER TABLE `sgindw_proctime_201702` ADD INDEX `time_range` (`time_range`) ;

ALTER TABLE `sgingcq_proctime_201702` ADD INDEX `hostname` (`hostname`) ;
ALTER TABLE `sgingcq_proctime_201702` ADD INDEX `time_range` (`time_range`) ;

ALTER TABLE `sgsgaw_proctime_201702` ADD INDEX `hostname` (`hostname`) ;
ALTER TABLE `sgsgaw_proctime_201702` ADD INDEX `time_range` (`time_range`) ;