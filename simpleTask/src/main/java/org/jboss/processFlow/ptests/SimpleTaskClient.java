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

import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.test.EJBServicesFactory;

import org.acme.insurance.Policy;
import org.acme.insurance.Driver;

public class SimpleTaskClient extends AbstractJavaSamplerClient {

    private static final String DEPLOYMENT_ID = "deploymentId";
    private static final String PROCESS_ID = "processId";

    private Logger log = getLogger();

    private String deploymentId;
    private String processId;
    private boolean claimTask = true;
    private boolean completeTask = true;
    
    public SampleResult runTest(JavaSamplerContext context) {
        ITaskService taskServiceProxy = EJBServicesFactory.getTaskServiceProxy();
        IKnowledgeSession kSessionProxy = EJBServicesFactory.getKnowledgeSessionProxy();

        deploymentId = System.getProperty(DEPLOYMENT_ID, "git-playground");
        processId = System.getProperty(PROCESS_ID, "simpleTask");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Driver.DRIVER_NAME, System.getProperty(Driver.DRIVER_NAME, "Azra"));
        parameters.put(Driver.AGE, context.getIntParameter(Driver.AGE, 20));
        parameters.put(Driver.NUMBER_OF_ACCIDENTS, context.getIntParameter(Driver.NUMBER_OF_ACCIDENTS, 0));
        parameters.put(Driver.NUMBER_OF_TICKETS, context.getIntParameter(Driver.NUMBER_OF_TICKETS, 1));
        parameters.put(Policy.VEHICLE_YEAR, context.getIntParameter(Policy.VEHICLE_YEAR, 2010));

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("\n\t"+DEPLOYMENT_ID+" : "+deploymentId);
        sBuilder.append("\n\t"+PROCESS_ID+" : "+processId);
        for (Map.Entry entry : parameters.entrySet()) {
            sBuilder.append("\n\t"+entry.getKey() + " : " + entry.getValue());
        }
        log.info("runTest() pVariables = "+sBuilder.toString());

        // 1)  start new process
        final long sendTime = System.currentTimeMillis();
        long processInstanceId = 0L;
        int ksessionId =0;
        Map<String, Object> returnMap = null;
        SampleResult results = new SampleResult();
        results.sampleStart();
        try {
            returnMap = kSessionProxy.startProcessAndReturnId(processId, parameters, deploymentId);
            if(returnMap == null){
                results.setResponseMessage("pInstance creation failed for processId = "+processId);
                results.setSuccessful(false);
            }else {
                processInstanceId = (Long)returnMap.get(IKnowledgeSession.PROCESS_INSTANCE_ID);
                results.setResponseMessage("pInstanceId = "+processInstanceId);
                results.setSuccessful(true);
                results.setResponseCodeOK();
            }
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
