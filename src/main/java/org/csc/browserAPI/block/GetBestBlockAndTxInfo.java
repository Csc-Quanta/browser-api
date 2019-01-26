package org.csc.browserAPI.block;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.csc.browserAPI.Helper.BlockHelper;
import org.csc.browserAPI.Helper.TxHelper;
import org.csc.browserAPI.gens.Block;
import org.csc.browserAPI.gens.Tx;

@NActorProvider
@Slf4j
@Data
public class GetBestBlockAndTxInfo extends SessionModules<Block.ReqGetTheBestBlock> {

    @ActorRequire(name = "browserapi.blockHelper", scope = "global")
    BlockHelper blockHelper;
    @ActorRequire(name = "txHelper")
    TxHelper txHelper;
    @Override
    public String[] getCmds() {
        return new String[] { Block.PBLKCommand.GBT.name() };
    }

    @Override
    public String getModule() {
        return Block.PBLKTModule.BOK.name();
    }

    @Override
    public void onPBPacket(final FramePacket pack, final Block.ReqGetTheBestBlock pb, final CompleteHandler handler) {
        Block.ResGetBestBlockAndTxInfo.Builder ret = Block.ResGetBestBlockAndTxInfo.newBuilder();
        Tx.ResGetTxCountInfo.Builder builder = Tx.ResGetTxCountInfo.newBuilder();
        Block.BlockInfo block = blockHelper.getTheBestBlock();
        if (block != null) {
            ret.setBlock(block);
        }
        txHelper.getTxCountInfo(builder);
        ret.setRplCode(1);
        ret.setTxAmountCount(builder.getTxAmountCount());
        ret.setTxCount(builder.getTxCount());
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
    }
}
