package org.jboss.processFlow;

import java.math.BigDecimal;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.TaskException;

import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.tasks.TaskChangeDetails;
import org.jboss.processFlow.test.SyncClientTest;

public class SignalTaskChangeDetails implements Runnable {

    private static final String SIGNAL_SUB_PROCESS_PROCESS_ID = "org.jboss.processFlow.signalTaskChangeDetails.processId";
    private static final String SIGNAL_SUB_PROCESS_SIGNAL_KEY = "org.jboss.processFlow.signalTaskChangeDetails.signalKey";
    private static final String SIGNAL_SUB_PROCESS_SIGNAL_VALUE = "org.jboss.processFlow.signalTaskChangeDetails.signalValue";

    private static Logger log = Logger.getLogger("SignalTaskChangeDetails");
    private String nodeId;
    private int counter = 0;
    private long threadStart= 0L;
    private ITaskService taskServiceProxy = null;
    private IKnowledgeSession kSessionProxy = null;
    private String processId = null;
    private String signalKey = null;

    public SignalTaskChangeDetails(Integer id) throws Exception {
        this.nodeId = id.toString();
        taskServiceProxy = SyncClientTest.getTaskServiceProxy();
        kSessionProxy = SyncClientTest.getKnowledgeSessionProxy();

        Properties properties = new Properties();
        properties.load(SignalTaskChangeDetails.class.getResourceAsStream(SyncClientTest.PROPERTIES_FILE_NAME));
            if(properties.size() == 0)
                throw new RuntimeException("start() no properties defined in "+SyncClientTest.PROPERTIES_FILE_NAME);

        processId = (String)properties.getProperty(SIGNAL_SUB_PROCESS_PROCESS_ID);
        if(processId == null)
            throw new Exception("SignalTaskChangeDetails() need to pass value for :"+SIGNAL_SUB_PROCESS_PROCESS_ID);
        signalKey = (String)properties.getProperty(SIGNAL_SUB_PROCESS_SIGNAL_KEY);
        if(signalKey == null)
            throw new Exception("SignalTaskChangeDetails() need to pass value for :"+SIGNAL_SUB_PROCESS_SIGNAL_KEY);

        if(!SyncClientTest.useAgent)
            SyncClientTest.addProcessToKnowledgeBase();
    }

    public void run() {
            Map<String, Object> parameters = null;
            parameters = new HashMap<String, Object>();

            final AtomicInteger completedCount = new AtomicInteger(0);
            threadStart = System.currentTimeMillis();
            for(counter=1; counter <= SyncClientTest.requestsPerClient; counter++) {

                final long sendTime = System.currentTimeMillis();
                Map<String, Object> returnMap = null;
                if(SyncClientTest.startNewProcessInstance) {
                    try {
                        returnMap = kSessionProxy.startProcessAndReturnId(processId, parameters);
                        if(returnMap == null){
                            log.error("run() pInstance creation failed for processId = "+processId);
                            return;
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
                Long processInstanceId = (Long)returnMap.get(IKnowledgeSession.PROCESS_INSTANCE_ID);
                Integer ksessionId = (Integer)returnMap.get(IKnowledgeSession.KSESSION_ID);

                try {
                    List<TaskSummary> tasks = taskServiceProxy.getTasksByProcessInstance(processInstanceId, null);
                    long taskId = tasks.get(0).getId();

                    // test normal  completion
                    //taskServiceProxy.completeTask(taskId, null, "admin");

                    TaskChangeDetails changeDetails = new TaskChangeDetails();
                    changeDetails.setTaskId(taskId);
                    Random randomObj = new Random();
                    String reason = "reason"+randomObj.nextInt(3);
                    log.info("*** reason = "+reason);
                    changeDetails.setReason(reason);
                    Thread.sleep(SyncClientTest.sleepTimeMillis);
                    if(ITaskService.FAIL_TASK_SIGNAL.equals(signalKey)) {
                    taskServiceProxy.startTask(taskId, "admin");
                    }
                    kSessionProxy.signalEvent(signalKey, changeDetails, processInstanceId, ksessionId);
                } catch(Exception x) {
                    x.printStackTrace();
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
}
