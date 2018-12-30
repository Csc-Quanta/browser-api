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
import org.apache.commons.lang3.math.NumberUtils;
import org.csc.bcapi.EncAPI;
import org.csc.browserAPI.Helper.AddressHelper;
import org.csc.browserAPI.gens.Address;
import org.csc.browserAPI.gens.Tx.*;

@NActorProvider
@Slf4j
@Data
public class GetTxByAddress extends SessionModules<ReqGetTxByAddress>{
	@ActorRequire(name = "addressHelper", scope = "global")
	AddressHelper addressHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	@Override
	public String[] getCmds() {
		return new String[] { PTRSCommand.GTA.name() };
	}

	@Override
	public String getModule() {
		return PTRSModule.TXS.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTxByAddress pb, final CompleteHandler handler) {
		ResGetTxByAddress.Builder ret = ResGetTxByAddress.newBuilder();
		int pageSize = NumberUtils.INTEGER_ZERO.equals(pb.getPageSize())?10:pb.getPageSize();
		int pageNo = NumberUtils.INTEGER_ZERO.equals(pb.getPageNo())?1:pb.getPageNo();
		try{
			ret.setRet(1);
			
			if(pb != null && StringUtils.isNotBlank(pb.getAddress())){
				Address.ResGetAddrDetailByAddr.Builder builder = addressHelper.getTxByAddress(pb.getAddress(),pageSize,pageNo);
				for(Transaction transaction : builder.getAddressInfo().getTxsList()){
					ret.addTxs(transaction);
				}
			}
		} catch (Exception e){
			log.error("get tx error " + e.getMessage());
			ret.setRet(-1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
