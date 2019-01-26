package org.csc.browserAPI.Helper;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.ordb.ORDBDataService;
import onight.tfw.outils.conf.PropHelper;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.backend.ordbgens.bc.entity.*;
import org.csc.bcapi.EncAPI;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Address;
import org.csc.browserAPI.gens.Address.AddressInfo;
import org.csc.browserAPI.gens.Tx;
import org.csc.browserAPI.gens.Tx.Transaction;
import org.csc.browserAPI.util.ByteUtil;
import org.csc.browserAPI.util.NumUtil;
import org.csc.evmapi.gens.Act;
import org.csc.evmapi.gens.Act.AccountTokenValue;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jack
 * <p>
 * address 相关信息获取
 */
@NActorProvider
@Provides(specifications = {ActorService.class}, strategy = "SINGLETON")
@Instantiate(name = "addressHelper")
@Slf4j
@Data
public class AddressHelper implements ActorService {

    @ActorRequire(name = "bc_encoder", scope = "global")
    EncAPI encApi;

    @ActorRequire(name = "browser_blockHelper", scope = "global")
    BrowserBlockHelper blockHelper;
    @ActorRequire(name = "browserapi.daos")
    Daos daos;
    private static PropHelper props = new PropHelper(null);
    @ActorRequire(name = "accountHelper")
    AccountHelper accountHelper;

    /**
     * 获得查询账户的sql
     *
     * @param paramInfo
     * @return String
     */
    private String getTxSql(ParamInfo paramInfo) {
        return "select * from (" +
                "select * from zc_bc_mutil_transaction allTx INNER JOIN ( " +
                "select input.BC_ADDRESS,input.MTX_HASH from zc_bc_mtx_input input INNER JOIN zc_bc_mtx_output output " +
                "ON input.MTX_HASH = output.MTX_HASH AND input.INDEX_IN_TX = output.INDEX_IN_TX " +
                "WHERE input.BC_ADDRESS = '" + paramInfo.getAddress() + "' and input.tx_status = '1') inputTx " +
                "on allTx.TX_HASH = inputTx.mtx_hash and allTx.tx_status = '1' " +
                "UNION " +
                "select * from zc_bc_mutil_transaction allTx INNER JOIN ( " +
                "select output.BC_ADDRESS,output.MTX_HASH from zc_bc_mtx_input input INNER JOIN zc_bc_mtx_output output " +
                "ON input.MTX_HASH = output.MTX_HASH AND input.INDEX_IN_TX = output.INDEX_IN_TX " +
                "WHERE output.BC_ADDRESS = '" + paramInfo.getAddress() + "' and output.tx_status = '1') outputTx " +
                "on allTx.TX_HASH = outputTx.mtx_hash and allTx.tx_status = '1') allInfo order by  " +
                "allInfo.create_time desc limit " + paramInfo.getStartCol() + "," + paramInfo.getPageSize();
    }

    /**
     * 获取from的信息
     *
     * @param txHash
     * @return
     */
    private String getChildSql(List<String> txHash, String tableName) {
        StringBuffer stringBuffer = new StringBuffer("select *from ").append(tableName).append(" input where input.MTX_HASH in(");
        for (String hash : txHash) {
            stringBuffer.append("'").append(hash).append("'").append(",");
        }
        stringBuffer = stringBuffer.delete(stringBuffer.length() - 1, stringBuffer.length());
        return stringBuffer.append(")").toString();
    }

    @Data
    class ParamInfo {
        //input的地址
        String address;
        //开始查询记录数
        int startCol;
        //需要查询的大小
        int pageSize;
    }

