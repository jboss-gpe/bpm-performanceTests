package com.example.workItem;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.drools.process.instance.WorkItemHandler;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;

import org.apache.log4j.Logger;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;

/*
  in general, multi instance (as every other construct) is synchronous and to make it async it's to make the handler that takes care of particular work item async 
  Completion of the tasks can be done in any order and asynchronously.
 */
public class StartPInstanceAsyncAndWait extends BaseWorkItemHandler implements WorkItemHandler {

    private static Logger log = Logger.getLogger("PlaceInWaitState");
    private static Object lockObj = new Object();
    private static IKnowledgeSession kProxy;

    public StartPInstanceAsyncAndWait() {
    	if(kProxy == null){
            synchronized(lockObj){
                if(kProxy != null)
                    return;

                Context jndiContext = null;
                try {
                    jndiContext = new InitialContext();
                    kProxy = (IKnowledgeSession)jndiContext.lookup((IKnowledgeSession.KNOWLEDGE_SESSION_SERVICE_JNDI));
                } catch(Exception x) {
                        throw new RuntimeException("static()", x);
                }finally {
                    try {
                        if(jndiContext != null)
                            jndiContext.close();
                    }catch(Exception x){
                        x.printStackTrace();
                    }
                }
            }
        }
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        int batchSize = (Integer)workItem.getParameter(BATCH_SIZE);
        String jobId = (String)workItem.getParameter(JOB_ID);
        String processId = (String)workItem.getParameter(PROCESS_ID);
        Map<String, Object> pHash = new HashMap<String, Object>();
        pHash.put(IKnowledgeSession.DELIVER_ASYNC, "TRUE");
        pHash.put(IKnowledgeSession.WORK_ITEM_ID, workItem.getId());
        pHash.put(IKnowledgeSession.PROCESS_INSTANCE_ID, workItem.getProcessInstanceId());
        pHash.put(JOB_ID, jobId);
        kProxy.startProcessAndReturnId(processId, pHash);
        
        log.info("executeWorkItem() jobId = "+jobId+" : batchSize = "+batchSize);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
