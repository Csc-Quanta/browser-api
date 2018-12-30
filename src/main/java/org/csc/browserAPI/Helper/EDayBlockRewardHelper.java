package org.csc.browserAPI.Helper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.backend.ordbgens.bc.entity.ZCBcBlockExample;
import org.csc.backend.ordbgens.bc.entity.ZCBcMutilTransactionExample;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Additional;
import org.csc.browserAPI.util.NumUtil;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "eDayBlockRewardHelper")
@Slf4j
@Data
public class EDayBlockRewardHelper extends AbReportHelp implements ActorService {
    @ActorRequire(name = "browserapi.daos")
    Daos daos;
    ForkJoinPool forkJoinPool = new ForkJoinPool();
    @Override
    public List<Additional.ReportResult> queryReport(Additional.ReqGetReportInfo reqGetReportInfo) {
        //这个是获取多少天数
        int day = reqGetReportInfo.getSplice();
        Date startDate = DateUtils.addDays(new Date(),-day);
        Date yesterDay = DateUtils.addDays(new Date(),-1);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ZCBcMutilTransactionExample mutilTransactionExample = new ZCBcMutilTransactionExample();
        mutilTransactionExample.createCriteria().andTxStatusEqualTo("1");
        int count = daos.getBcMultiTransactionDao().countByExample(mutilTransactionExample);
        //准备天数数据
        List<Additional.ReportResult.Builder> dayList = getResultBuilder(day,simpleDateFormat,startDate);
        ZCBcBlockExample example = new ZCBcBlockExample();
        example.createCriteria().andBhTimestampBetween(new BigDecimal(startDate.getTime())
                ,new BigDecimal(yesterDay.getTime())).andBlockStatusEqualTo("1");
        Future<List<BCBlockBean>> future = forkJoinPool.submit(new QueryTask(startDate.getTime(),yesterDay.getTime(),daos,0,count));
        List<BCBlockBean> bcBlocks = new ArrayList<>();
        try {
            bcBlocks = future.get();
        } catch (Exception e) {
            log.error("查询区块奖励报表失败：",e);
        }
        bcBlocks.forEach(ele->{
            ele.setGroupByTime(simpleDateFormat.format(new Date(ele.getBhTimestamp().longValue())));
        });
        Map<String,List<BCBlockBean>> resultMap = bcBlocks.stream().collect(Collectors.groupingBy(BCBlockBean::getGroupByTime));
        Iterator<Map.Entry<String,List<BCBlockBean>>> iterator = resultMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String,List<BCBlockBean>> car = iterator.next();
            BigInteger bigInteger = BigInteger.ZERO;
            for(BCBlockBean bcBlockBean : car.getValue()){
                bigInteger = bigInteger.add(new BigInteger(bcBlockBean.getBmRewardHex(),16));
            }
            for(Additional.ReportResult.Builder reportResult : dayList){
                if(reportResult.getDataTime().equals(car.getKey())){
                    reportResult.setData(NumUtil.dealZero(bigInteger.toString(),8));
                }
            }
        }
        return dayList.stream().map(Additional.ReportResult.Builder::build)
                .sorted(Comparator.comparing(Additional.ReportResult::getDataTime))
                .collect(Collectors.toList());
    }
}
