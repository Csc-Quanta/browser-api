package org.csc.browserAPI.Helper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.backend.ordbgens.bc.entity.ZCBcMtxInput;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Additional;
import org.csc.browserAPI.util.NumUtil;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

/**
 * 每日交易总额报表
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "eDayTxAmountCountHelper")
@Slf4j
@Data
public class EDayTxAmountCountHelper extends AbReportHelp implements ActorService{
    @ActorRequire(name = "browserapi.daos")
    Daos daos;
    @Override
    public List<Additional.ReportResult> queryReport(Additional.ReqGetReportInfo reqGetReportInfo) {
        int day = reqGetReportInfo.getSplice();
        ReportDto dto = getTx(daos,day);
        Map<String,Future<List<ZCBcMtxInput>>> results = new HashMap();
        while (dto.getIterator().hasNext()){
            Map.Entry<String,List<MutilTransaction>> car= dto.getIterator().next();
            List<MutilTransaction> vList = car.getValue();
            List<String> list = new ArrayList<>();
            vList.stream().forEach(tx->{
                list.add(tx.getTxHash());
            });
            results.put(car.getKey(),executorService.submit(new ExecutorsThread(list,daos)));
        }
        Iterator<Map.Entry<String,Future<List<ZCBcMtxInput>>>> entryIterator = results.entrySet().iterator();
        List<Additional.ReportResult.Builder> list = dto.getReportResults();
        while (entryIterator.hasNext()){
            Map.Entry<String,Future<List<ZCBcMtxInput>>> car = entryIterator.next();
            try {
                BigInteger bigInteger = BigInteger.ZERO;
                for(ZCBcMtxInput bcMtxInput : car.getValue().get()){
                    bigInteger = bigInteger.add(bcMtxInput.getAmount().toBigInteger());
                }
                for(Additional.ReportResult.Builder reportResult : dto.getReportResults()){
                    if(reportResult.getDataTime().equals(car.getKey())){
                        BigDecimal bigDecimal = new BigDecimal(bigInteger.toString());
                        reportResult.setData(NumUtil.dealZero(bigDecimal.toString(),8));
                    }
                }
            } catch(Exception e) {
                log.error("获取当前日：{} 交易额失败:",car.getKey(),e);
            }
        }
        return list.stream().map(Additional.ReportResult.Builder::build)
                .sorted(Comparator.comparing(Additional.ReportResult::getDataTime))
                .collect(Collectors.toList());
    }
}


