package org.csc.browserAPI.Helper;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.backend.ordbgens.bc.entity.*;
import org.csc.bcapi.EncAPI;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Tx;
import org.csc.browserAPI.gens.Tx.Transaction;
import org.csc.browserAPI.util.NumUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jack
 * 
 *         block 相关信息获取
 * 
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "browser_blockHelper")
@Slf4j
@Data
public class BrowserBlockHelper implements ActorService {

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@ActorRequire(name = "browserapi.daos")
	Daos daos;

	private final static boolean STABLE_BLOCK = false;

	/**
	 * 根据交易hash从数据库中获取信息
	 * @param txHash 交易的hash
	 * @return 交易信息
	 */
	public Transaction getTxByTxHashFromDB(String txHash){
		ZCBcMutilTransactionExample example = new ZCBcMutilTransactionExample();
		example.createCriteria().andTxHashEqualTo(txHash);
		ZCBcMutilTransaction bcMutilTransacton = (ZCBcMutilTransaction) daos.getBcMultiTransactionDao()
				.selectOneByExample(example);
		if(bcMutilTransacton == null){
			log.info("getTxByTxHash ：{}, bcMutilTransacton ==null",txHash);
			return null;
		}
		ZCBcMtxInputExample bcMtxInputExample = new ZCBcMtxInputExample();
		bcMtxInputExample.createCriteria().andMtxHashEqualTo(txHash);
		List<Object> objs = daos.getBcMtxInputDao().selectByExample(bcMtxInputExample);
		List<ZCBcMtxInput> bcMtxInputs = JSON.parseArray(JSON.toJSONString(objs),ZCBcMtxInput.class);
		ZCBcMtxOutputExample bcMtxOutputExample = new ZCBcMtxOutputExample();
		bcMtxOutputExample.createCriteria().andMtxHashEqualTo(txHash);
		List<Object> outObjs = daos.getBcMtxOutputDao().selectByExample(bcMtxInputExample);
		List<ZCBcMtxOutput> bcMtxOutputs = JSON.parseArray(JSON.toJSONString(outObjs),ZCBcMtxOutput.class);
		List<Tx.TxInput> inputList = new ArrayList<>();
		List<Tx.TxOutput> outputs = new ArrayList<>();
		bcMtxInputs.forEach(input->{
			Tx.TxInput.Builder txBuilder = Tx.TxInput.newBuilder();
			txBuilder.setAmount(NumUtil.dealZero(input.getAmount().toString(),8));
			txBuilder.setAddress(input.getBcAddress());
			txBuilder.setNonce(input.getBcNonce());
			inputList.add(txBuilder.build());
		});
		bcMtxOutputs.forEach(outPut->{
			Tx.TxOutput.Builder outBuilder = Tx.TxOutput.newBuilder();
			outBuilder.setAmount(NumUtil.dealZero(outPut.getAmount().toString(),8));
			outBuilder.setAddress(outPut.getBcAddress());
			outputs.add(outBuilder.build());
		});
		Transaction.Builder transaction = Transaction.newBuilder();
		transaction.setBlockNumber(bcMutilTransacton.getBlockHeight().longValue());
		transaction.addAllFroms(inputList);
		transaction.addAllTos(outputs);
		transaction.setTxHash(txHash);
		transaction.setTimeStamp(bcMutilTransacton.getTxTimestamp().longValue());
		transaction.setStatus(bcMutilTransacton.getTxStatus());
		return transaction.build();
	}
	/************* 测试 ***************
	 * */

	public List<String> getBlocksHash() {
		List<String> list = new ArrayList<String>();

		return list;
	}
}
