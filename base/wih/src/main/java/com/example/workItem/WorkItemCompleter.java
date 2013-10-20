package com.example.workItem;

import java.util.Map;

import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkItemCompleter extends BaseWIHandler implements WorkItemHandler {

    private static Logger log = LoggerFactory.getLogger("WorkItemCompleter");
    private static Object lockObj = new Object();

    public WorkItemCompleter() {
        super();
    }

    // will standardize on use of String as workItem parameter values so that can be invoked via REST based gwt-console-server
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        Map<String, Object> wiParams = workItem.getParameters();
        Map<String, Object> taskWICompleterMap = (Map<String,Object>)workItem.getParameter("taskWICompleterMap");

        if(enableLog) {
            for(Map.Entry entry : wiParams.entrySet()) {
                log.info("executeWorkItem() wiParams key= "+entry.getKey()+"\n"+entry.getValue()+"\n");
            }
        }

        Long waitStateWorkItemId = Long.parseLong((String)taskWICompleterMap.get(IKnowledgeSession.WORK_ITEM_ID));
        Long pInstanceId = workItem.getProcessInstanceId();

        log.info("executeWorkItem() now completing workItem for Id = "+waitStateWorkItemId+" : pInstanceId = "+pInstanceId);
        String jsonPayload = (String)taskWICompleterMap.get("jsonPayload");
        if(jsonPayload != null) {
            log.info("executeWorkItem() jsonPayload = "+jsonPayload);
        }
        kProxy.completeWorkItem(waitStateWorkItemId, taskWICompleterMap, pInstanceId, null);
    
        log.info("executeWorkItem() will now execute completeWorkItem on signal branch...");
        manager.completeWorkItem(workItem.getId(), null);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
