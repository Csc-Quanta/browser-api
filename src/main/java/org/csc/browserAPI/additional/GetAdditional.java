package org.csc.browserAPI.additional;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.csc.browserAPI.Helper.AdditionalHelper;
import org.csc.browserAPI.gens.Additional;
import org.csc.browserAPI.gens.Additional.PADICommand;
import org.csc.browserAPI.gens.Additional.PADIModule;
import org.csc.browserAPI.gens.Additional.ResGetAdditional;

@NActorProvider
@Slf4j
@Data
public class GetAdditional extends SessionModules<Additional.ReqGetAdditional>{

	@ActorRequire(name = "browser.additionalHelper", scope = "global")
	AdditionalHelper additionalHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PADICommand.GET.name() };
	}

	@Override
	public String getModule() {
		return PADIModule.PAM.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final Additional.ReqGetAdditional pb, final CompleteHandler handler) {
		ResGetAdditional.Builder ret = ResGetAdditional.newBuilder();
		ret = additionalHelper.getAdditional();
		ret.setRet(1);
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
