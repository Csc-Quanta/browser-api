package org.csc.browserAPI.block;

import java.util.List;

import org.csc.backend.ordbgens.bc.entity.ZCBcBlock;
import org.csc.browserAPI.Helper.BlockHelper;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Block.BlockHeader;
import org.csc.browserAPI.gens.Block.BlockInfo;
import org.csc.browserAPI.gens.Block.PBLKCommand;
import org.csc.browserAPI.gens.Block.PBLKTModule;
import org.csc.browserAPI.gens.Block.ReqGetBatchBlocks;
import org.csc.browserAPI.gens.Block.ResGetBatchBlocks;

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
public class GetBatchBlocks extends SessionModules<ReqGetBatchBlocks> {

	@ActorRequire(name = "browserapi.blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@ActorRequire(name = "browserapi.daos", scope = "global")
	Daos daos;

	@Override
	public String[] getCmds() {
		return new String[] { PBLKCommand.GBB.name() };
	}

	@Override
	public String getModule() {
		return PBLKTModule.BOK.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBatchBlocks pb, final CompleteHandler handler) {
		ResGetBatchBlocks.Builder ret = ResGetBatchBlocks.newBuilder();
		// 默认参数

		ret.setTotalCount(blockHelper.getBlockCount());

		List<Object> list = blockHelper.getBatchBlocks(pb.getPageNo(), pb.getPageSize());

		if (list != null && !list.isEmpty()) {
			for (Object o : list) {
				ZCBcBlock block = (ZCBcBlock) o;
				BlockHeader.Builder  header = BlockHeader.newBuilder();
				header = blockHelper.serializeBlockHeader(block, null);
				BlockInfo.Builder blockInfo = BlockInfo.newBuilder();
				blockInfo.setHeader(header);
				ret.addBlocks(blockInfo);
			}
		}

		ret.setRet(1);
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
