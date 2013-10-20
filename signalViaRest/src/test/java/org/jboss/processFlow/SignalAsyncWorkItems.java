package org.jboss.processFlow;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.log4j.Logger;
import org.jboss.processFlow.test.BaseRestClient;
import org.jboss.processFlow.test.SyncClientTest;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;

public class SignalAsyncWorkItems extends BaseRestClient implements Runnable {

    private static final String SIGNAL_VIA_HTTP = "org.jboss.processFlow.signal.via.http";
    public static final String SIGNAL_VIA_EJB = "org.jboss.processFlow.signal.via.ejb";
    public static final String COMPLETE_WI_VIA_HTTP = "org.jboss.processFlow.complete.wi.via.http";
    public static final String SIGNAL_METHOD = "org.jboss.processFlow.signal.method";
    
    private static final String CLIENT_ID = "clientId";
    private static final String WORK_ITEM_COMPLETER="workItemCompleter";
    private static Logger log = Logger.getLogger("SignalAsyncWorkItems");
    private static Connection connectionObj = null;
    private static Destination gwDObj = null;
    private static boolean largeRequestBody = false;
    private static long blockTime = 5000L;
    
    private Session sessionObj = null;
    private String signalMethod;

    public SignalAsyncWorkItems(Integer id) throws Exception {
        super(id);
        Integer hqRemotingPort = Integer.parseInt(properties.getProperty("remote.connection.hornetq.port"));
        String pfpCoreHostName = properties.getProperty("org.jboss.processFlow.pfp.core.host.name");
        processId = properties.getProperty("org.jboss.processFlow.signalViaRest.process.id", "simpleTask");
        largeRequestBody = Boolean.parseBoolean(properties.getProperty("org.jboss.processFlow.signalViaRest.large.request.body", "FALSE"));
        signalMethod = properties.getProperty(SIGNAL_METHOD, SIGNAL_VIA_HTTP);
        System.out.println("main() signalMethod = "+signalMethod);
    }

    public void run() {
        try {
            httpclient = new DefaultHttpClient();

            final AtomicInteger completedCount = new AtomicInteger(0);
            MultipartEntity reqEntity = new MultipartEntity();
            threadStart = System.currentTimeMillis();
            HttpEntityEnclosingRequestBase httpBase = null;
            String targetUrl = null;
            HttpResponse response = null;
            authenticateIntoBusinessCentral();
            
            for(counter=1; counter <= SyncClientTest.requestsPerClient; counter++) {
                
                // 1)  start a new process instance
                targetUrl = urlPrefix+SUBDOMAIN_FORM_BASED_NEW_INSTANCE+processId+"/complete";
                httpBase = new HttpPost(targetUrl);
                reqEntity.addPart(CLIENT_ID, new StringBody(nodeId));
                httpBase.setEntity(reqEntity);
                response = httpclient.execute(httpBase);
                processResponse(response, targetUrl, HttpStatus.SC_OK);
               
                Long workItemId = 0L;
                Long pInstanceId = 0L;
                System.out.println("run() just received tMessage with following props:\n\tworkItemId = "+workItemId+"\n\tpInstanceId = "+pInstanceId);
                Thread.sleep(SyncClientTest.sleepTimeMillis);
                
                if(signalMethod.equals(SIGNAL_VIA_EJB)){
                    Map<String, Object> signalMap = new HashMap<String, Object>();
                    
                    signalMap.put(IKnowledgeSession.PROCESS_INSTANCE_ID, pInstanceId.toString());
                    signalMap.put(IKnowledgeSession.WORK_ITEM_ID, workItemId.toString());
                    kSessionProxy.signalEvent(WORK_ITEM_COMPLETER, signalMap, pInstanceId, null);
                }else if (signalMethod.equals(SIGNAL_VIA_HTTP)){
                    if(largeRequestBody) {
                        // build targetUrl to PFP knowledgeService REST API
                        targetUrl = buildTargetUrl()+"/knowledgeService/"+SUBDOMAIN_SIGNAL+pInstanceId+"/transition?signalType="+WORK_ITEM_COMPLETER;
                        
                        // build payload that mimics format used by PFP modified business-central-server REST api
                        String jsonPayload = "[{ \"name\": \"azra\", \"age\": 9 }, { \"name\": \"alex\", \"age\": 4 }]";
                        StringBuilder pBuilder = new StringBuilder();
                        pBuilder.append("$"+IKnowledgeSession.WORK_ITEM_ID+"$");
                        pBuilder.append(workItemId.toString());
                        pBuilder.append("$jsonPayload$");
                        pBuilder.append(jsonPayload);
                        httpBase = new HttpPut(targetUrl);
                        httpBase.setEntity(new StringEntity(pBuilder.toString()));
                    }else {
                        targetUrl = urlPrefix+SUBDOMAIN_SIGNAL+pInstanceId+"/transition?signal="+WORK_ITEM_COMPLETER+"$processInstanceId$"+pInstanceId+"$workItemId$"+workItemId;
                        httpBase = new HttpPost(targetUrl);
                    }
                    response = httpclient.execute(httpBase);
                    processResponse(response, targetUrl, HttpStatus.SC_OK);
                }else if (signalMethod.equals(COMPLETE_WI_VIA_HTTP)){
                    // build targetUrl to PFP knowledgeService REST API
                    targetUrl = buildTargetUrl()+"/knowledgeService"+SUBDOMAIN_SIGNAL+pInstanceId+"/"+workItemId+"/complete";
                    
                    // build payload that mimics format used by PFP modified business-central-server REST api
                    String jsonPayload = "[{ \"name\": \"azra\", \"age\": 9 }, { \"name\": \"alex\", \"age\": 4 }]";
                    StringBuilder pBuilder = new StringBuilder();
                    pBuilder.append("$jsonPayload$");
                    pBuilder.append(jsonPayload);
                    httpBase = new HttpPut(targetUrl);
                    httpBase.setEntity(new StringEntity(pBuilder.toString()));
                    response = httpclient.execute(httpBase);
                    processResponse(response, targetUrl, HttpStatus.SC_OK);
                }else {
                	log.error("*** DID NOT PASS VALID SIGNAL METHOD : "+signalMethod);
                	return;
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
        }catch(Exception x){
            x.printStackTrace();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }finally{
            httpclient.getConnectionManager().shutdown();
            if(connectionObj != null)
                try {
                    connectionObj.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
        }
    }
    
    private String buildTargetUrl() {
    	String protocol = properties.getProperty(PROTOCOL_KEY, "http");
    	String host = properties.getProperty(HOST_KEY, "localhost");
    	String port = properties.getProperty(KSERVICE_PORT_KEY, "8330");
    	StringBuilder sBuilder = new StringBuilder();
    	sBuilder.append(protocol);
    	sBuilder.append(SEPERATOR);
    	sBuilder.append(host);
    	sBuilder.append(":");
    	sBuilder.append(port);
    	sBuilder.append("/pfpRest");
    	return sBuilder.toString();
    }
}
