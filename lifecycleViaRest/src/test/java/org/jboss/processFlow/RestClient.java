package org.jboss.processFlow;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import org.apache.log4j.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.test.SyncClientTest;
import org.jboss.processFlow.test.BaseRestClient;

public class RestClient extends BaseRestClient implements Runnable {

    private static final Logger log = Logger.getLogger(RestClient.class);

    public RestClient(Integer id) throws IOException {
        super(id);
    }

    public void run() {
        try {
            httpclient = new DefaultHttpClient();
            authenticateIntoBusinessCentral();

            final AtomicInteger completedCount = new AtomicInteger(0);
            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("selectedEmployee", new StringBody("alex"));
            reqEntity.addPart("bonusAmount", new StringBody("1500"));
            if(deliverAsync)
                reqEntity.addPart(IKnowledgeSession.DELIVER_ASYNC, new StringBody("NOT USED"));
            threadStart = System.currentTimeMillis();
            JsonNode responseNode = null;
            HttpPost httpPost = null;
            HttpGet httpGet = null;
            String targetUrl = null;
            HttpResponse response = null;

            for(counter=1; counter <= SyncClientTest.requestsPerClient; counter++) {
                
                // 1)  start a new process instance
                if(useForm){
                    // using Form URL ... NOTE:  will not get a usable return value when using FORM URL
                    System.out.println("processId = "+processId);
                    targetUrl = urlPrefix+SUBDOMAIN_FORM_BASED_NEW_INSTANCE+processId+"/complete";
                    httpPost = new HttpPost(targetUrl);
                    httpPost.setEntity(reqEntity);
                    response = httpclient.execute(httpPost);
                    processResponse(response, targetUrl, HttpStatus.SC_OK);

                    if(deliverAsync) {
                        // sleep for a few seconds to make sure pInstance has started .... could also implement JMS callback that KnowledgeSessionMDB provides
                        Thread.sleep(2000);
                    }
                }else {
                    // not using Form URL ... will receive back a usable return value
                    targetUrl = urlPrefix+SUBDOMAIN_NEW_INSTANCE+processId+"/new_instance";
                    httpPost = new HttpPost(targetUrl);
                    response = httpclient.execute(httpPost);
                    String newPInstanceResponse = processResponse(response, targetUrl, HttpStatus.SC_OK);
                    responseNode = jsonMapper.readValue(newPInstanceResponse, JsonNode.class);
                    String pInstanceId = responseNode.path(ID).getTextValue();
                    String startDate = responseNode.path(START_DATE).getTextValue();
                    String definitionId = responseNode.path(DEFINITION_ID).getTextValue();
                    log.info("run() just started new pInstance as follows :  pDefId = "+definitionId+" : pInstanceId = "+pInstanceId+" : startDate = "+startDate);
                }
                
                // 2)  query for all tasks where this userId could be a potential owner
                targetUrl = urlPrefix+SUBDOMAIN_TASKS+userId+"/participation";
                httpGet = new HttpGet(targetUrl);
                response = httpclient.execute(httpGet);
                String taskString = processResponse(response, targetUrl, HttpStatus.SC_OK);
                responseNode = jsonMapper.readValue(taskString, JsonNode.class);
                ArrayNode tasksNode = (ArrayNode)responseNode.path("tasks");
                if(tasksNode.size() == 0) {
                    log.error("run() nodeId = "+nodeId+" : # zero tasks for user = "+userId);
                    return;
                }
                
                // 3)  iterate through tasks and claim the first available
                JsonTask claimedTask = null;
                for(int t=0; t < tasksNode.size(); t++){
                    JsonNode taskNode = tasksNode.get(t);
                    JsonTask jTask = jsonMapper.readValue(taskNode, JsonTask.class);
                    try {
                        targetUrl = urlPrefix+SUBDOMAIN_TASK+jTask.getId()+"/assign/"+userId;
                        httpPost = new HttpPost(targetUrl);
                        response = httpclient.execute(httpPost);
                        String claimTaskResponse = processResponse(response, targetUrl, HttpStatus.SC_OK);
                        claimedTask = jTask;
                        break;
                    }catch(Exception u){
                        log.error(u.getLocalizedMessage());
                    }
                }
                if(claimedTask == null){
                    log.error("run() nodeId = "+nodeId+" : unable to claim any tasks");
                    return;
                }
                log.info("run() just claimed task = "+claimedTask.getId()+" : from pInstanceId = "+claimedTask.getProcessInstanceId()+" : processId = "+claimedTask.getProcessId());

                
                // 4)  now complete the task
                targetUrl = urlPrefix+"/rs/form/task/"+claimedTask.getId()+"/complete";
                httpPost = new HttpPost(targetUrl);
                reqEntity = new MultipartEntity();
                reqEntity.addPart("taskSelectedEmployee", new StringBody("azra"));
                reqEntity.addPart("taskBonusAmount", new StringBody("15000"));
                httpPost.setEntity(reqEntity);
                response = httpclient.execute(httpPost);
                String completedTaskResponse = processResponse(response, targetUrl, HttpStatus.SC_OK);
                log.info("run() just completed task = "+claimedTask.getId()+" : from pInstanceId = "+claimedTask.getProcessInstanceId()+" : processId = "+claimedTask.getProcessId());

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
            httpclient.getConnectionManager().shutdown();
        }
    }
}

class JsonTask implements Serializable {
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getProcessInstanceId() {
        return processInstanceId;
    }
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
    public String getProcessId() {
        return processId;
    }
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAssignee() {
        return assignee;
    }
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    public String getIsBlocking() {
        return isBlocking;
    }
    public void setIsBlocking(String isBlocking) {
        this.isBlocking = isBlocking;
    }
    public String getIsSignalling() {
        return isSignalling;
    }
    public void setIsSignalling(String isSignalling) {
        this.isSignalling = isSignalling;
    }
    public List<String> getOutcomes() {
        return outcomes;
    }
    public void setOutcomes(List<String> outcomes) {
        this.outcomes = outcomes;
    }
    public String getCurrentState() {
        return currentState;
    }
    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }
    public List<String> getParticipantUsers() {
        return participantUsers;
    }
    public void setParticipantUsers(List<String> participantUsers) {
        this.participantUsers = participantUsers;
    }
    public List<String> getParticipantGroups() {
        return participantGroups;
    }
    public void setParticipantGroups(List<String> participantGroups) {
        this.participantGroups = participantGroups;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getPriority() {
        return priority;
    }
    public void setPriority(String priority) {
        this.priority = priority;
    }
    private String id;
    private String processInstanceId;
    private String processId;
    private String name;
    private String assignee;
    private String isBlocking;
    private String isSignalling;
    private String currentState;
    private List<String> outcomes;
    private List<String> participantUsers;
    private List<String> participantGroups;
    private String url;
    private String priority;
    
}
