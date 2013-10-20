package com.redhat.gpe.jpaService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnit;
import javax.transaction.TransactionManager;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.redhat.gpe.persistenceService.IPersistenceService;
import com.redhat.gpe.domain.Customer;

@Local(IPersistenceService.class)
@Singleton(name="jpaService")
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class JPAService implements IPersistenceService {

    private static Logger log = LoggerFactory.getLogger("JPAService");

    private @PersistenceUnit(unitName="")EntityManagerFactory testEMF;
    private @Resource(name="java:/TransactionManager") TransactionManager tMgr;

    @PostConstruct
    public void start() {
        log.info("start() testEMF = "+testEMF);
    }

    public int persist() {
        try {
            log.info("persist() trnx status = "+tMgr.getStatus());
            EntityManager emObj = testEMF.createEntityManager();
            Customer cObj = new Customer("Azra and Alex", "Bride");
            emObj.persist(cObj);
            return 1;
        }catch(Throwable x) {
            x.printStackTrace();
            return 0;
        }
    }

    @PreDestroy
    public void stop() {
        log.info("stop()");
    }
}
