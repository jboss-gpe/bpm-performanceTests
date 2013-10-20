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

import org.jboss.processFlow.test.SyncClientTest;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;

public class SimpleTimerSyncClient implements Runnable {

    private static Logger log = Logger.getLogger("SimpleTimerSyncClient");

    private String nodeId;
    private int counter = 0;
    private long threadStart= 0L;
    private IKnowledgeSession kSessionProxy = null;
    private String processId;
    

    public SimpleTimerSyncClient(Integer id) throws Exception {
        this.nodeId = id.toString();
        kSessionProxy = SyncClientTest.getKnowledgeSessionProxy();

        Properties properties = new Properties();
        properties.load(SimpleTimerSyncClient.class.getResourceAsStream(SyncClientTest.PROPERTIES_FILE_NAME));
        if(properties.size() == 0)
            throw new RuntimeException("start() no properties defined in "+SyncClientTest.PROPERTIES_FILE_NAME);
        processId = properties.getProperty("org.jboss.processFlow.timerTest.processId");
    }

    public void run() {
        Map<String, Object> parameters = null;
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
                    returnMap = kSessionProxy.startProcessAndReturnId(processId, parameters);
                    if(returnMap == null){
                        log.error("run() pInstance creation failed for processId = "+processId);
                        return;
                    }
                
                    processInstanceId = (Long)returnMap.get(IKnowledgeSession.PROCESS_INSTANCE_ID);
                    ksessionId = (Integer)returnMap.get(IKnowledgeSession.KSESSION_ID);
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
}
