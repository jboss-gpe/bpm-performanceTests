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

public class SimpleTaskSyncClient implements Runnable {

    private static Logger log = Logger.getLogger("SimpleTaskSyncClient");
    private static final String PROCESS_ID = "simpleTask";

    private boolean claimTask = true;
    private boolean completeTask = true;
    private String nodeId;
    private int counter = 0;
    private long threadStart= 0L;
    private ITaskService taskServiceProxy = null;
    private IKnowledgeSession kSessionProxy = null;
    

    public SimpleTaskSyncClient(Integer id) throws Exception {
        this.nodeId = id.toString();
        taskServiceProxy = SyncClientTest.getTaskServiceProxy();
        kSessionProxy = SyncClientTest.getKnowledgeSessionProxy();

        Properties properties = new Properties();
        properties.load(SimpleTaskSyncClient.class.getResourceAsStream(SyncClientTest.PROPERTIES_FILE_NAME));
        if(properties.size() == 0)
            throw new RuntimeException("start() no properties defined in "+SyncClientTest.PROPERTIES_FILE_NAME);

        String pString = (String)properties.getProperty("org.jboss.processFlow.claimTask");
        if(pString != null)
            claimTask = Boolean.parseBoolean(pString);
        pString = (String)properties.getProperty("org.jboss.processFlow.completeTask");
        if(pString != null)
            completeTask = Boolean.parseBoolean(pString);

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("\n\tclaimTask = "+claimTask);
        sBuilder.append("\n\tcompleteTask = "+completeTask);
        log.info(sBuilder.toString());
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

                if(SyncClientTest.abortProcessInstance){
                    log.warn("run() about to abort pInstance w/ id = "+processInstanceId);
                    kSessionProxy.abortProcessInstance(processInstanceId, ksessionId);
                    break;
                }
            }


            if(!claimTask)
                continue;
            
            
            TaskSummary claimedTask = null;
            List<String> groupList = new ArrayList<String>();
            groupList.add(SyncClientTest.groupId);
            List<Status> statuses = new ArrayList<Status>();
            statuses.add(Status.Ready);
            if(SyncClientTest.useGuaranteedClaimApproach) {
                // loop twice unless task not claimed with first attempt
                for(int d=0; d < 2; d++) {
                    claimedTask = taskServiceProxy.guaranteedClaimTaskAssignedAsPotentialOwnerByStatusByGroup(SyncClientTest.userId, groupList, statuses, "en-UK", 0, 10);
                    if(claimedTask != null)
                        break;
                }
            }else {

                // 2)  query for any tasks with role 'creditController'
                List<TaskSummary> taskList = taskServiceProxy.getTasksAssignedAsPotentialOwnerByStatusByGroup(SyncClientTest.userId, groupList, statuses, "en-UK", 0, 10);
                if(taskList.size() == 0) {
                    log.error("run() nodeId = "+nodeId+" : # zero tasks with groupId "+SyncClientTest.groupId);
                    return;
                }

                // 3)  iterate through list of tasks and claim the first one possible
                for(TaskSummary tObj : taskList) {
                    try {
                        taskServiceProxy.claimTask(tObj.getId(), SyncClientTest.userId, SyncClientTest.userId, groupList);
                        if(SyncClientTest.enableLog) {
                            log.info("run() nodeId = "+nodeId+" : just claimed task = "+tObj.getId());
                            TaskSummary tTestObj = taskServiceProxy.getTask(tObj.getId());
                            Map<String, Object> tVariables = taskServiceProxy.getTaskContent(tObj.getId(), true);
                            log.info("run() inbound task variables as follows for claimed task w/ id = "+tObj.getId());
                            for(Map.Entry<String,Object> tVariable : tVariables.entrySet()){
                                log.info("\t"+tVariable.getKey()+ " = "+tVariable.getValue());
                            }
                        }
                        claimedTask = tObj;
                        break;
                    }catch(javax.ejb.EJBException x) {
                        if(SyncClientTest.enableLog) 
                            log.error("run() claimTask.  nodeId = "+nodeId+" : EJBException cause = "+x.getCause()+" : taskId = "+tObj.getId());
                    }catch(org.jbpm.task.service.PermissionDeniedException x){
                        if(SyncClientTest.enableLog)
                            log.error("run() claimTask.  nodeId = "+nodeId+" : PermissionDeniedException : taskId = "+tObj.getId());
                    }
                }
            }

            if(claimedTask == null) {
                log.error("run() nodeId = "+nodeId+" : no tasks claimed nor completed");
                return;
            }

            if(!SyncClientTest.checkDupsSet.add(claimedTask.getId())){
                log.error("run() nodeId = "+nodeId+" : ******** CLAIMED THE SAME TASK MORE THAN ONCE : "+claimedTask.getId());
                return;
            }

            if(!completeTask)
                continue;

            // 4)  complete the task previously claimed
            Map<String, Object> completedTaskHash = new HashMap();
            try {Thread.sleep(SyncClientTest.sleepTimeMillis);}catch(Exception x){x.printStackTrace();}
            if(SyncClientTest.includeProcessVariables) {
                completedTaskHash.put("taskSelectedEmployee", "Azra");
                completedTaskHash.put("taskBonusAmount", new Integer(15000));
            }
            try {
                taskServiceProxy.completeTask(claimedTask.getId(), completedTaskHash, SyncClientTest.userId);
                log.info("run() nodeId = "+nodeId+" :\tcompleteTask = "+claimedTask.getId()+"\tcounter = "+counter+"\tduration = "+(System.currentTimeMillis() - sendTime));
            }catch(javax.ejb.EJBException x) {
                log.error("run() completeTask nodeId = "+nodeId+" : exception = "+x.getCause());
                continue;
            }catch(org.jbpm.task.service.PermissionDeniedException x){
                log.error("run() completeTask nodeId = "+nodeId+" : PermissionDeniedException : taskId = "+claimedTask.getId());
                continue;
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
    private TaskSummary guaranteedClaimTaskAssignedAsPotentialOwnerByStatusByGroup(String userId, List<String>groupList, List<Status> statuses, String language, Integer firstResult, Integer maxResults){
        return taskServiceProxy.guaranteedClaimTaskAssignedAsPotentialOwnerByStatusByGroup(SyncClientTest.userId, groupList, statuses, "en-UK", 0, 10);
    }

    public void printInboundTaskVariables(long taskId) {
    }
}