    /**
     * 通过账户地址查询账户交易信息
     *
     * @param address
     * @return
     */
    public Address.ResGetAddrDetailByAddr.Builder getTxByAddress(String address, int pageSize, int pageNo) {
        int scol = pageSize * (pageNo - 1);
        AddressInfo.Builder account = AddressInfo.newBuilder();
        ORDBDataService dataService = (ORDBDataService) daos.getBcMtxInputDao().getDaosupport();
        ParamInfo paramInfo = new ParamInfo();
        paramInfo.setAddress(address);
        paramInfo.setStartCol(scol);
        paramInfo.setPageSize(pageSize);
        ;
        String querySql = getTxSql(paramInfo);
        log.debug("getTxByAddress 查询的sql为：{}", querySql);
        List<Object> objectList = (List<Object>) dataService.doBySQL(querySql);
        List<ZCBcMutilTransaction> resultList = JSON.parseArray(JSON.toJSONString(objectList), ZCBcMutilTransaction.class);
        if (resultList == null || resultList.isEmpty()) {
            log.info("resultList == null || resultList.isEmpty");
            return null;
        }
        ZCBcMtxInputExample example = new ZCBcMtxInputExample();
        example.createCriteria().andBcAddressEqualTo(address)
                .andTxStatusEqualTo("1");
        example.setGroupByClause("mtx_hash");
        int totalCount = daos.getBcMtxInputDao().countByExample(example);
        ZCBcMtxOutputExample outputExample = new ZCBcMtxOutputExample();
        outputExample.createCriteria().andTxStatusEqualTo("1")
                .andBcAddressEqualTo(address);
        example.setGroupByClause("mtx_hash");
        //收钱方也要算进去
        totalCount += daos.getBcMtxOutputDao().countByExample(outputExample);
        List<String> hashList = new ArrayList<>();
        for (ZCBcMutilTransaction transacton : resultList) {
            hashList.add(transacton.getTxHash());
        }
        String inputSql = getChildSql(hashList, "zc_bc_mtx_input");
        String outputSql = getChildSql(hashList, "zc_bc_mtx_output");
        log.debug("getInputSql :{}", inputSql);
        log.debug("getOutputSql:{}", outputSql);
        List<Object> objInputs = (List<Object>) dataService.doBySQL(inputSql);
        List<ZCBcMtxInput> bcMtxInputs = JSON.parseArray(JSON.toJSONString(objInputs), ZCBcMtxInput.class);
        ORDBDataService ordbDataService = (ORDBDataService) daos.getBcMtxOutputDao().getDaosupport();
        List<Object> objOutputs = (List<Object>) ordbDataService.doBySQL(outputSql);
        List<ZCBcMtxOutput> bcMtxOutputs = JSON.parseArray(JSON.toJSONString(objOutputs), ZCBcMtxOutput.class);
        for (ZCBcMutilTransaction transacton : resultList) {
            Transaction.Builder builder = Transaction.newBuilder();
            List<Tx.TxInput> inputList = new ArrayList<>();
            List<Tx.TxOutput> outputList = new ArrayList<>();
            for (ZCBcMtxInput input : bcMtxInputs) {
                if (input.getMtxHash().equals(transacton.getTxHash())) {
                    Tx.TxInput.Builder txBuilder = Tx.TxInput.newBuilder();
                    txBuilder.setNonce(input.getBcNonce());
                    txBuilder.setAmount(NumUtil.dealZero(input.getAmount().toString(), 8));
                    txBuilder.setAddress(input.getBcAddress());
                    inputList.add(txBuilder.build());
                }
            }
            for (ZCBcMtxOutput bcMtxOutput : bcMtxOutputs) {
                if (bcMtxOutput.getMtxHash().equals(transacton.getTxHash())) {
                    Tx.TxOutput.Builder txBuilder = Tx.TxOutput.newBuilder();
                    txBuilder.setAmount(NumUtil.dealZero(bcMtxOutput.getAmount().toString(), 8));
                    txBuilder.setAddress(bcMtxOutput.getBcAddress());
                    outputList.add(txBuilder.build());
                }
            }
            builder.addAllFroms(inputList);
            builder.addAllTos(outputList);
            builder.setTxHash(transacton.getTxHash());
            builder.setTimeStamp(transacton.getTxTimestamp().longValue());
            builder.setBlockNumber(transacton.getBlockHeight().longValue());
            account.addTxs(builder.build());
        }
        Address.ResGetAddrDetailByAddr.Builder builder = Address.ResGetAddrDetailByAddr.newBuilder();
        builder.setAddressInfo(account);
        builder.setTotalCount(totalCount);
        return builder;
    }

