package org.csc.browserAPI.account;


import com.google.protobuf.ByteString;
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
import org.csc.bcapi.EncAPI;
import org.csc.browserAPI.Helper.AddressHelper;
import org.csc.browserAPI.gens.Address.*;

@NActorProvider
@Slf4j
@Data
public class GetAccountDetailByAddress extends SessionModules<ReqGetAddrDetailByAddr>{

	@ActorRequire(name = "addressHelper", scope = "global")
	AddressHelper addressHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PADRCommand.GAD.name() };
	}

	@Override
	public String getModule() {
		return PADRModule.ADS.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetAddrDetailByAddr pb, final CompleteHandler handler) {
		ResGetAddrDetailByAddr.Builder ret = ResGetAddrDetailByAddr.newBuilder();
		if(pb != null && StringUtils.isNotBlank(pb.getAddress())){
			AddressInfo addrInfo = addressHelper.getAccountByAddress(pb.getAddress());
			if(addrInfo != null)
				ret.setAddressInfo(addrInfo);
			ret.setRplCode(1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}

