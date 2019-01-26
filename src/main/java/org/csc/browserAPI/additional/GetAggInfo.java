package org.csc.browserAPI.additional;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.csc.backend.ordbgens.bc.entity.ZCBcBlock;
import org.csc.backend.ordbgens.bc.entity.ZCBcBlockExample;
import org.csc.browserAPI.Helper.AddressHelper;
import org.csc.browserAPI.Helper.BlockHelper;
import org.csc.browserAPI.Helper.BrowserBlockHelper;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Additional;
import org.csc.browserAPI.gens.Address;
import org.csc.browserAPI.gens.Block;
import org.csc.browserAPI.gens.Tx;

@NActorProvider
@Slf4j
@Data
public class GetAggInfo extends SessionModules<Additional.ReqGetAggInfo> {

    @ActorRequire(name = "addressHelper", scope = "global")
    AddressHelper addressHelper;
    @ActorRequire(name = "browserapi.blockHelper", scope = "global")
    BlockHelper blockHelper;
    @ActorRequire(name = "browser_blockHelper", scope = "global")
    BrowserBlockHelper browserBlockHelper;
    @ActorRequire(name = "browserapi.daos", scope = "global")
    Daos daos;

    @Override
    public String[] getCmds() {
        return new String[] { Additional.PADICommand.AGG.name() };
    }

    @Override
    public String getModule() {
        return Additional.PADIModule.PAM.name();
    }

    @Override
    public void onPBPacket(final FramePacket pack, final Additional.ReqGetAggInfo pb, final CompleteHandler handler) {
        Additional.ResGetAggInfo.Builder ret = Additional.ResGetAggInfo.newBuilder();
        String keyword = pb.getKeyword();
        ret.setRplCode(1);
        if(StringUtils.isBlank(keyword)){
            log.info("入参信息为空 返回1");
            handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
            return;
        }
        Address.AddressInfo addressInfo = addressHelper.getAccountByAddress(keyword);
        if(addressInfo != null){
            ZCBcBlockExample example = new ZCBcBlockExample();
            example.createCriteria().andBmAddressEqualTo(keyword);
            int count = daos.getBcBlockDao().countByExample(example);
            ret.setResult("1");
            if(count>0){
                //矿工的地址
                ret.setResult("2");
            }
            ret.setAddressInfo(addressInfo);
            handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
            return;
        }
        ZCBcBlock blockEntity = blockHelper.getBlockByBlockHash(keyword);
        if(blockEntity != null){
            ret.setResult("5");
            ret.setBlock(getBlock(blockEntity));
            handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
            return;
        }
        boolean checkResult = NumberUtils.isCreatable(keyword);
        if(checkResult) {
            blockEntity = blockHelper.getBlockByBlockNumber(Integer.parseInt(keyword));
            if (blockEntity != null) {
                ret.setResult("4");
                ret.setBlock(getBlock(blockEntity));
                handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
                return;
            }
        }
        Tx.Transaction transaction = browserBlockHelper.getTxByTxHashFromDB(keyword);
        if(transaction!=null ){
            ret.setResult("3");
            ret.setTxs(transaction);
            handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
            return;
        }
        ret.setResult("0");
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
        return;
    }

    /**
     * 解析区块
     * @param blockEntity
     * @return
     */
    private Block.BlockInfo.Builder getBlock(ZCBcBlock blockEntity){
        Block.BlockHeader.Builder header = blockHelper.serializeBlockHeader(blockEntity, null);
        Block.BlockInfo.Builder blockInfo = Block.BlockInfo.newBuilder();
        blockInfo.setHeader(header);
        return blockInfo;
    }
}