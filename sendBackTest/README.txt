package com.sample;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.test.JbpmJUnitTestCase;
import org.jbpm.workflow.core.impl.NodeImpl;
import org.jbpm.workflow.instance.node.HumanTaskNodeInstance;
import org.junit.Test;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.WorkflowProcess;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.task.api.TaskService;
import org.kie.internal.task.api.model.TaskSummary;

public class TestKnowledgeSessionService extends JbpmJUnitTestCase {
    
    public TestKnowledgeSessionService() {
        super(true);
        setPersistence(true);
    }
    
    @Test
    public void testSendBack() throws Exception {
        StatefulKnowledgeSession ksession = createKnowledgeSession("apple.three-nodes.bpmn2");
        TaskService taskService = getTaskService(ksession);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("appId", "55");
        ProcessInstance pI = ksession.startProcess("apple.three-nodes", parameters);
        long processInstanceId = pI.getId();
        System.out.println("ProcessInstanceId:" + processInstanceId);
        
        assertNodeTriggered(processInstanceId, "First Task");
        
        for (int i = 0; i < 3; i++) {
            stepForward("krisv", taskService);
        }
        
        assertNodeTriggered(processInstanceId, "Second Task", "Third Task");
        
        pI = ksession.getProcessInstance(processInstanceId);
        Collection<NodeInstance> nodeInstances = ((WorkflowProcessInstance) pI).getNodeInstances();
        assertEquals(1, nodeInstances.size());
        long nodeInstanceId = nodeInstances.iterator().next().getId();
        
        UserTransaction ut = (UserTransaction) new InitialContext().lookup( "java:comp/UserTransaction" );
        ut.begin();
        sendBack(processInstanceId, 3, nodeInstanceId, ksession);
        ut.commit();
        
        for (int i = 0; i < 6; i++) {
            stepForward("krisv", taskService);
        }
        assertProcessInstanceCompleted(processInstanceId, ksession);
     
    }
    
    private void stepForward(String userId, TaskService taskService) {
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(userId, "en-UK");
        assertEquals(1, tasks.size());
        long taskId = tasks.get(0).getId();
        taskService.start(taskId, userId);
        System.out.println("Completing task " + taskId);
        taskService.complete(taskId, userId, null);
    }
    
    private void sendBack(long processInstanceId, long toNodeId, long rejectNodeInstanceId, StatefulKnowledgeSession ksession) {
        RuleFlowProcessInstance processInstance = (RuleFlowProcessInstance) ksession.getProcessInstance(processInstanceId);
        WorkflowProcess process =  (WorkflowProcess) processInstance.getProcess();
        Node toNode = process.getNode(toNodeId);
        
         HumanTaskNodeInstance rejected = (HumanTaskNodeInstance) processInstance.getNodeInstance(rejectNodeInstanceId);
         rejected.cancel();
                 
        org.jbpm.workflow.instance.NodeInstance toNodeInstance = processInstance.getNodeInstance(toNode);
        toNodeInstance.trigger(null, NodeImpl.CONNECTION_DEFAULT_TYPE);
    }

}

