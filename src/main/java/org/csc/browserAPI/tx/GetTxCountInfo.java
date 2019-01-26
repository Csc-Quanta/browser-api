package org.csc.browserAPI.tx;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.csc.browserAPI.Helper.TxHelper;
import org.csc.browserAPI.gens.Tx;

@NActorProvider
@Slf4j
@Data
public class GetTxCountInfo extends SessionModules<Tx.ReqGetTxCountInfo> {

    @ActorRequire(name = "txHelper")
    TxHelper txHelper;

    @Override
    public String[] getCmds() {
        return new String[] { Tx.PTRSCommand.TXC.name() };
    }

    @Override
    public String getModule() {
        return Tx.PTRSModule.TXS.name();
    }

    @Override
    public void onPBPacket(final FramePacket pack, final Tx.ReqGetTxCountInfo pb, final CompleteHandler handler) {
        Tx.ResGetTxCountInfo.Builder ret = Tx.ResGetTxCountInfo.newBuilder();

        try{
            ret.setRplCode(1);
            txHelper.getTxCountInfo(ret);
        } catch (Exception e){
            log.error("get tx error {}",e);
            ret.setRplCode(-1);
        }
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
    }
}
