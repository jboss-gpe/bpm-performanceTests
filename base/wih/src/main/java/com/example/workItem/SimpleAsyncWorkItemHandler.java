package com.example.workItem;

import java.util.HashMap;
import java.util.Map;

import org.drools.process.instance.WorkItemHandler;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;

import org.apache.log4j.Logger;
import org.jboss.processFlow.tasks.handlers.BasePFPTaskHandler;

public class SimpleAsyncWorkItemHandler implements WorkItemHandler {

    private static Logger log = Logger.getLogger("SimpleAsyncWorkItemHandler");
    private int ksessionId = 0;

    public SimpleAsyncWorkItemHandler(StatefulKnowledgeSession sessionObj) {
        ksessionId = sessionObj.getId();
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        Map<String, Object> taskWICompleterMap = (Map<String,Object>)workItem.getParameter("taskWICompleterMap");
        Long pInstanceId = workItem.getProcessInstanceId();

        log.info("executeWorkItem() pInstanceId = "+pInstanceId+" : ksessionId = "+ksessionId);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
