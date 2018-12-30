package org.csc.browserAPI.additional;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.apache.commons.lang3.math.NumberUtils;
import org.csc.browserAPI.Helper.AdditionalHelper;
import org.csc.browserAPI.gens.Additional;
import org.csc.browserAPI.gens.Additional.PADICommand;
import org.csc.browserAPI.gens.Additional.PADIModule;
import org.csc.browserAPI.gens.Additional.ReqGetNodes;

@NActorProvider
@Slf4j
@Data
public class GetNodes extends SessionModules<ReqGetNodes>{

	@ActorRequire(name = "browser.additionalHelper", scope = "global")
	AdditionalHelper additionalHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PADICommand.GNS.name() };
	}

	@Override
	public String getModule() {
		return PADIModule.PAM.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetNodes pb, final CompleteHandler handler) {
		int pageNo = NumberUtils.INTEGER_ZERO.equals(pb.getPageNo())?1:pb.getPageNo();
		int pageSize = NumberUtils.INTEGER_ZERO.equals(pb.getPageSize())?10:pb.getPageSize();
		Additional.ResGetNodes.Builder builde = additionalHelper.getNodes(pageSize,pageNo);
		if(builde == null ){
			builde = Additional.ResGetNodes.newBuilder();
		}
		builde.setRet(1);
		handler.onFinished(PacketHelper.toPBReturn(pack, builde.build()));
	}
}
