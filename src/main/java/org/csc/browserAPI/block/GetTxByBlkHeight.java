package org.csc.browserAPI.block;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.apache.commons.lang3.math.NumberUtils;
import org.csc.backend.ordbgens.bc.entity.ZCBcBlock;
import org.csc.browserAPI.Helper.BlockHelper;
import org.csc.browserAPI.gens.Block;
import org.csc.browserAPI.gens.Tx;

import java.util.List;
import java.util.Map;

@NActorProvider
@Slf4j
@Data
public class GetTxByBlkHeight extends SessionModules<Block.ReqGetTxByBlkHeight> {

    @ActorRequire(name = "browserapi.blockHelper", scope = "global")
    BlockHelper blockHelper;
    @Override
    public String[] getCmds() {
        return new String[] { Block.PBLKCommand.GBX.name() };
    }

    @Override
    public String getModule() {
        return Block.PBLKTModule.BOK.name();
    }

    @Override
    public void onPBPacket(final FramePacket pack, final Block.ReqGetTxByBlkHeight pb, final CompleteHandler handler) {
        int pageSize = NumberUtils.INTEGER_ZERO.equals(pb.getPageSize())?10:pb.getPageSize();
        int pageNo = NumberUtils.INTEGER_ZERO.equals(pb.getPageNo())?1:pb.getPageNo();
        Block.ResGetTxByBlkHeight.Builder ret = Block.ResGetTxByBlkHeight.newBuilder();
        ZCBcBlock blockEntity = blockHelper.getBlockByBlockNumber(pb.getBlockNumber());
        Map<String,Object> resultMap = blockHelper.oBlock2BlockInfo(blockEntity,pageSize,pageNo);
        if(resultMap == null){
            log.info("查询结果为null ，不存在的区块高度：{}",pb.getBlockNumber());
            ret.setRet(1);
            return;
        }
        ret.addAllTxs((List<Tx.Transaction>)resultMap.get("list"));
        ret.setTotalCount((Integer) resultMap.get("count"));
        ret.setRet(1);
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
    }
}
