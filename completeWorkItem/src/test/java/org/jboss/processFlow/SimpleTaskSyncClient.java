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

import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.Logger;

import org.jbpm.task.Status;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.TaskException;
import org.jboss.processFlow.test.SyncClientTest;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;

public class SimpleTaskSyncClient implements Runnable {

    private static Logger log = Logger.getLogger("SimpleTaskSyncClient");
    private static final String PROCESS_ID = "reusablesubprocesstest";
    //private static final String PROCESS_ID = "embeddedsubprocesstest";

    private String nodeId;
    private int counter = 0;
    private long threadStart= 0L;
    private IKnowledgeSession kSessionProxy = null;
    

    public SimpleTaskSyncClient(Integer id) throws Exception {
        Properties properties = new Properties();
        properties.load(SimpleTaskSyncClient.class.getResourceAsStream(SyncClientTest.PROPERTIES_FILE_NAME));
        if(properties.size() == 0)
            throw new RuntimeException("start() no properties defined in "+SyncClientTest.PROPERTIES_FILE_NAME);

        String pathToLog4jConfig = (String)properties.getProperty(SyncClientTest.PATH_TO_LOG4J_CONFIG);
        System.out.println("****** pathToLog4jConfig = "+pathToLog4jConfig);
        DOMConfigurator.configure(pathToLog4jConfig);

        StringBuilder sBuilder = new StringBuilder();
        log.info(sBuilder.toString());

        this.nodeId = id.toString();
        kSessionProxy = SyncClientTest.getKnowledgeSessionProxy();

    }

    public void run() {
        Map<String, Object> parameters = null;
        if(SyncClientTest.includeProcessVariables) {
            parameters = new HashMap<String, Object>();
            parameters.put("bonusAmount", new Integer(1500));
            parameters.put("selectedEmployee", "Alex");
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
                    if(SyncClientTest.enableLog){
                        Map<String, Object> pVariables = kSessionProxy.getActiveProcessInstanceVariables(processInstanceId, ksessionId);
                        log.info("run() nodeId = "+nodeId+" : created pInstance w/ id = "+processInstanceId+ " : with following pInstance variables ");
                        for(Map.Entry<String,Object> pVariable : pVariables.entrySet()){
                            log.info("\t"+pVariable.getKey()+ " = "+pVariable.getValue());
                        }
                    }
                } catch(javax.ejb.EJBTransactionRolledbackException x) {
                    StringBuilder sBuilder = new StringBuilder();
                    sBuilder.append("run() nodeId = "+nodeId+" : javax.ejb.EJBTransactionRolledbackException thrown when attempting to start process. cause = \n\t");
                    sBuilder.append(x.getCause());
                    log.error(sBuilder.toString());
                    return;
                } catch(Exception x) {
                    throw new RuntimeException(x);
                }

            }


            // 4)  complete  workItem
            //taskServiceProxy.completeTask(claimedTask.getId(), completedTaskHash, SyncClientTest.userId);
            SyncClientTest.serverNodeCountHash.putIfAbsent(nodeId, new AtomicInteger(0));
            SyncClientTest.serverNodeCountHash.get(nodeId).incrementAndGet();
            completedCount.incrementAndGet();
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
