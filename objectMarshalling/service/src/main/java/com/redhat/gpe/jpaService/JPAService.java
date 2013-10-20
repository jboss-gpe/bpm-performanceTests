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
import javax.naming.Context;
import javax.naming.InitialContext;
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
    private static Object lockObj = new Object();

    private EntityManagerFactory testEMF;
    private @Resource(name="java:/TransactionManager") TransactionManager tMgr;

    public void getEmf() throws Exception {
        if(testEMF == null) {
            synchronized(lockObj) {
                if(testEMF != null)
                    return;

                Context jndiContext = new InitialContext();
                // use same EntityManagerFactory that jbpm engine uses
                testEMF = (EntityManagerFactory)jndiContext.lookup("java:/app/knowledgeSessionEMF");

                log.info("start() testEMF = "+testEMF);
            }
        }
    }

    public long persist(Customer cObj) {
        long id = 0L;
        try {
            getEmf();
            EntityManager emObj = testEMF.createEntityManager();
            emObj.merge(cObj);
            emObj.flush();
            id = cObj.getId();
            log.info("persist() cObj id = "+id);
        }catch(Throwable x) {
            x.printStackTrace();
        }
        return id;
    }

    @PreDestroy
    public void stop() {
        log.info("stop()");
    }
}
