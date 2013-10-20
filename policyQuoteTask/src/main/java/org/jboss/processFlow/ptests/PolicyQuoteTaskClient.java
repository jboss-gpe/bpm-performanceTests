package org.jboss.processFlow.ptests;

import java.math.BigDecimal;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import org.apache.log.Logger;

import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;

import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.test.EJBServicesFactory;

import org.acme.insurance.Policy;
import org.acme.insurance.Driver;

public class PolicyQuoteTaskClient extends AbstractJavaSamplerClient {

    private static final String DEPLOYMENT_ID = "deploymentId";
    private static final String PROCESS_ID = "processId";
    private static final String USER_ID = "userId";

    private Logger log = getLogger();

    private String deploymentId;
    private String processId;
    private boolean claimTask = true;
    private boolean completeTask = true;
    private String userId;
    
    public SampleResult runTest(JavaSamplerContext context) {
        ITaskService tProxy = EJBServicesFactory.getTaskServiceProxy();
        IKnowledgeSession kProxy = EJBServicesFactory.getKnowledgeSessionProxy();

        deploymentId = System.getProperty(DEPLOYMENT_ID, "git-playground");
        processId = System.getProperty(PROCESS_ID, "policyQuoteTask");
        userId = System.getProperty(USER_ID, "jboss");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Driver.DRIVER_NAME, System.getProperty(Driver.DRIVER_NAME, "Azra"));
        parameters.put(Driver.AGE, context.getIntParameter(Driver.AGE, 20));
        parameters.put(Driver.NUMBER_OF_ACCIDENTS, context.getIntParameter(Driver.NUMBER_OF_ACCIDENTS, 0));
        parameters.put(Driver.NUMBER_OF_TICKETS, context.getIntParameter(Driver.NUMBER_OF_TICKETS, 1));
        parameters.put(Policy.VEHICLE_YEAR, context.getIntParameter(Policy.VEHICLE_YEAR, 2010));

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("\n\t"+DEPLOYMENT_ID+" : "+deploymentId);
        sBuilder.append("\n\t"+PROCESS_ID+" : "+processId);
        sBuilder.append("\n\t"+USER_ID+" : "+userId);
        for (Map.Entry entry : parameters.entrySet()) {
            sBuilder.append("\n\t"+entry.getKey() + " : " + entry.getValue());
        }
        log.info("runTest() pVariables = "+sBuilder.toString());

        // 1)  start new process
        final long sendTime = System.currentTimeMillis();
        long pInstanceId = 0L;
        int ksessionId =0;
        Map<String, Object> returnMap = null;
        SampleResult results = new SampleResult();
        results.sampleStart();
        try {
            sBuilder = new StringBuilder();
            returnMap = kProxy.startProcessAndReturnId(processId, parameters, deploymentId);
            pInstanceId = (Long)returnMap.get(IKnowledgeSession.PROCESS_INSTANCE_ID);
            sBuilder.append("pInstanceId = "+pInstanceId);

            // sleep for two seconds to ensure all has been flushed to database prior to querying
            Thread.sleep(2000);

            // query for tasks by pInstanceId
            List<TaskSummary> tasks = tProxy.getTasksAssignedAsPotentialOwner(userId, ITaskService.ENGLISH);
            TaskSummary tObj = tasks.get(0);
            sBuilder.append("taskId = "+tObj.getId());

            // claim task
            tProxy.claimTask(tObj.getId(), userId);

            // start task
            tProxy.startTask(tObj.getId(), userId);

            // complete task
            parameters = new HashMap<String, Object>();
            parameters.put("taskPrice", new Integer(450));
            tProxy.completeTask(tObj.getId(), parameters, userId);

            results.setResponseMessage(sBuilder.toString());
            results.setSuccessful(true);
            results.setResponseCodeOK();
        } catch(javax.ejb.EJBTransactionRolledbackException x) {
            sBuilder = new StringBuilder();
            sBuilder.append("run() javax.ejb.EJBTransactionRolledbackException thrown when attempting to start process. cause = \n\t");
            sBuilder.append(x.getCause());
            results.setResponseMessage(sBuilder.toString());
            results.setSuccessful(false);
        } catch(Exception x) {
            throw new RuntimeException(x);
        }

        results.sampleEnd();
        return results;
    }
}
