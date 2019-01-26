package org.csc.browserAPI.Helper;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.JsonSerializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.csc.browserAPI.util.ByteUtil;
import org.csc.evmapi.gens.Act;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "accountHelper")
@Slf4j
@Data
public class AccountHelper implements ActorService {
    @ActorRequire(name = "http", scope = "global")
    IPacketSender sender;

    private static String QUERY_NODE;// 30802
    private static PropHelper props = new PropHelper(null);
    static {
        QUERY_NODE = props.get("query_node_url", "http://localhost:8000");
        log.debug("init query tx and node url successful url is:{}",QUERY_NODE);
    }
    /**
     * 查询账户信息
     * @param address
     */
    public Act.AccountValue.Builder getAccount(String address){
        Act.AccountValue.Builder builder =Act.AccountValue.newBuilder();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> reqParam = new HashMap<String, Object>();
        reqParam.put("address", address);
        String[] nodeUrls = QUERY_NODE.split(",");
        for(String nodeUrl : nodeUrls) {
            FramePacket fp = PacketHelper.buildUrlFromJson(JsonSerializer.formatToString(reqParam), "POST", nodeUrl+"/cks/api/pbgac.do");
            try {
                val nodeRet = sender.send(fp, 3000);
                if (nodeRet.getBody() != null && nodeRet.getBody().length > 0) {
                    JsonNode jsonObject = mapper.readTree(nodeRet.getBody());
                    if (jsonObject != null) {
                        // 相应的要添加节点的类型（raft、dpos）和节点的状态（pending、direct）
                        if (jsonObject.has("account")) {
                            Map<String, Object> map = JSON.parseObject(jsonObject.get("account").toString(), Map.class);
                            builder.setNonce(Integer.parseInt(map.get("nonce").toString()));
                            String balance = map.get("balance")!=null?map.get("balance").toString():"";
                            builder.setBalance(ByteString.copyFrom(ByteUtil
                                    .bigIntegerToBytes(new BigInteger(StringUtils.isBlank(balance)?"0":balance))));
                            String max = map.get("max")!=null?map.get("max").toString():"";
                            builder.setMax(ByteString.copyFrom(ByteUtil
                                    .bigIntegerToBytes(new BigInteger(StringUtils.isBlank(max)?"0":max))));
                            String acceptMax = map.get("acceptMax")!=null?map.get("acceptMax").toString():"";
                            String acceptLimit = map.get("acceptLimit")!=null?map.get("acceptLimit").toString():"";
                            builder.setAcceptMax(ByteString.copyFrom(ByteUtil
                                    .bigIntegerToBytes(new BigInteger(StringUtils.isBlank(acceptMax)?"0":acceptMax))));
                            builder.setAcceptLimit(Integer.parseInt(acceptLimit));
                            List<Act.AccountTokenValue> tokenValues = new ArrayList<>();
                            List<Map> maps = JSON.parseArray(map.get("tokens")!=null?map.get("tokens").toString():"",Map.class);
                            if(maps!=null) {
                                for (Map tokenMap : maps) {
                                    Act.AccountTokenValue.Builder tokenValue = Act.AccountTokenValue.newBuilder();
                                    tokenValue.setToken(tokenMap.get("token") != null ? tokenMap.get("token").toString() : "");
                                    String tokenBalace = tokenMap.get("balance") != null ? tokenMap.get("balance").toString() : "";
                                    tokenValue.setBalance(ByteString.copyFrom(ByteUtil
                                            .bigIntegerToBytes(new BigInteger(StringUtils.isBlank(tokenBalace) ? "0" : tokenBalace))));
                                    String locked = tokenMap.get("locked") != null ? tokenMap.get("locked").toString() : "";
                                    tokenValue.setLocked(ByteString.copyFrom(ByteUtil
                                            .bigIntegerToBytes(new BigInteger(StringUtils.isBlank(locked) ? "0" : locked))));
                                    tokenValues.add(tokenValue.build());
                                }
                            }
                            builder.addAllTokens(tokenValues);
                            return builder;
                        }
                    }
                } else {
                    log.warn("this url :{} query address is empty",nodeUrl);
                }
            } catch (Exception e) {
                log.error("url:{} get node info error : {}",nodeUrl,e);
            }
        }
        return null;
    }
}
