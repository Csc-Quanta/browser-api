package org.csc.browserAPI.task;

import java.util.Calendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Validate;
import org.csc.browserAPI.Helper.AdditionalHelper;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Additional.ReqGetNodes;
import org.csc.browserAPI.gens.Additional.ResGetNodes;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
@NActorProvider
@Slf4j
@Data
/**
 * @ClassName: NodeInfoTask 
 * @Description: 节点信息定时抓取任务 
 * @date 2018-12-22 14:30:45
 */
public class NodeInfoTask extends SessionModules<ReqGetNodes> {

	String name = "[节点信息定时抓取任务]";
	
	@ActorRequire(name = "browserapi.daos")
	Daos daos;
	
	@ActorRequire(name = "browser.additionalHelper", scope = "global")
	AdditionalHelper additionalHelper;
	
	int wait_sec = props().get("browser.nodetask.waitsec", 60*1000);
	PropHelper prop = new PropHelper(null);
		
	@Override
	public String[] getCmds() {
		return new String[] { "GNT" };
	}
	@Override
	public String getModule() {
		return "BSR";
	}
	@Override
	public void onPBPacket(FramePacket pack, ReqGetNodes pbo, CompleteHandler handler) {
		ResGetNodes.Builder ret = ResGetNodes.newBuilder();
		//dorun();
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	public boolean initParams(){
		while (daos == null){
			log.info("NodeInfoTask daos is null,start it again wait...");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	public void dorun() {
		if(initParams()){
			try{
				additionalHelper.initNodes();
			}catch(Exception e){
				log.warn("同步节点错误：{}",e);
			}
		}
	}
	//隔多长时间后运行
	private Long subdate(){
		Calendar c = Calendar.getInstance();
		long now = c.getTimeInMillis();
		c.add(Calendar.DAY_OF_MONTH, 0);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		System.out.println(c.toString());
		long millis = c.getTimeInMillis() - now;
		return millis;
		
	}
	ScheduledThreadPoolExecutor scheduler;
	@Override
	@Validate
	public void validate() {
		log.warn("{}启动",name);
		scheduler = new ScheduledThreadPoolExecutor(1);
		scheduler.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				NodeInfoTask.this.dorun();
			}
		}, subdate(), wait_sec, TimeUnit.MILLISECONDS);
		super.validate();
	}
	
}
