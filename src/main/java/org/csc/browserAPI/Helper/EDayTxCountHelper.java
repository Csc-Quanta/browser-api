package org.csc.browserAPI.Helper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Additional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每日交易笔数报表
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "eDayTxCountHelper")
@Slf4j
@Data
public class EDayTxCountHelper extends AbReportHelp implements ActorService {
    @ActorRequire(name = "browserapi.daos")
    Daos daos;
    @Override
    public List<Additional.ReportResult> queryReport(Additional.ReqGetReportInfo reqGetReportInfo) {
        //需要查询的天数
        int day = reqGetReportInfo.getSplice();
        ReportDto dto = getTx(daos,day);
        while (dto.getIterator().hasNext()){
            Additional.ReportResult.Builder builder = Additional.ReportResult.newBuilder();
            Map.Entry<String, List<MutilTransaction>> car = dto.getIterator().next();
            for(Additional.ReportResult.Builder reportResult : dto.getReportResults()){
                if(reportResult.getDataTime().equals(car.getKey())){
                    reportResult.setData(String.valueOf(car.getValue().size()));
                }
            }
        }
        return dto.getReportResults().stream().map(Additional.ReportResult.Builder::build)
                .sorted(Comparator.comparing(Additional.ReportResult::getDataTime))
                .collect(Collectors.toList());
    }
}
