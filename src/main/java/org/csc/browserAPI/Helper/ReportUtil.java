package org.csc.browserAPI.Helper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import java.util.HashMap;
import java.util.Map;

/**
 * 报表util
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "reportUtil")
@Slf4j
@Data
public class ReportUtil implements ActorService {
    @ActorRequire(name = "eDayActiveNumHelper")
    QueryReportHelper eDayActiveNumHelper;

    @ActorRequire(name = "eDayBlockRewardHelper")
    QueryReportHelper eDayBlockRewardHelper;

    @ActorRequire(name = "eDayTxAmountCountHelper")
    QueryReportHelper eDayTxAmountCountHelper;

    @ActorRequire(name = "eDayTxCountHelper")
    QueryReportHelper eDayTxCountHelper;

    private Map<String,QueryReportHelper> reportHelperMap = new HashMap<>();

    public Map<String,QueryReportHelper> getReportHelperMap(){
        return reportHelperMap;
    }
    @Validate
    public void startUp(){
        new Thread(()->{
            while (eDayActiveNumHelper == null){
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            reportHelperMap.put("every_day_tx_count",eDayTxCountHelper);
            reportHelperMap.put("every_day_tx_amount_count",eDayTxAmountCountHelper);
            reportHelperMap.put("every_day_blk_reward_count",eDayBlockRewardHelper);
            reportHelperMap.put("every_day_active_count",eDayActiveNumHelper);
        }).start();
    }
}
