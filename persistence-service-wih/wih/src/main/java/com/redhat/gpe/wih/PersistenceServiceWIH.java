package com.redhat.gpe.wih;

import javax.naming.InitialContext;
import javax.naming.Context;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.redhat.gpe.persistenceService.IPersistenceService;

public class PersistenceServiceWIH implements WorkItemHandler {

    private static final String TEST_JPA="test.persist.with.JPA";
    private static Logger log = LoggerFactory.getLogger("PersistenceServiceWIH");

    // JPA based service
    private static IPersistenceService jService;

    // Spring service
    private static IPersistenceService sService;

    private static Object lockObj = new Object();

    public PersistenceServiceWIH() {
        // make sure whatever is included in this constructor is performant!!
        // this constructor is instantiated with every ksession re-load
        if(jService == null) {
            try {
                synchronized(lockObj) {
                    if(sService != null)
                        return;
                    Context jContext = new InitialContext();
                    jService = (IPersistenceService)jContext.lookup("java:global/persistenceService/jpaService!com.redhat.gpe.persistenceService.IPersistenceService");
                    sService = (IPersistenceService)jContext.lookup("java:global/persistenceService/springService!com.redhat.gpe.persistenceService.IPersistenceService");
                    log.info("PersistenceServiceWIH() JPA Service = "+jService);
                }
            }catch(Exception x) {
                throw new RuntimeException(x);
            }
        }
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        if(Boolean.parseBoolean(System.getProperty(TEST_JPA, "true"))) {
            jService.persist();
        }else{
            sService.persist();
        }
        log.info("executeWorkItem() will now execute completeWorkItem ...");
        manager.completeWorkItem(workItem.getId(), null);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
