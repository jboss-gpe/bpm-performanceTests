package com.redhat.gpe.wih;

import javax.naming.InitialContext;
import javax.naming.Context;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.redhat.gpe.persistenceService.IPersistenceService;
import com.redhat.gpe.domain.Customer;

public class PersistenceServiceWIH implements WorkItemHandler {

    private static final String CUSTOMER_IN="customer";
    private static final String CUSTOMER_OUT="customer";
    private static Logger log = LoggerFactory.getLogger("PersistenceServiceWIH");

    // JPA based service
    private static IPersistenceService jService;

    private static Object lockObj = new Object();


    public PersistenceServiceWIH() {
        // make sure whatever is included in this constructor is performant!!
        // this constructor is instantiated with every ksession re-load
        if(jService == null) {
            try {
                synchronized(lockObj) {
                    if(jService != null)
                        return;
                    Context jContext = new InitialContext();
                    jService = (IPersistenceService)jContext.lookup("java:global/persistenceService/jpaService!com.redhat.gpe.persistenceService.IPersistenceService");
                    log.info("PersistenceServiceWIH() JPA Service = "+jService);
                }
            }catch(Exception x) {
                throw new RuntimeException(x);
            }
        }
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        Customer cObj = (Customer)workItem.getParameter(CUSTOMER_IN);
        if(cObj == null)
            throw new RuntimeException("executeWorkItem() must pass a task variable with name of:   "+CUSTOMER_IN);
        long customerId  = jService.persist(cObj);
        cObj.setId(customerId);
        log.info("executeWorkItem() will now execute completeWorkItem ...");
        manager.completeWorkItem(workItem.getId(), null);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
