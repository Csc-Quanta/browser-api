package org.csc.browserAPI.block;


import org.apache.commons.lang3.StringUtils;
import org.csc.browserAPI.Helper.BlockHelper;
import org.csc.browserAPI.gens.Block.BlockInfo;
import org.csc.browserAPI.gens.Block.PBLKCommand;
import org.csc.browserAPI.gens.Block.PBLKTModule;
import org.csc.browserAPI.gens.Block.ReqGetBlockByTxHash;
import org.csc.browserAPI.gens.Block.ResGetBlockByTxHash;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class GetBlockByTxHash extends SessionModules<ReqGetBlockByTxHash>{

	@ActorRequire(name = "browserapi.blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PBLKCommand.GTH.name() };
	}

	@Override
	public String getModule() {
		return PBLKTModule.BOK.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBlockByTxHash pb, final CompleteHandler handler) {
		ResGetBlockByTxHash.Builder ret = ResGetBlockByTxHash.newBuilder();
		if(pb != null && StringUtils.isNotBlank(pb.getTxHash())){
			BlockInfo block = blockHelper.getBlockByTxHash(pb.getTxHash());
			if(block != null){
				ret.setBlock(block);
			}
		}
		ret.setRet(1);
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	
}
