package com.example.workItem;

import org.drools.process.instance.WorkItemHandler;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;

import org.apache.log4j.Logger;

public class HelloWorkItemHandler implements WorkItemHandler {

    Logger log = Logger.getLogger("HelloWorkItemHandler");

    public HelloWorkItemHandler() {
        // make sure whatever is included in this constructor is performant!!
        // this constructor is instantiated with every ksession re-load
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.info("executeWorkItem() will now execute completeWorkItem ...");
        manager.completeWorkItem(workItem.getId(), null);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
