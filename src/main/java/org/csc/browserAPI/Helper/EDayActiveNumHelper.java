package org.csc.browserAPI.Helper;

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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

/**
 * 每日活跃人数报表
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "eDayActiveNumHelper")
@Slf4j
@Data
public class EDayActiveNumHelper extends AbReportHelp implements ActorService {
    @ActorRequire(name = "browserapi.daos")
    Daos daos;
    @Override
    public List<Additional.ReportResult> queryReport(Additional.ReqGetReportInfo reqGetReportInfo) {
        int day = reqGetReportInfo.getSplice();
        ReportDto dto = getTx(daos,day);
        Map<String, Future<List<ZCBcMtxInput>>> results = new HashMap();
        while (dto.getIterator().hasNext()){
            Map.Entry<String,List<MutilTransaction>> nextMap = dto.getIterator().next();
            List<String> list = new ArrayList<>();
            nextMap.getValue().forEach(tx->{
                list.add(tx.getTxHash());
            });
            results.put(nextMap.getKey(),executorService.submit(new ExecutorsThread(list,daos)));
        }
        Iterator<Map.Entry<String,Future<List<ZCBcMtxInput>>>> entryIterator = results.entrySet().iterator();
        while (entryIterator.hasNext()){
            Map.Entry<String,Future<List<ZCBcMtxInput>>> car = entryIterator.next();
            try {
                List<ZCBcMtxInput> list = car.getValue().get();
                for(Additional.ReportResult.Builder reportResult : dto.getReportResults()){
                    if(reportResult.getDataTime().equals(car.getKey())){
                        reportResult.setData(String.valueOf(list.stream()
                                .collect(Collectors.groupingBy(ZCBcMtxInput::getBcAddress)).size()));
                    }
                }
            } catch(Exception e) {
                log.error("获取当前日：{} 交易活跃数失败:",car.getKey(),e);
            }
        }
        return dto.getReportResults().stream().map(Additional.ReportResult.Builder::build)
                .sorted(Comparator.comparing(Additional.ReportResult::getDataTime))
                .collect(Collectors.toList());
    }
}
