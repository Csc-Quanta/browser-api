package org.csc.browserAPI.block;


import org.apache.commons.lang3.StringUtils;
import org.csc.backend.ordbgens.bc.entity.ZCBcBlock;
import org.csc.browserAPI.Helper.BlockHelper;
import org.csc.browserAPI.gens.Block.BlockHeader;
import org.csc.browserAPI.gens.Block.BlockInfo;
import org.csc.browserAPI.gens.Block.PBLKCommand;
import org.csc.browserAPI.gens.Block.PBLKTModule;
import org.csc.browserAPI.gens.Block.ReqGetBlockByBlockHash;
import org.csc.browserAPI.gens.Block.ResGetBlockByBlockHash;

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
public class GetBlockByBlockHash extends SessionModules<ReqGetBlockByBlockHash>{

	@ActorRequire(name = "browserapi.blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PBLKCommand.GHA.name() };
	}

	@Override
	public String getModule() {
		return PBLKTModule.BOK.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBlockByBlockHash pb, final CompleteHandler handler) {
		ResGetBlockByBlockHash.Builder ret = ResGetBlockByBlockHash.newBuilder();
		if(pb != null && StringUtils.isNotBlank(pb.getBlockHash())){
			ZCBcBlock blockEntity = blockHelper.getBlockByBlockHash(pb.getBlockHash());
			if(blockEntity != null){
				BlockHeader.Builder header = blockHelper.serializeBlockHeader(blockEntity, null);
				
				BlockInfo.Builder blockInfo = BlockInfo.newBuilder();
				blockInfo.setHeader(header);
				ret.setBlock(blockInfo);
			}
			
			ret.setRet(1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
