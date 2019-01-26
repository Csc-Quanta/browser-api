package org.csc.browserAPI.Helper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.backend.ordbgens.bc.entity.ZCBcMtxInput;
import org.csc.backend.ordbgens.bc.entity.ZCBcMtxInputExample;
import org.csc.backend.ordbgens.bc.entity.ZCBcMutilTransactionExample;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Tx;
import org.csc.browserAPI.util.NumUtil;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "txHelper")
@Slf4j
@Data
public class TxHelper implements ActorService {

    @ActorRequire(name = "browserapi.daos")
    Daos daos;

    /**
     * 查询交易信息
     * @param builder
     */
    public void getTxCountInfo(Tx.ResGetTxCountInfo.Builder builder){
        ZCBcMutilTransactionExample example = new ZCBcMutilTransactionExample();
        example.createCriteria().andTxStatusEqualTo("1")
                .andTxTypeNotEqualTo(888);
        int txCount = daos.getBcMultiTransactionDao().countByExample(example);
        ZCBcMtxInputExample inputExample = new ZCBcMtxInputExample();
        inputExample.createCriteria().andTxStatusEqualTo("1");
        inputExample.setSelectCols("SUM(AMOUNT) AMOUNT");
        ZCBcMtxInput zcBcMtxInput = (ZCBcMtxInput) daos.bcMtxInputDao.selectOneByExample(inputExample);
        builder.setTxCount(txCount);
        builder.setTxAmountCount(NumUtil.dealZero(zcBcMtxInput.getAmount().toString(),8));
    }
}
