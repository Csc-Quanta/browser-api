package org.csc.browserAPI.Helper;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.backend.ordbgens.bc.entity.*;
import org.csc.bcapi.EncAPI;
import org.csc.browserAPI.common.Constant;
import org.csc.browserAPI.dao.Daos;
import org.csc.browserAPI.gens.Block.BlockHeader;
import org.csc.browserAPI.gens.Block.BlockInfo;
import org.csc.browserAPI.gens.Block.BlockMiner;
import org.csc.browserAPI.gens.Tx.Transaction;
import org.csc.browserAPI.gens.Tx.TxInput;
import org.csc.browserAPI.gens.Tx.TxOutput;
import org.csc.browserAPI.util.ByteUtil;
import org.csc.browserAPI.util.NumUtil;

import java.math.BigDecimal;
import java.util.*;

/**
 * block information helper
 * 
 * @author murphy
 * 
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "browserapi.blockHelper")
@Slf4j
@Data
public class BlockHelper implements ActorService {

	@ActorRequire(name = "browserapi.daos")
	Daos daos;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	private final static boolean STABLE_BLOCK = false;
	/**
	 * 获取最新区块信息
	 * 
	 * @return ZCBcBlock 区块对象
	 */
	public ZCBcBlock getBestBlock(){
		ZCBcBlockExample blackExp = new ZCBcBlockExample();
		blackExp.setLimit(1);
		blackExp.setOrderByClause("BH_NUMBER DESC");
		List<Object> blackList = daos.bcBlockDao.selectByExample(blackExp);
		if(blackList==null || blackList.isEmpty()){
			return null;
		}
		return (ZCBcBlock) blackList.get(0);
	}
	
	/**
	 * 根据区块高度查询区块信息
	 * 
	 * @param blockNumber 区块高度
	 * @return ZCBcBlock 区块对象
	 */
	public ZCBcBlock getBlockByBlockNumber(long blockNumber){
		ZCBcBlockExample blackExp = new ZCBcBlockExample();
		blackExp.createCriteria().andBhNumberEqualTo(new BigDecimal(blockNumber));
		List<Object> blackList = daos.bcBlockDao.selectByExample(blackExp);
		if(blackList==null || blackList.isEmpty()){
			return null;
		}
		return (ZCBcBlock) blackList.get(0);
	}
	
	/**
	 * 根据区块hash查询区块信息
	 * 
	 * @param blockHash 区块哈希
	 * @return ZCBcBlock 区块对象
	 */
	public ZCBcBlock getBlockByBlockHash(String blockHash){
		ZCBcBlockExample blackExp = new ZCBcBlockExample();
		blackExp.createCriteria().andBhBlockHashEqualTo(blockHash);
		List<Object> blackList = daos.bcBlockDao.selectByExample(blackExp);
		if(blackList==null || blackList.isEmpty()){
			return null;
		}
		return (ZCBcBlock) blackList.get(0);
	}
	
	/**
	 * 根据区块高度查询区块中的交易信息
	 * 
	 * @param blockNumber 区块高度
	 * @return List<Object> 交易集合
	 */
	public Map<String,Object> getTxByBlockNumber(int blockNumber,int pageSize,int pageNo){
		Map<String,Object> resultMap = new HashMap<>();
		int scol = pageSize *(pageNo -1);
		ZCBcMutilTransactionExample transactionExp = new ZCBcMutilTransactionExample();
		transactionExp.createCriteria().andBlockHeightEqualTo(new BigDecimal(blockNumber))
			.andTxTypeNotEqualTo(Constant.COIN_BASE_TRANS_TYPE);//挖矿
		transactionExp.setOrderByClause("INDEX_IN_BLOCK ASC");
		transactionExp.setLimit(pageSize);
		transactionExp.setOffset(scol);
		List<Object> blackList = daos.bcMultiTransactionDao.selectByExample(transactionExp);
		List<ZCBcMutilTransaction> list = JSON.parseArray(JSON.toJSONString(blackList),ZCBcMutilTransaction.class);
		resultMap.put("list",list);
		transactionExp = new ZCBcMutilTransactionExample();
		transactionExp.createCriteria().andBlockHeightEqualTo(new BigDecimal(blockNumber))
				.andTxTypeNotEqualTo(Constant.COIN_BASE_TRANS_TYPE);//挖矿
		resultMap.put("count",daos.bcMultiTransactionDao.countByExample(transactionExp));
		return resultMap;
	}
	
	
	/**
	 * 获取最新的 block
	 * 
	 * @return
	 */
	public BlockInfo getTheBestBlock() {
		
		BlockInfo.Builder block = BlockInfo.newBuilder();
		ZCBcBlock blockEntity = null;
		BlockHeader.Builder header = BlockHeader.newBuilder();
		try {
			blockEntity = getBestBlock();
			if (blockEntity != null) {
				header = serializeBlockHeader(blockEntity, null);
				block.setHeader(header);
			}
		} catch (Exception e) {
			log.error("get the best block error" + e.getMessage());
		}
		return block.build();
	}

	/**
	 * 获取创世块信息
	 * @return
	 */
	public BlockInfo getGenisBlock() {
		BlockInfo.Builder block = BlockInfo.newBuilder();
		ZCBcBlock blockEntity = null;
		BlockHeader.Builder header;
		try {
			blockEntity = getBlockByBlockNumber(0);
			if (blockEntity != null) {
				header = serializeBlockHeader(blockEntity, null);
				block.setHeader(header);
			}
		} catch (Exception e) {
			log.error("get the genis block error" + e.getMessage());
		}
		return block.build();
	}
	/**
	 * 获取区块总个数
	 * 
	 * @return 总区块数
	 */
	public long getBlockCount(){
		ZCBcBlockExample exp = new ZCBcBlockExample();
		exp.createCriteria().andBhNumberNotEqualTo(BigDecimal.ZERO);
		long count = 0;
		try{
			count = daos.bcBlockDao.countByExample(exp);
		}catch(Exception e){
			log.error("get all blocks error" + e.getMessage());
		}
		
		return count;
	}
	/**
	 * 分页查询 blocks
	 * 
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	public List<Object> getBatchBlocks(int pageNo, int pageSize) {
		if (pageNo <= 0) {
			pageNo = 1;
		}
		if (pageSize <= 0) {
			pageSize = 10;
		}
		List<Object> list =null;
		try {
			int offset = pageNo == 0? 0 : (pageNo-1) * pageSize;
			ZCBcBlockExample exp = new ZCBcBlockExample();
			exp.createCriteria().andBhNumberNotEqualTo(BigDecimal.ZERO);
			exp.setOffset(offset);
			exp.setLimit(pageSize);
			exp.setOrderByClause("BH_NUMBER DESC");
			list = daos.bcBlockDao.selectByExample(exp);
			
		} catch (Exception e) {
			log.error("get batch blocks error" + e.getMessage());
		}

		return list;
	}

	/**
	 * 通过 txHash 获取 block 信息
	 * 
	 * @param txHash
	 * @return
	 */
	public BlockInfo getBlockByTxHash(String txHash) {
		BlockInfo.Builder blockInfo = BlockInfo.newBuilder();
		try {
			ZCBcMutilTransactionExample txExp = new ZCBcMutilTransactionExample();
			txExp.createCriteria().andTxHashEqualTo(txHash);
			List<Object> txList = daos.bcMultiTransactionDao.selectByExample(txExp);
			ZCBcMutilTransaction transaction = null;
			if(txList !=null && !txList.isEmpty()){
				transaction = (ZCBcMutilTransaction) txList.get(0);
			}
			
			if(transaction == null){
				return null;
			}
			
			ZCBcBlockExample blockExp = new ZCBcBlockExample();
			blockExp.createCriteria().andBhNumberEqualTo(transaction.getBlockHeight());
			List<Object> blockList = daos.bcBlockDao.selectByExample(blockExp);
			ZCBcBlock block = null;
			if(blockList != null && !blockList.isEmpty()){
				block = (ZCBcBlock) blockList.get(0);
			}
			if (block != null){
				BlockHeader.Builder header = serializeBlockHeader(block, null);
				blockInfo.setHeader(header);
			}
		} catch (Exception e) {
			log.error("get block by txHash error " + e.getMessage());
		}
		return blockInfo.build();
	}

	/**
	 * ZCBcBlock 对象 转 browserAPI 中的 block
	 * 
	 * @param blockEntity 区块信息
	 * @return
	 */
	public Map<String,Object> oBlock2BlockInfo(ZCBcBlock blockEntity, int pageSize, int pageNo) {
		if(blockEntity!=null) {
			List<Transaction> txList = new ArrayList<>();
			Map<String,Object> txMap = getTxByBlockNumber(blockEntity.getBhNumber().intValue(), pageSize, pageNo);
			List<ZCBcMutilTransaction> transactions = (List<ZCBcMutilTransaction>) txMap.get("list");
			if (transactions != null && !transactions.isEmpty()) {
				for (ZCBcMutilTransaction zcBcMutilTransaction : transactions) {
					Transaction tx = ZCBcMutilTransaction2Transaction(zcBcMutilTransaction);
					if(tx == null){
						continue;
					}
					tx = tx.toBuilder().setBlockNumber(blockEntity.getBhNumber().longValue()).build();
					txList.add(tx);
				}
			}
			txMap.put("list",txList);
			return txMap;
		}
		return null;
	}

	/**
	 * ZCBcBlock 转 BlockMiner
	 * 
	 * @return
	 */
	public BlockMiner.Builder oBlockMiner2Miner(ZCBcBlock blockEntity) {
		BlockMiner.Builder miner = BlockMiner.newBuilder();
		miner.setNode(StringUtils.isNotBlank(blockEntity.getBmNode()) ? blockEntity.getBmNode() : "");
		miner.setAddress(StringUtils.isNotBlank(blockEntity.getBmAddress()) ? blockEntity.getBmAddress() : "");
		miner.setReward(NumUtil.dealZero(ByteUtil.bytesToBigInteger(encApi.hexDec(blockEntity.getBmRewardHex())).toString(),8));
		miner.setBcuid(StringUtils.isNotBlank(blockEntity.getBmBcuid()) ? blockEntity.getBmBcuid() : "");

		return miner;
	}

	/**
	 * ZCBcBlock 转 BlockHeader
	 * 
	 * @return
	 */
	public BlockHeader.Builder serializeBlockHeader(ZCBcBlock blockEntity,List<Object> transactions) {
		BlockHeader.Builder header = null;

		// header
		if (blockEntity != null) {
			header = BlockHeader.newBuilder();
			header.setBlockHash(blockEntity.getBhBlockHash() != null ? blockEntity.getBhBlockHash() : "");
			header.setParentHash(blockEntity.getBhParentHash() != null ? blockEntity.getBhParentHash() : "");
			header.setTxTrieRoot(blockEntity.getBhTxtRieroot() != null ? blockEntity.getBhTxtRieroot() : "");
			header.setTimestamp(blockEntity.getBhTimestamp().longValue());
			header.setBlockNumber(blockEntity.getBhNumber().longValue());
			// header.setNonce(oBlockHeader.getNonce() != null ?
			// encApi.hexEnc(oBlockHeader.getNonce().toByteArray()) : "");
			//header.setSliceId(blockEntity.getBhSliceid());
			header.setMiner(oBlockMiner2Miner(blockEntity));
			header.setTxCount(blockEntity.getBhTxnCount());
			if(transactions != null) {
				for (Object o : transactions) {
					ZCBcMutilTransaction tx = (ZCBcMutilTransaction) o;
					header.addTxHashs(tx.getTxHash());
				}
			}
		}
		return header;
	}

	
	/**
	 * 根据交易hash获取交易input
	 * @param txHash 交易哈希
	 * @return List<Object> inputs
	 */
	public List<Object> getTransactionInputsByTxHash(String txHash){
		ZCBcMtxInputExample inpExp = new ZCBcMtxInputExample();
		inpExp.createCriteria().andMtxHashEqualTo(txHash);
		List<Object> list = null;
		try{
			list = daos.bcMtxInputDao.selectByExample(inpExp);
		}catch(Exception e){
			log.error("get transaction inputs error " + e.getMessage());
		}
		return list;
	}
	
	/**
	 * 根据交易hash获取交易output
	 * @param txHash 交易哈希
	 * @return List<Object> outputs
	 */
	public List<Object> getTransactionOutputsByTxHash(String txHash){
		ZCBcMtxOutputExample oupExp = new ZCBcMtxOutputExample();
		oupExp.createCriteria().andMtxHashEqualTo(txHash);
		List<Object> list = null;
		try{
			list = daos.bcMtxOutputDao.selectByExample(oupExp);
		}catch(Exception e){
			log.error("get transaction inputs error " + e.getMessage());
		}
		return list;
	}

	/**
	 * ZCBcMutilTransaction 转 Transaction
	 * 
	 * @return
	 */
	public Transaction ZCBcMutilTransaction2Transaction(ZCBcMutilTransaction transaction) {
		Transaction.Builder tx = Transaction.newBuilder();
//		Transaction tx = null;
		try {
			if(transaction!=null){
				// 获取区块的高度
				long blockHeight = transaction.getBlockHeight().longValue();

				// 交易时间
				long timeStamp = transaction.getTxTimestamp().longValue();
				
				// 交易状态
				String txStatus = transaction.getTxStatus();
				
				String txHash = transaction.getTxHash();
				tx.setTxHash(txHash);
				tx.setBlockNumber(blockHeight);
				tx.setTimeStamp(timeStamp);
				tx.setStatus(StringUtils.isNotBlank(txStatus)?txStatus:"");
				// data
				ByteString data = ByteString.copyFromUtf8(transaction.getData());
				// 委托代理
//				List<ByteString> delegates = mtBody.getDelegateList();
//				List<String> delegateStrs = null;
//				if (delegates != null && !delegates.isEmpty()) {
//					delegateStrs = new LinkedList<String>();
//					for (ByteString byteStr : delegates) {
//						delegateStrs.add(encApi.hexEnc(byteStr.toByteArray()));
//					}
//				}

				// 输入
				List<Object> mtxInput = getTransactionInputsByTxHash(transaction.getTxHash());
				List<TxInput.Builder> froms = null;
				if (mtxInput != null && !mtxInput.isEmpty()) {
					froms = new LinkedList<TxInput.Builder>();
					for (Object o : mtxInput) {
						ZCBcMtxInput mtxI = (ZCBcMtxInput) o;
						TxInput.Builder input = TxInput.newBuilder();
						input.setAddress(mtxI.getBcAddress());
						input.setAmount(NumUtil.dealZero(mtxI.getAmount().toPlainString(),8));
//						input.setCryptoToken(mtxI.getCryptoTokenHex());
						input.setFee(0);
						input.setNonce(mtxI.getBcNonce());
//						input.setSymbol(StringUtils.isNotBlank(mtxI.get) ? mtxI.getSymbol() : "");
//						input.setToken(StringUtils.isNotBlank(mtxI.getToken()) ? mtxI.getToken() : "");
						tx.addFroms(input);
					}
				}
				// 输出
				List<Object> mtxOutput = getTransactionOutputsByTxHash(transaction.getTxHash());
				List<TxOutput.Builder> tos = null;
				if (mtxOutput != null && !mtxOutput.isEmpty()) {
					tos = new LinkedList<TxOutput.Builder>();
					for (Object o : mtxOutput) {
						ZCBcMtxOutput mtxO = (ZCBcMtxOutput) o;
						TxOutput.Builder output = TxOutput.newBuilder();
						output.setAddress(mtxO.getBcAddress());
						output.setAmount(NumUtil.dealZero(mtxO.getAmount().toPlainString(),8));
//						output.setCryptoToken(mtxO.getCryptoTokenHex());
//						output.setSymbol(StringUtils.isNotBlank(mtxO.getSymbol()) ? mtxO.getSymbol() : "");
						tx.addTos(output);
					}
				}

				return tx.build();
			}
		} catch (Exception e) {
			log.error("get transaction error " + e.getMessage());
		}
		return null;
	}
	
	/**
	 * 通过 txhash 获取交易详情
	 * 
	 * @param txHash
	 * @return
	 */
	public ZCBcMutilTransaction getTransactionByTxHash(String txHash) {
		ZCBcMutilTransactionExample txExp = new ZCBcMutilTransactionExample();
		txExp.createCriteria().andTxHashEqualTo(txHash);
		txExp.setOrderByClause("INDEX_IN_BLOCK ASC");
		List<Object> txList = null;
		ZCBcMutilTransaction transaction = null;
		try {
			txList = daos.bcMultiTransactionDao.selectByExample(txExp);
			if(txList!=null && !txList.isEmpty()){
				transaction = (ZCBcMutilTransaction) txList.get(0);
			}
		} catch (Exception e) {
			log.error("get transaction error " + e.getMessage());
		}
		return transaction;
	}

}
