package com.redhat.gpe.kie.services.client.api;

import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotEquals;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.api.RemoteRestRuntimeFactory;
import org.kie.services.client.api.RestRequestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.acme.insurance.Driver;
import org.acme.insurance.Policy;

public class LiveServerTest {

    public static final String DEPLOYMENT_ID = "deploymentId";
    public static final String DEPLOYMENT_URL = "deployment.url";
    public static final String USER_ID = "userId";
    public static final String PASSWORD = "password";
    public static final String PROCESSID = "processId";
    public static final String DRIVER_NAME = "driverName";
    public static final String AGE = "age";
    public static final String NUMBER_OF_ACCIDENTS = "numberOfAccidents";
    public static final String NUMBER_OF_TICKETS = "numberOfTickets";
    public static final String VEHICLE_YEAR = "vehicleYear";
    public static final String CREDIT_SCORE = "creditScore";
    public static final String DRIVER = "driver";
    public static final String POLICY = "policy";

    protected static Logger log = LoggerFactory.getLogger(LiveServerTest.class);

    private String deploymentId = "org.jbpm:Evaluation:1.0";
    private URL deploymentUrl;
    private String userId;
    private String password;
    private String processId;

    private String name;
    private int age;
    private int numAccidents;
    private int numTickets;
    private int vehicleYear;
    private int creditScore;

    public LiveServerTest() throws Exception {
        this.deploymentId = System.getProperty(DEPLOYMENT_ID, "git-playground");
        this.deploymentUrl = new URL(System.getProperty(DEPLOYMENT_URL, "http://zareason:8330/kie-jbpm-services/"));
        this.userId = System.getProperty(USER_ID, "jboss");
        this.password = System.getProperty(PASSWORD, "brms");
        this.processId = System.getProperty(PROCESSID, "simpleTask");

        this.name = System.getProperty(DRIVER_NAME, "alex");
        this.age = Integer.parseInt(System.getProperty(AGE, "21"));
        this.numAccidents = Integer.parseInt(System.getProperty(NUMBER_OF_ACCIDENTS, "0"));
        this.numTickets = Integer.parseInt(System.getProperty(NUMBER_OF_TICKETS, "1"));
        this.vehicleYear = Integer.parseInt(System.getProperty(VEHICLE_YEAR, "2011"));
        this.creditScore = Integer.parseInt(System.getProperty(CREDIT_SCORE, "800"));

        StringBuilder sBuilder = new StringBuilder("system properties =");
        sBuilder.append("\n\tdeploymentId : "+deploymentId);
        sBuilder.append("\n\tdeploymentUrl : "+deploymentUrl);
        sBuilder.append("\n\tuserId : "+userId);
        sBuilder.append("\n\tpassword : "+password);
        sBuilder.append("\n\tprocessId : "+processId);
        sBuilder.append("\n\tdriverName : "+name);
        sBuilder.append("\n\tage : "+age);
        sBuilder.append("\n\t# accidents : "+numAccidents);
        sBuilder.append("\n\t# tickets : "+numTickets);
        sBuilder.append("\n\t# creditScore : "+creditScore);
        sBuilder.append("\n\tvehicle year : "+vehicleYear);
        log.info(sBuilder.toString());
    }

    @Test
    public void remoteRestApi() {
        String taskUserId = userId;
        
        RemoteRuntimeEngineFactory restSessionFactory = new RemoteRestRuntimeFactory(deploymentId, deploymentUrl, userId, password);

        // create REST request
        RuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        Map<String, Object> params = new HashMap<String, Object>();

        // populate domain model classes
        Driver driverObj = new Driver();
        driverObj.setDriverName(name);
        driverObj.setAge(age);
        driverObj.setNumberOfAccidents(numAccidents);
        driverObj.setNumberOfTickets(numTickets);
        driverObj.setCreditScore(creditScore);
        Policy policyObj = new Policy();
        policyObj.setVehicleYear(vehicleYear);
        policyObj.setDriver(driverObj);
        params.put(POLICY, policyObj);
        
        ProcessInstance processInstance = ksession.startProcess(processId, params);

        log.info("Started process instance: " + processInstance + " " + (processInstance == null ? "" : processInstance.getId()));

        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(userId, "en-UK");
        long taskId = findTaskId(processInstance.getId(), tasks);

        Task task = taskService.getTaskById(taskId);
        log.info("Got task " + taskId + ": " + task);
        taskService.start(taskId, taskUserId);
        taskService.complete(taskId, taskUserId, null);

        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Completed);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(processInstance.getId(), statuses, "en-UK");
        assertEquals("Expected 1 tasks.", 1, taskIds.size());
    }

    protected long findTaskId(long procInstId, List<TaskSummary> taskSumList) {
        long taskId = -1;
        for (TaskSummary task : taskSumList) {
            if (task.getProcessInstanceId() == procInstId) {
                taskId = task.getId();
                return taskId;
            }
        }
        throw new RuntimeException("findTaskId() no tasks found for pInstanceId = "+procInstId);
    }

    private ClientResponse<?> checkResponse(ClientResponse<?> responseObj) throws Exception {
        responseObj.resetStream();
        int status = responseObj.getStatus();
        if (status != 200) {
            System.out.println("Response with exception:\n" + responseObj.getEntity(String.class));
            assertEquals("Status OK", 200, status);
        }
        return responseObj;
    }

}
