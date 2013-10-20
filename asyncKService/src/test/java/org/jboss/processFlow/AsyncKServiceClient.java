package org.jboss.processFlow;

import java.io.File;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InitialContext;

import javax.jms.*;
import javax.naming.Context;

import org.apache.log4j.Logger;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.tasks.TaskChangeDetails;
import org.jbpm.task.query.TaskSummary;

/**
 *  AsyncKServiceClient
 *  JA Bride
 *  23 March, 2008
 */
public class AsyncKServiceClient {

    public static final String PROPERTIES_FILE_NAME="/jbpm-performanceTests.properties";
    private static final String ID = "clientId";
    private static final String BIND_ADDRESS = "bindAddress";
    private static final String FINAL_MESSAGE = "FINAL_MESSAGE";
    private static Logger log = Logger.getLogger(AsyncKServiceClient.class);

    private static String cFactoryName = "/ClusteredConnectionFactory";
    private static int clientCount = 0;
    private static int requestsPerClient = 0;
    private static int byteMessageSize = 1;
    private static boolean isPersistent = false;
    private static String gwDObjName = null;
    private static String responseQueueName = null;
    private static String completionQueueName = null;
    private static int sleepTimeMillis = 0;
    private static boolean logTrnxResult = true;
    private static boolean blockForTrnxResponse = true;
    private static int totalCount = 0;
    private static Connection connectionObj = null;
    private static Destination gwDObj = null;
    private static ExecutorService execObj = null;
    private static BigDecimal bdThousand;
    private static int debuggerPort = 0;
    private static boolean useAgent=true;
    private static boolean enableLog = true;
    private static String absolutePathToBpmn;
    private static int rampUpInterval = 0;
    private static int hornetqRemotingPort = 0;
    private static String pfpCoreHostName;
    private static String processId;
    private static String signalKey;
    private static ITaskService taskServiceProxy = null;
    private static final ConcurrentMap<String, AtomicInteger> serverNodeCountHash = new ConcurrentHashMap<String, AtomicInteger>();

