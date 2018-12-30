package org.csc.browserAPI.tx;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.apache.commons.lang3.math.NumberUtils;
import org.csc.backend.ordbgens.bc.entity.ZCBcMtxInput;
import org.csc.backend.ordbgens.bc.entity.ZCBcMtxInputExample;
import org.csc.backend.ordbgens.bc.entity.ZCBcMutilTransaction;
import org.csc.backend.ordbgens.bc.entity.ZCBcMutilTransactionExample;
import org.csc.browserAPI.common.Constant;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Tx;
import org.csc.browserAPI.util.NumUtil;

import java.util.ArrayList;
import java.util.List;

@NActorProvider
@Slf4j
@Data
public class GetAllTxList extends SessionModules<Tx.ReqGetTxList> {
    @ActorRequire(name = "browserapi.daos")
    Daos daos;
    @Override
    public String[] getCmds() {
        return new String[] { Tx.PTRSCommand.TXL.name() };
    }

    @Override
    public String getModule() {
        return Tx.PTRSModule.TXS.name();
    }
    @Override
    public void onPBPacket(final FramePacket pack, final Tx.ReqGetTxList pb, final CompleteHandler handler) {
        Tx.ResGetTxList.Builder ret = Tx.ResGetTxList.newBuilder();
        Integer pageSize = NumberUtils.INTEGER_ZERO.equals(pb.getPageSize())?10:pb.getPageSize();
        Integer pageNo = NumberUtils.INTEGER_ZERO.equals(pb.getPageNo())?1:pb.getPageNo();
        //开始计算分页数据
        int scol = pageSize * (pageNo - 1);
        ZCBcMutilTransactionExample bcMutilTransactonExample = new ZCBcMutilTransactionExample();
        bcMutilTransactonExample.setOrderByClause("BLOCK_HEIGHT DESC");
        bcMutilTransactonExample.createCriteria().andTxTypeNotEqualTo(Constant.COIN_BASE_TRANS_TYPE)
        .andTxStatusEqualTo("1");
        bcMutilTransactonExample.setOffset(scol);
        bcMutilTransactonExample.setLimit(pageSize);
        log.debug("bcMutilTransactonExample 查询条件：{}",bcMutilTransactonExample.toString());
        //分页查询
        List<Object> objects = daos.getBcMultiTransactionDao()
                .selectByExample(bcMutilTransactonExample);
        if(objects==null || objects.isEmpty()){
            ret.setRet(1);
            handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
            return;
        }
        List<ZCBcMutilTransaction> list = JSON.parseArray(JSON.toJSONString(objects),ZCBcMutilTransaction.class);
        //进行转换
        List<Tx.Transaction> txList = new ArrayList<>();
        for (ZCBcMutilTransaction bcMutilTransacton : list){
            Tx.Transaction.Builder transaction = Tx.Transaction.newBuilder();
            transaction.setBlockNumber(bcMutilTransacton.getBlockHeight().longValue());
            transaction.setStatus(bcMutilTransacton.getTxStatus());
            transaction.setTxHash(bcMutilTransacton.getTxHash());
            transaction.setTimeStamp(bcMutilTransacton.getTxTimestamp().longValue());
            ZCBcMtxInputExample bcMtxInputExample = new ZCBcMtxInputExample();
            bcMtxInputExample.createCriteria().andMtxHashEqualTo(bcMutilTransacton.getTxHash());
            bcMtxInputExample.setGroupByClause("MTX_HASH");
            bcMtxInputExample.setSelectCols("SUM(AMOUNT) as amount");
            Object object = daos.getBcMtxInputDao().selectOneByExample(bcMtxInputExample);
            if(object == null){
                transaction.setTotalAmount(String.valueOf(NumberUtils.INTEGER_ZERO));
                txList.add(transaction.build());
                continue;
            }
            ZCBcMtxInput bcMtxInput = (ZCBcMtxInput) object;
            transaction.setTotalAmount(NumUtil.dealZero(bcMtxInput.getAmount().toString(),8));
            txList.add(transaction.build());
        }
        bcMutilTransactonExample = new ZCBcMutilTransactionExample();
        bcMutilTransactonExample.createCriteria().andTxTypeNotEqualTo(Constant.COIN_BASE_TRANS_TYPE)
                .andTxStatusEqualTo("1");
        int totalCount = daos.getBcMultiTransactionDao().countByExample(bcMutilTransactonExample);
        ret.setRet(1);
        ret.addAllTxs(txList);
        ret.setTotalCount(totalCount);
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
    }

}
