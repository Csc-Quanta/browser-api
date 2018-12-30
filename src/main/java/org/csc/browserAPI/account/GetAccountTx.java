package org.csc.browserAPI.account;

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
import org.csc.browserAPI.gens.Address;
import org.csc.browserAPI.gens.Address.PADRCommand;
import org.csc.browserAPI.gens.Address.PADRModule;
import org.csc.browserAPI.gens.Address.ReqGetAddrDetailByAddr;
import org.csc.browserAPI.gens.Address.ResGetAddrDetailByAddr;

@NActorProvider
@Slf4j
@Data
public class GetAccountTx extends SessionModules<ReqGetAddrDetailByAddr> {
	@ActorRequire(name = "addressHelper", scope = "global")
	AddressHelper addressHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PADRCommand.GTX.name() };
	}

	@Override
	public String getModule() {
		return PADRModule.ADS.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetAddrDetailByAddr pb, final CompleteHandler handler) {
		int pageSize = NumberUtils.INTEGER_ZERO.equals(pb.getPageSize())?10:pb.getPageSize();
		int pageNo = NumberUtils.INTEGER_ZERO.equals(pb.getPageNo())?1:pb.getPageNo();
		Address.ResGetAddrDetailByAddr.Builder addrInfo = ResGetAddrDetailByAddr.newBuilder();
		if (pb != null && StringUtils.isNotBlank(pb.getAddress())) {
			addrInfo = addressHelper
					.getTxByAddress(pb.getAddress(),pageSize,pageNo);
			if(addrInfo == null){
				addrInfo = ResGetAddrDetailByAddr.newBuilder();
			}
		}
		addrInfo.setRet(1);
		handler.onFinished(PacketHelper.toPBReturn(pack, addrInfo.build()));
	}
}
