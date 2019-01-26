package org.csc.browserAPI.additional;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.csc.browserAPI.Helper.QueryReportHelper;
import org.csc.browserAPI.Helper.ReportUtil;
import org.csc.browserAPI.gens.Additional;

import java.util.List;

@NActorProvider
@Slf4j
@Data
public class GetReportInfo extends SessionModules<Additional.ReqGetReportInfo> {
    @ActorRequire(name = "reportUtil", scope = "global")
    ReportUtil reportUtil;

    @Override
    public String[] getCmds() {
        return new String[] { Additional.PADICommand.REP.name() };
    }

    @Override
    public String getModule() {
        return Additional.PADIModule.PAM.name();
    }
    @Override
    public void onPBPacket(final FramePacket pack, final Additional.ReqGetReportInfo pb, final CompleteHandler handler) {
        log.info("查询报表信息 入参：{}", pb.toString());
        Additional.ResGetReportInfo.Builder ret = Additional.ResGetReportInfo.newBuilder();
        QueryReportHelper queryReportHelper = reportUtil.getReportHelperMap().get(pb.getType());
        if(queryReportHelper == null){
            log.warn("不存在的报表类型：{}",pb.getType());
            ret.setRplCode(-1);
            handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
            return;
        }
        ret.setRplCode(1);
        List<Additional.ReportResult> list = queryReportHelper.queryReport(pb);
        ret.addAllReportResult(list);
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
    }
}
