package org.jboss.processFlow;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import org.jboss.processFlow.test.BaseClient;
import org.jboss.processFlow.test.SyncClientTest;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;

public class ConcurrentSignal extends BaseClient implements Runnable {

    private static final String CLIENT_ID = "clientId";
    private static final String WORK_ITEM_COMPLETER="workItemCompleter";
    private static Logger log = Logger.getLogger("ConcurrentSignal");
    private static Integer ksessionId = null;
    private static Long pInstanceId = 0L;
    private static boolean processStarted = false;
    private static Object lockObj = new Object();
    
    public ConcurrentSignal(Integer id) throws Exception {
        super(id);
        processId = "signalAsyncWorkItems";
        init();
    }
                
    private void init() throws Exception{
        synchronized(lockObj) {
            if(processStarted)
                return;

            // a single process instance should be instantiated (and placed in a wait state) 
            // all client threads will manipuate process instance variables of this single pInstance
            Map<String, Object> returnMap = kSessionProxy.startProcessAndReturnId(processId, null);
            if(returnMap == null){
                log.error("run() pInstance creation failed for processId = "+IKnowledgeSession.PROCESS_ID);
                return;
            }
            pInstanceId = (Long)returnMap.get(IKnowledgeSession.PROCESS_INSTANCE_ID);
            ksessionId = (Integer)returnMap.get(IKnowledgeSession.KSESSION_ID);
                
            log.info("init() just started pInstance with:\n\tpInstanceId = "+pInstanceId+" : ksessionId = "+ksessionId);
            processStarted = true;
            Thread.sleep(SyncClientTest.sleepTimeMillis);
        }
    }

    public void run() {
        try {
            final AtomicInteger completedCount = new AtomicInteger(0);
            threadStart = System.currentTimeMillis();
            
            for(counter=1; counter <= SyncClientTest.requestsPerClient; counter++) {
                
                Map<String, Object> signalMap = new HashMap<String, Object>();
                signalMap.put(IKnowledgeSession.PROCESS_INSTANCE_ID, pInstanceId.toString());
                kSessionProxy.signalEvent(WORK_ITEM_COMPLETER, signalMap, pInstanceId, ksessionId);
                
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
        }catch(Exception x){
            x.printStackTrace();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }finally{
        }
    }
}
