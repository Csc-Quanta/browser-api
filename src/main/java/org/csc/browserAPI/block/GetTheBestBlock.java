package org.csc.browserAPI.block;

import org.csc.browserAPI.Helper.BlockHelper;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Block.BlockInfo;
import org.csc.browserAPI.gens.Block.PBLKCommand;
import org.csc.browserAPI.gens.Block.PBLKTModule;
import org.csc.browserAPI.gens.Block.ReqGetTheBestBlock;
import org.csc.browserAPI.gens.Block.ResGetTheBestBlock;

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
public class GetTheBestBlock extends SessionModules<ReqGetTheBestBlock> {

	@ActorRequire(name = "browserapi.blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@ActorRequire(name = "browserapi.daos", scope = "global")
	Daos daos;
	
	@Override
	public String[] getCmds() {
		return new String[] { PBLKCommand.GTB.name() };
	}

	@Override
	public String getModule() {
		return PBLKTModule.BOK.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTheBestBlock pb, final CompleteHandler handler) {
		ResGetTheBestBlock.Builder ret = ResGetTheBestBlock.newBuilder();
		BlockInfo block = blockHelper.getTheBestBlock();
		if (block != null) {
			ret.setBlock(block);
		}
		ret.setRet(1);

		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}
