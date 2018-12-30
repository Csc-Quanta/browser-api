package org.csc.browserAPI.dao;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.IJPAClient;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.annotations.StoreDAO;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.csc.backend.ordbgens.bc.entity.*;

@iPojoBean
@Provides(specifications = { IJPAClient.class, ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Instantiate(name = "browserapi.daos")
@Component
@Data
public class Daos implements ActorService, IJPAClient{

    @StoreDAO
    public OJpaDAO<ZCBcAccount> bcAccountDao;

    @StoreDAO
    public OJpaDAO<ZCBcBlock> bcBlockDao;

    @StoreDAO
    public OJpaDAO<ZCBcMutilTransaction> bcMultiTransactionDao;

    @StoreDAO
    public OJpaDAO<ZCBcActAddress> bcActAddressDao;

    @StoreDAO
    public OJpaDAO<ZCBcMtxInput> bcMtxInputDao;

    @StoreDAO
    public OJpaDAO<ZCBcMtxOutput> bcMtxOutputDao;

    @StoreDAO
    public OJpaDAO<ZCBcMtxSignature> bcMtxSignDao;
    
    @StoreDAO
    public OJpaDAO<ZCBcNode> bcNodeDao;

    @StoreDAO
    public OJpaDAO<ZCBcActTokenValue> bcActTokenValueDao;
    @Override
    public void onDaoServiceReady(DomainDaoSupport domainDaoSupport) {
        log.debug("browserapi onDaoServiceReady:" + domainDaoSupport);
    }

    @Override
    public void onDaoServiceAllReady() {
        log.debug("browserapi allDao Ready....");
    }
}