    /**
     * @param tokenValues 账户erc20信息
     * @param address     账户的地址
     * @return
     */
    public ZCBcAccount getAccountByAddress(List<ZCBcActTokenValue> tokenValues, String address) {
        ZCBcAccount zcBcAccount = new ZCBcAccount();
        Act.AccountValue.Builder oAccountValue = accountHelper.getAccount(address);
        if (oAccountValue != null) {
            // nonce
            zcBcAccount.setActNonce(oAccountValue.getNonce());
            // balance
            zcBcAccount.setActBalance(new BigDecimal(ByteUtil.bytesToBigInteger(oAccountValue.getBalance().toByteArray())));
            // erc20的信息
            if (oAccountValue.getTokensList() != null && !oAccountValue.getTokensList().isEmpty()) {
                for (AccountTokenValue t : oAccountValue.getTokensList()) {
                    ZCBcActTokenValue zcBcActTokenValue = new ZCBcActTokenValue();
                    zcBcActTokenValue.setActvBalance(new BigDecimal(ByteUtil
                            .bytesToBigInteger(t.getBalance().toByteArray())));
                    zcBcActTokenValue.setActvToken(StringUtils.isNotBlank(t.getToken()) ? t.getToken() : "");
                    zcBcActTokenValue.setActvLocked(new BigDecimal(ByteUtil
                            .bytesToBigInteger(t.getLocked().toByteArray())));
                    zcBcActTokenValue.setActAddress(address);
                    zcBcActTokenValue.setActtStatus("1");
                    tokenValues.add(zcBcActTokenValue);
                }
            }
            //最少签名者数量
            zcBcAccount.setActAcceptLimit(BigDecimal.valueOf(oAccountValue.getAcceptLimit()));
            //每笔请求最大交易金额
            zcBcAccount.setActAcceptMax(new BigDecimal(ByteUtil.bytesToBigInteger(oAccountValue
                    .getAcceptMax().toByteArray())));
            //每日最大交易次数
            zcBcAccount.setActMax(new BigDecimal(ByteUtil.bytesToBigInteger(oAccountValue.getMax().toByteArray())));
            zcBcAccount.setStorageHex(Hex.encodeHexString(oAccountValue.getStorage().toByteArray()));
            byte[] dataArray = oAccountValue.getData().toByteArray();
            if (ByteUtil.isNullOrZeroArray(dataArray)) {
                zcBcAccount.setDataHex(Hex.encodeHexString(dataArray));
                zcBcAccount.setCodeHex(Hex.encodeHexString(oAccountValue.getCodeHash().toByteArray()));
                zcBcAccount.setCodeHashHex(Hex.encodeHexString(oAccountValue.getCodeHash().toByteArray()));
            }
        }
        return zcBcAccount;
    }

