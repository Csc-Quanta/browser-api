package org.csc.browserAPI.Helper;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.csc.backend.ordbgens.bc.entity.ZCBcBlock;
import org.csc.backend.ordbgens.bc.entity.ZCBcMtxInput;
import org.csc.backend.ordbgens.bc.entity.ZCBcMtxInputExample;
import org.csc.backend.ordbgens.bc.entity.ZCBcMutilTransaction;
import org.csc.backend.ordbgens.bc.entity.ZCBcMutilTransactionExample;
import org.csc.browserAPI.common.Constant;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Additional;

import com.alibaba.fastjson.JSON;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.ordb.ORDBDataService;
@Slf4j
public abstract class AbReportHelp implements QueryReportHelper{
    ExecutorService executorService = Executors.newFixedThreadPool(5);
    @Data
    static class MutilTransaction extends ZCBcMutilTransaction {
        //需要根据这个时间进行groupBy
        String groupByTime;
    }
    @Data
    static class BCBlockBean extends ZCBcBlock{
        //需要根据这个时间进行groupBy
        String groupByTime;
    }
    @Data
    @Builder
    static class ReportDto{
        List<Additional.ReportResult.Builder> reportResults;
        Iterator<Map.Entry<String,List<MutilTransaction>>> iterator;
    }
    class ExecutorsThread implements Callable<List<ZCBcMtxInput>> {
        List<String> txHash;
        Daos daos;
        public ExecutorsThread(List<String> txHash,Daos daos){
            this.txHash = txHash;
            this.daos = daos;
        }
        @Override
        public List<ZCBcMtxInput> call(){
            ZCBcMtxInputExample example = new ZCBcMtxInputExample();
            example.createCriteria().andMtxHashIn(txHash);
            List<Object> objects = daos.getBcMtxInputDao().selectByExample(example);
            List<ZCBcMtxInput> bcMtxInputs = JSON.parseArray(JSON.toJSONString(objects),ZCBcMtxInput.class);
            return bcMtxInputs;
        }
    }
    /**
     * 查询指定天数的交易信息 不包含挖矿
     * @param day 指定天数
     * @return
     */
    public ReportDto getTx(Daos daos,int day){
        Date now = new Date();
        Date date = DateUtils.addDays(now,-day);
        Date yesterDay = DateUtils.addDays(now,-1);
        ZCBcMutilTransactionExample example = new ZCBcMutilTransactionExample();
        example.createCriteria().andTxTimestampBetween(new BigDecimal(date.getTime()),new BigDecimal(yesterDay.getTime()))
                .andTxStatusEqualTo("1").andTxTypeNotEqualTo(Constant.COIN_BASE_TRANS_TYPE);//
        List<Object> objectList = daos.getBcMultiTransactionDao().selectByExample(example);
        List<MutilTransaction> mutilTransactions = JSON.parseArray(JSON.toJSONString(objectList), MutilTransaction.class);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        //防止某天无记录
        List<Additional.ReportResult.Builder> dayList = getResultBuilder(day,simpleDateFormat,date);
        for (MutilTransaction mutilTransaction : mutilTransactions){
            mutilTransaction.setGroupByTime(simpleDateFormat.format(new Date(mutilTransaction.getTxTimestamp().longValue())));
        }
        //排个序
        Map<String,List<MutilTransaction>> groupByMap = mutilTransactions.stream().collect(Collectors.groupingBy(MutilTransaction::getGroupByTime));
        Iterator<Map.Entry<String,List<MutilTransaction>>> iterator = groupByMap.entrySet().iterator();
        return ReportDto.builder().iterator(iterator).reportResults(dayList).build();
    }

    /**
     * 获取天数
     * @param day
     * @param simpleDateFormat
     * @param date
     * @return
     */
    List<Additional.ReportResult.Builder> getResultBuilder(int day,SimpleDateFormat simpleDateFormat,Date date){
        List<Additional.ReportResult.Builder> dayList = new ArrayList<>();
        for (int i = 0; i < day; i++) {
            Additional.ReportResult.Builder builder = Additional.ReportResult.newBuilder();
            builder.setDataTime(simpleDateFormat.format(DateUtils.addDays(date, i)));
            builder.setData("0");
            dayList.add(builder);
        }
        return dayList;
    }
    /**
     * 查询区块的数据
     */
	static class QueryTask extends RecursiveTask<List<BCBlockBean>> {
        private long startTime;
        private long endTime;
        private int pageSize = 5000;
        private Daos daos;
        private int startCount;
        private int count;
        public QueryTask(long startTime,long endTime,Daos daos,int startCount,int count){
            this.startTime = startTime;
            this.daos = daos;
            this.endTime = endTime;
            this.startCount = startCount;
            this.count = count;
        }
        private String getSql(){
            return "select * from (select * from zc_bc_block where BLOCK_STATUS = '1'  LIMIT "+startCount+","+(count-startCount)+") temp " +
                "where temp.BH_TIMESTAMP >= "+startTime+" and  temp.BH_TIMESTAMP <="+endTime+"";
        }
        @Override
        protected List<BCBlockBean> compute() {
            int resultCount = pageSize+startCount;
            if(resultCount>=count){
                ORDBDataService dataService = (ORDBDataService) daos.getBcBlockDao().getDaosupport();
                String sql = getSql();
                log.debug("查询的sql:{}",sql);
                Object objects = dataService.doBySQL(sql);
                return JSON.parseArray(JSON.toJSONString(objects),BCBlockBean.class);
            }
            //取中间时间
            int middleCount = (count+startCount)/2;
            QueryTask taskLeft = new QueryTask(startTime, endTime, daos,startCount,middleCount);
            QueryTask taskRight = new QueryTask(startTime, endTime, daos,middleCount+1,count);
            taskLeft.fork();
            taskRight.fork();
            List<BCBlockBean> resultList = new ArrayList<>(taskLeft.join());
            resultList.addAll(taskRight.join());
            return resultList;
        }
    }
}