    public static void main(String args[]) {
        Properties properties = new Properties();
        try {
            properties.load(AsyncKServiceClient.class.getResourceAsStream(PROPERTIES_FILE_NAME));
            if(properties.size() == 0)
                throw new RuntimeException("start() no properties defined in "+PROPERTIES_FILE_NAME);

            String pString = (String)properties.getProperty("org.jboss.processFlow.useKnowledgeAgent");
            if(pString != null)
                useAgent = Boolean.parseBoolean(pString);
            if(properties.get("org.jboss.processFlow.testClient.debuggerPort") != null)
                debuggerPort = Integer.parseInt((String)properties.get("org.jboss.processFlow.testClient.debuggerPort"));
            if(properties.get("org.jboss.processFlow.clientCount") != null)
                clientCount = Integer.parseInt((String)properties.get("org.jboss.processFlow.clientCount"));
            if(properties.get("org.jboss.processFlow.requestsPerClient") != null)
                requestsPerClient = Integer.parseInt((String)properties.get("org.jboss.processFlow.requestsPerClient"));

            pString = (String)properties.get("org.jboss.processFlow.enableLog");
            if(pString != null)
                enableLog = Boolean.parseBoolean(pString);
            if(System.getProperty("org.jboss.processFlow.absolutePathToBpmn") != null)
                absolutePathToBpmn = (String)System.getProperty("org.jboss.processFlow.absolutePathToBpmn");

            pString = (String)properties.get("org.jboss.processFlow.sleepTimeMillis");
            if(pString != null)
                sleepTimeMillis = Integer.parseInt(pString);

            pString = (String)properties.get("org.jboss.processFlow.rampUpInterval");
            if(pString != null)
                rampUpInterval = Integer.parseInt(pString);

            hornetqRemotingPort = Integer.parseInt(properties.getProperty("remote.connection.hornetq.port"));
            pfpCoreHostName = properties.getProperty("org.jboss.processFlow.pfp.core.host.name");
            processId = properties.getProperty("org.jboss.processFlow.asyncKService.processId");
            signalKey = properties.getProperty("org.jboss.processFlow.asyncKService.signalKey");

            if(properties.get("org.jboss.processFlow.isPersistent") != null)
                isPersistent = Boolean.parseBoolean(System.getProperty("org.jboss.processFlow.isPersistent"));

            cFactoryName = properties.getProperty("org.jboss.processFlow.connectionFactoryName");
            gwDObjName = properties.getProperty("org.jboss.processFlow.ksession.queue");

            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append("\n\tclientCount = "+clientCount);
            sBuilder.append("\n\tmessagesPerClient = "+requestsPerClient);
            sBuilder.append("\n\tbyteMessageSize = "+byteMessageSize);
            sBuilder.append("\n\tisPersistent = "+isPersistent);
            sBuilder.append("\n\tgateway destination = "+gwDObjName);
            sBuilder.append("\n\tresponse destination = "+responseQueueName);
            sBuilder.append("\n\tcompletionQueue = "+completionQueueName);
            sBuilder.append("\n\tsleepTime = "+sleepTimeMillis);
            sBuilder.append("\n\tlogTrnxResult = "+logTrnxResult);
            sBuilder.append("\n\tblockForTrnxResponse = "+blockForTrnxResponse);
            sBuilder.append("\n\tuseAgent = "+useAgent);
            sBuilder.append("\n\thornetqRemotingPort = "+hornetqRemotingPort);
            sBuilder.append("\n\tconfigurable.pfp.core.host.name = "+pfpCoreHostName);
            sBuilder.append("\n\tprocessId = "+processId);
            sBuilder.append("\n\tsignalKey = "+signalKey);
            sBuilder.append("\n\tcFactoryName = "+cFactoryName);
            sBuilder.append("\n\tgwDObjName = "+gwDObjName);
            log.info(sBuilder.toString());
            
            Properties env = new Properties();
            env.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            Context jndiContext = new InitialContext(env);
            taskServiceProxy = (ITaskService)jndiContext.lookup(ITaskService.TASK_SERVICE_JNDI);
            jndiContext.close();

            env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            env.put(Context.PROVIDER_URL, "remote://"+pfpCoreHostName+":"+hornetqRemotingPort);
            jndiContext = new InitialContext(env);
            ConnectionFactory cFactory = (ConnectionFactory)jndiContext.lookup(cFactoryName);
            gwDObj = (Destination)jndiContext.lookup(gwDObjName);
            jndiContext.close();

            //session objects have all of the load-balancing and fail-over magic .... only need one Connection object
            connectionObj = cFactory.createConnection();
            connectionObj.setExceptionListener(new ExceptionListener() {
                public void onException(final JMSException e) {
                    log.error("JMSException = "+e.getLocalizedMessage());
                }
            });
            connectionObj.start();

            if(!useAgent) {
                File bpmnFile = new File(absolutePathToBpmn);
                if(!bpmnFile.exists())
                    throw new RuntimeException("main() the following bpmn file does not exist: "+absolutePathToBpmn);

                Session sessionObj = null;
                MessageProducer m_sender = null;
                try {
                    sessionObj = connectionObj.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    m_sender = sessionObj.createProducer(gwDObj);
                    ObjectMessage oMessage = sessionObj.createObjectMessage(bpmnFile);
                    oMessage.setStringProperty(IKnowledgeSession.OPERATION_TYPE, IKnowledgeSession.ADD_PROCESS_TO_KNOWLEDGE_BASE);
                    m_sender.send(oMessage);
                    log.info("main() just sent message to async kservice to add process to kbase");
                }finally {
                    if(sessionObj != null)
                        sessionObj.close();
                }
                // sleep for a second to give knowledgeBase time to build
                Thread.sleep(5000);
            }


            execObj = Executors.newFixedThreadPool(clientCount);
            for(int t=1; t <= clientCount; t++) {
                Runnable threadObj = new JMSClient(t);
                execObj.execute(threadObj);
            }

            execObj.shutdown();
            execObj.awaitTermination(1200, TimeUnit.MINUTES);
            log.info("main() all tasks completed on ExecutorService .... server node counts as follows : ");
            Iterator nodeIterator = serverNodeCountHash.keySet().iterator();
            while(nodeIterator.hasNext()) {
                String nodeId = (String)nodeIterator.next();
                log.info("\t"+nodeId+"\t"+serverNodeCountHash.get(nodeId));
            }

        } catch(Throwable x) {
            x.printStackTrace();
        } finally {
            //log.info("main() about to close jms connection = "+connectionObj);
            try {
                if(connectionObj != null)
                    connectionObj.close();
            } catch(Exception x) {
                x.printStackTrace();
            }
        }
    }

    private synchronized static int computeTotal(int x) {
        totalCount = totalCount + x;
        return totalCount;
    }

    static class JMSClient implements Runnable, javax.jms.MessageListener {
        private int id = 0;
        int counter = 0;
        long threadStart = 0L;
        boolean keepWaiting = true;
        Destination tempQueue = null;
 
        public JMSClient(int id) {
            this.id = id;
        }

        public void onMessage(Message responseMessage) {
            int tCount = computeTotal(counter);
            log.info("THREAD_COMPLETE! "+id+" "+tCount+" "+((System.currentTimeMillis() - threadStart)/1000));
            keepWaiting = false;
        }