    /**
     * 从数据库中查询账户信息根据地址
     *
     * @param address 账户地址
     * @return 返回的地址的详细信息
     */
    public AddressInfo getAccountByAddress(String address) {
        AddressInfo.Builder account = AddressInfo.newBuilder();
        ZCBcAccountExample example = new ZCBcAccountExample();
        List<String> actStatusList = new ArrayList<>();
        Collections.addAll(actStatusList, "0", "1");
        example.createCriteria().andActAddressEqualTo(address)
                .andActStatusIn(actStatusList);
        ZCBcAccount bcAccount = (ZCBcAccount) daos.getBcAccountDao().selectOneByExample(example);
        //在数据库中查不到此地址
        if (bcAccount == null) {
            log.warn("无此账户的信息 地址：{}", address);
            return null;
        }
        String flushFlg = bcAccount.getFlushFlg();
        List<ZCBcActTokenValue> zcBcActTokenValues = new ArrayList<>();
        ZCBcAccount zcBcAccount = null;
        switch (flushFlg) {
            case "0": //需要进行刷新
                //需要查询主链
                zcBcAccount = getAccountByAddress(zcBcActTokenValues, address);
                break;
            case "1"://不需要进行刷新
                //判断时间上是否存在超时情况
                Date flushTime = bcAccount.getFlushTime();
                //mysql数据库账户信息的更新时间差，小时为单位
                int dValue = props.get("mysql.account.info.update.time", 24);
                Date nowDate = new Date();
                flushTime = DateUtils.addHours(flushTime, dValue);
                if (flushTime.compareTo(nowDate) <= 0) {
                    zcBcAccount = getAccountByAddress(zcBcActTokenValues, address);
                }
                break;
        }
        ZCBcActTokenValueExample bcActTokenValueExample = new ZCBcActTokenValueExample();
        bcActTokenValueExample.createCriteria().andActAddressEqualTo(address)
                .andActtStatusEqualTo("1");
        //查询erc20的信息
        List<Object> objects = daos.getBcActTokenValueDao().selectByExample(bcActTokenValueExample);
        List<ZCBcActTokenValue> actTokenValues = JSON.parseArray(JSON.toJSONString(objects), ZCBcActTokenValue.class);
        if (zcBcAccount == null) {
            zcBcAccount = bcAccount;
        } else {
            zcBcAccount.setFlushFlg("1");
            zcBcAccount.setFlushTime(new Date());
            zcBcAccount.setActAddress(address);
            zcBcAccount.setUpdateTime(new Date());
            daos.getBcAccountDao().updateByPrimaryKeySelective(zcBcAccount);
            actTokenValues.forEach(actTokenValue -> {
                zcBcActTokenValues.forEach(
                        zcBcActTokenValue -> {
                            if (actTokenValue.getActvToken().equals(zcBcActTokenValue.getActvToken())) {
                                zcBcActTokenValue.setActvTokenId(actTokenValue.getActvTokenId());
                            }
                        }
                );
            });
            //需要更新的list
            List<Object> updateList = zcBcActTokenValues.stream().map(tokenValue -> {
                if (StringUtils.isNotBlank(tokenValue.getActvTokenId())) {
                    return tokenValue;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            //需要插入的list
            List<Object> needInsert = zcBcActTokenValues.stream().map(tokenValue -> {
                if (StringUtils.isBlank(tokenValue.getActvTokenId())) {
                    return tokenValue;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            daos.getBcActTokenValueDao().batchUpdate(updateList);
            daos.getBcActTokenValueDao().batchInsert(needInsert);
            actTokenValues = zcBcActTokenValues;
        }
        //交易次数
        account.setNonce(zcBcAccount.getActNonce());
        // 余额
        BigDecimal actBalance = zcBcAccount.getActBalance();
        String balance = actBalance == null ? "" : actBalance.toString();
        account.setBalance(NumUtil.dealZero(StringUtils.isBlank(balance) ? "0" : balance, 8));
        //查询erc20token
        List<Address.Token> list = new ArrayList<>(zcBcActTokenValues.size());
        actTokenValues.forEach(tokenValue -> {
            Address.Token.Builder token = Address.Token.newBuilder();
            ZCBcActTokenValue zcBcActTokenValue = JSON.parseObject(JSON.toJSONString(tokenValue), ZCBcActTokenValue.class);
            token.setBalance(zcBcActTokenValue.getActvBalance().toString());
            token.setLocked(zcBcActTokenValue.getActvLocked().toString());
            token.setTokenName(zcBcActTokenValue.getActvToken());
            list.add(token.build());
        });
        account.addAllTokens(list);
        return account.build();
    }
}
