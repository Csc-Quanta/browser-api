package org.csc.browserAPI.tx;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.apache.commons.lang3.StringUtils;
import org.csc.browserAPI.Helper.BrowserBlockHelper;
import org.csc.browserAPI.gens.Tx.PTRSCommand;
import org.csc.browserAPI.gens.Tx.PTRSModule;
import org.csc.browserAPI.gens.Tx.ReqGetTxByTxHash;
import org.csc.browserAPI.gens.Tx.ResGetTxByTxHash;

@NActorProvider
@Slf4j
@Data
public class GetTxByTxHash extends SessionModules<ReqGetTxByTxHash>{

	@ActorRequire(name = "browser_blockHelper", scope = "global")
	BrowserBlockHelper blockHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PTRSCommand.GTT.name() };
	}

	@Override
	public String getModule() {
		return PTRSModule.TXS.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTxByTxHash pb, final CompleteHandler handler) {
		ResGetTxByTxHash.Builder ret = ResGetTxByTxHash.newBuilder();
		
		try{
			ret.setRet(1);
			if(pb != null && StringUtils.isNotBlank(pb.getTxHash())){
				ret.setTxs(blockHelper.getTxByTxHashFromDB(pb.getTxHash()));
			}
		} catch (Exception e){
			log.error("get tx error " + e.getMessage());
			ret.setRet(-1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