        public void run() {
            Session sessionObj = null;
            MessageProducer m_sender = null;
            MessageConsumer m_consumer = null;
            try {
                /*
                    -- client side load balancing :  sessions created from a single JMS Connection load-balance across different nodes of the cluster
                    -- compare with 'symmetric-cluster' example where a connection object is being instantiated for each node in cluster
                */
                sessionObj = connectionObj.createSession(false, Session.AUTO_ACKNOWLEDGE);
                tempQueue = sessionObj.createTemporaryQueue();
                m_sender = sessionObj.createProducer(gwDObj);
                m_consumer = sessionObj.createConsumer(tempQueue);
                
                // in this particular use-case, not using either a unique Id nor timestamp on the message
                m_sender.setDisableMessageID(true);
                m_sender.setDisableMessageTimestamp(true);

                if(isPersistent)
                    m_sender.setDeliveryMode(DeliveryMode.PERSISTENT);
                else
                    m_sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                threadStart = System.currentTimeMillis();
                String response = null;
                for(counter = 0; counter < requestsPerClient; counter++) {
                    long originalTime = System.currentTimeMillis();
                    ObjectMessage oMessage = sessionObj.createObjectMessage();
                    oMessage.setStringProperty(IKnowledgeSession.OPERATION_TYPE, IKnowledgeSession.START_PROCESS_AND_RETURN_ID);
                    oMessage.setStringProperty(IKnowledgeSession.PROCESS_ID, processId);
                    oMessage.setJMSReplyTo(tempQueue);
                    oMessage.setJMSCorrelationID(createRandomString());
                    m_sender.send(oMessage);
                    if(logTrnxResult)
                        log.info("sendMessages() counter = "+counter);
                    else if(counter > 0 && (counter % 100) == 0)
                        log.info("sendMessages() counter = "+counter);

                    ObjectMessage replyMessage = (ObjectMessage)m_consumer.receive(60000); //blocks for max 60 seconds
                    if(replyMessage != null) {
                        if(logTrnxResult) {
                            long duration = System.currentTimeMillis() - originalTime;
                            log.info("TRNX_COMPLETE!\t"+System.currentTimeMillis()+"\t"+id+"\t"+counter+"\t"+replyMessage.getStringProperty(IKnowledgeSession.NODE_ID)+"\t"+duration);
                        }
                        String nodeId = replyMessage.getStringProperty(IKnowledgeSession.NODE_ID);
                        if(nodeId != null) {
                            serverNodeCountHash.putIfAbsent(nodeId, new AtomicInteger(0));
                            serverNodeCountHash.get(nodeId).incrementAndGet();
                        } else {
                            log.warn("run() reply message does not include property : "+IKnowledgeSession.NODE_ID);
                        }
                    } else {
                        response = "OH-NO! messageCount = "+counter+"    :    subscriberId = "+id+"    :    replyMessage = null";
                        break;
                    }
                    Map<String, Object> returnMap = (Map<String,Object>)replyMessage.getObject();
                    Long processInstanceId = (Long)returnMap.get(IKnowledgeSession.PROCESS_INSTANCE_ID);
                    Integer ksessionId = (Integer)returnMap.get(IKnowledgeSession.KSESSION_ID);

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

                    if(ITaskService.FAIL_TASK_SIGNAL.equals(signalKey)) {
                        taskServiceProxy.startTask(taskId, "admin");
                    }
                    
                    oMessage = sessionObj.createObjectMessage(changeDetails);
                    oMessage.setStringProperty(IKnowledgeSession.OPERATION_TYPE, IKnowledgeSession.SIGNAL_EVENT);
                    oMessage.setStringProperty(IKnowledgeSession.SIGNAL_TYPE, signalKey);
                    oMessage.setLongProperty(IKnowledgeSession.PROCESS_INSTANCE_ID, processInstanceId);
                    m_sender.send(oMessage);
                        
                    Thread.sleep(sleepTimeMillis );
                }

                int tCount = computeTotal(counter);
                response = "THREAD_COMPLETE!\t"+id+"\t"+tCount+"\t "+((System.currentTimeMillis() - threadStart)/1000);
                log.info(response);
        
            } catch(Throwable x) {
                log.info("run() id  = "+id+"    :    Throwable = "+x);
                x.printStackTrace();
            } finally {
                try {
                    if(sessionObj != null) {
                        sessionObj.close();
                    }
                } catch(Exception x) {
                    x.printStackTrace();
                }
            }
        }
    }
    
    private static String createRandomString() {
        Random random = new Random(System.currentTimeMillis());
        long randomLong = random.nextLong();
        return Long.toHexString(randomLong);
    }
}
