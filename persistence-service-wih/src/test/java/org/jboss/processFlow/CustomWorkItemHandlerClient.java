package org.jboss.processFlow;

import java.math.BigDecimal;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import org.jbpm.task.Status;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.TaskException;
import org.jboss.processFlow.test.SyncClientTest;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;

public class CustomWorkItemHandlerClient implements Runnable {

    private final static String PROCESS_ID = "customWorkItemHandler";
    private static Logger log = Logger.getLogger("CustomWorkItemHandlerClient");

    private boolean claimTask = true;
    private boolean completeTask = true;
    private String nodeId;
    private int counter = 0;
    private long threadStart= 0L;
    private ITaskService taskServiceProxy = null;
    private IKnowledgeSession kSessionProxy = null;

    public CustomWorkItemHandlerClient(Integer id) throws Exception {
        this.nodeId = id.toString();
        taskServiceProxy = SyncClientTest.getTaskServiceProxy();
        kSessionProxy = SyncClientTest.getKnowledgeSessionProxy();

        if(!SyncClientTest.useAgent)
            SyncClientTest.addProcessToKnowledgeBase();

        Properties properties = new Properties();
        properties.load(CustomWorkItemHandlerClient.class.getResourceAsStream(SyncClientTest.PROPERTIES_FILE_NAME));
            if(properties.size() == 0)
                throw new RuntimeException("start() no properties defined in "+SyncClientTest.PROPERTIES_FILE_NAME);

        StringBuilder sBuilder = new StringBuilder();
        log.info(sBuilder.toString());
    }

    public void run() {
            Map<String, Object> parameters = null;
            if(SyncClientTest.includeProcessVariables) {
                parameters = new HashMap<String, Object>();
                parameters.put("pVar", new Integer(1500));
            }
            final AtomicInteger completedCount = new AtomicInteger(0);
            threadStart = System.currentTimeMillis();
            for(counter=1; counter <= SyncClientTest.requestsPerClient; counter++) {

                // 1)  start new process
                final long sendTime = System.currentTimeMillis();
                long processInstanceId = 0L;
                int ksessionId =0;
                if(SyncClientTest.startNewProcessInstance) {
                    Map<String, Object> returnMap = null;
                    try {
                        returnMap = kSessionProxy.startProcessAndReturnId(PROCESS_ID, parameters);
                        if(returnMap == null){
                            log.error("run() pInstance creation failed for processId = "+PROCESS_ID);
                            return;
                        }
                        processInstanceId = (Long)returnMap.get(IKnowledgeSession.PROCESS_INSTANCE_ID);
                        ksessionId = (Integer)returnMap.get(IKnowledgeSession.KSESSION_ID);
                        log.info("run() created pInstance with id = "+processInstanceId+" : ksessionId = "+ksessionId);

                    } catch(javax.ejb.EJBTransactionRolledbackException x) {
                        StringBuilder sBuilder = new StringBuilder();
                        sBuilder.append("run() nodeId = "+nodeId+" : javax.ejb.EJBTransactionRolledbackException thrown when attempting to start process. cause = \n\t");
                        sBuilder.append(x.getCause());
                        log.error(sBuilder.toString());
                        return;
                    } catch(Exception x) {
                        throw new RuntimeException(x);
                    }

                    if(SyncClientTest.abortProcessInstance){
                    log.warn("run() about to abort pInstance w/ id = "+processInstanceId);
                    kSessionProxy.abortProcessInstance(processInstanceId, ksessionId);
                    break;
                    }
                }

                SyncClientTest.serverNodeCountHash.putIfAbsent(nodeId, new AtomicInteger(0));
                SyncClientTest.serverNodeCountHash.get(nodeId).incrementAndGet();
                completedCount.incrementAndGet();
                try {
                    Thread.sleep(SyncClientTest.sleepTimeMillis);
                } catch(Exception x) {
                    x.printStackTrace();
                }
            }
            long duration = (System.currentTimeMillis() - threadStart);
            BigDecimal aveDuration = new BigDecimal(SyncClientTest.computeAverageDuration(duration)).divide(SyncClientTest.bdThousand);
            int tCount = SyncClientTest.computeTotal(completedCount.get());
            if(SyncClientTest.enableLog)
                log.info("THREAD_COMPLETE!\t"+nodeId+"\t"+tCount+"\t"+aveDuration);
    }

    public void printInboundTaskVariables(long taskId) {
    }
}