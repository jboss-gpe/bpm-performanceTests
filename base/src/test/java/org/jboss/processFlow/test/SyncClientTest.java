package org.jboss.processFlow.test;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.naming.InitialContext;
import javax.naming.Context;

import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;

import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.Logger;

/**
 *  SyncClientTest
 *  JA Bride
 *  17 March, 2011
 */
public class SyncClientTest {

    public static final String PROPERTIES_FILE_NAME="/jbpm-performanceTests.properties";
    public static final String PATH_TO_LOG4J_CONFIG = "path.to.log4j.config";
    private static final String ID = "clientId";
    private static final String BIND_ADDRESS = "bindAddress";
    private static final String FINAL_MESSAGE = "FINAL_MESSAGE";
    private static final String COMMA = ",";
    private static Logger log = Logger.getLogger(SyncClientTest.class);

    public static int clientCount = 0;
    public static int requestsPerClient = 0;
    public static int rampUpInterval = 0;
    public static boolean enableLog = true;
    public static int totalCount = 0;
    public static long totalDuration = 0;
    public static boolean startNewProcessInstance = true;
    public static int timesInvoked = 1;
    public static String clientHandlerName;
    public static ExecutorService execObj = null;
    public static int debuggerPort = 0;
    public static String jmxPort = "1099";
    public static final ConcurrentMap<String, AtomicInteger> serverNodeCountHash = new ConcurrentHashMap<String, AtomicInteger>();
    public static BigDecimal bdThousand;
    public static Context jndiContext = null;
    public static ITaskService taskServiceProxy = null;
    public static IKnowledgeSession ksessionProxy = null;
    public static Set checkDupsSet = new CopyOnWriteArraySet();
    public static int sleepTimeMillis = 0;
    public static String absolutePathToBpmn;
    public static boolean includeProcessVariables;
    public static boolean abortProcessInstance = false;
    public static boolean useAgent=true;
    public static String userId;
    public static String groupId;
    public static boolean useGuaranteedClaimApproach = true;

    public static void main(String args[]) {
        Properties properties = new Properties();
        try {
            bdThousand = new BigDecimal(1000);

            properties.load(SyncClientTest.class.getResourceAsStream(PROPERTIES_FILE_NAME));
            if(properties.size() == 0)
                throw new RuntimeException("start() no properties defined in "+PROPERTIES_FILE_NAME);

            String pathToLog4jConfig = (String)properties.getProperty(SyncClientTest.PATH_TO_LOG4J_CONFIG);
            DOMConfigurator.configure(pathToLog4jConfig);

            if(System.getProperty("org.jboss.processFlow.handlerImplementation") == null)
                throw new Exception("main() must define a value for 'org.jboss.processFlow.handlerImplementation'");
            else {
                clientHandlerName = System.getProperty("org.jboss.processFlow.handlerImplementation");
            }

            includeProcessVariables = Boolean.parseBoolean((String)properties.getProperty("org.jboss.processFlow.includeProcessVariables", "TRUE"));
            useAgent = Boolean.parseBoolean((String)properties.getProperty("org.jboss.processFlow.useKnowledgeAgent", "TRUE"));
            abortProcessInstance = Boolean.parseBoolean((String)properties.getProperty("org.jboss.processFlow.abortProcessInstance", "TRUE"));
            enableLog = Boolean.parseBoolean((String)properties.getProperty("org.jboss.processFlow.enableLog", "TRUE"));
            startNewProcessInstance = Boolean.parseBoolean((String)properties.getProperty("org.jboss.processFlow.startNewProcessInstance", "TRUE"));
            useGuaranteedClaimApproach = Boolean.parseBoolean((String)properties.getProperty("org.jboss.processFlow.useGuaranteedClaimApproach", "TRUE"));
            debuggerPort = Integer.parseInt((String)properties.getProperty("org.jboss.processFlow.testClient.debuggerPort", "0"));
            clientCount = Integer.parseInt((String)properties.getProperty("org.jboss.processFlow.clientCount", "1"));
            requestsPerClient = Integer.parseInt((String)properties.getProperty("org.jboss.processFlow.requestsPerClient", "0"));
            absolutePathToBpmn = (String)System.getProperty("org.jboss.processFlow.absolutePathToBpmn", "populateMe");
            sleepTimeMillis = Integer.parseInt((String)properties.getProperty("org.jboss.processFlow.sleepTimeMillis", "0"));
            rampUpInterval = Integer.parseInt((String)properties.getProperty("org.jboss.processFlow.rampUpInterval", "0"));
            userId = (String)properties.getProperty("org.jboss.processFlow.performanceTests.userId");
            groupId = (String)properties.getProperty("org.jboss.processFlow.performanceTests.groupId");

            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append("\n\tclientHandler = "+clientHandlerName);
            sBuilder.append("\n\trampUpInterval = "+rampUpInterval);
            sBuilder.append("\n\tincludeProcessVariables = "+includeProcessVariables);
            sBuilder.append("\n\tuseAgent = "+useAgent);
            sBuilder.append("\n\tabortProcessInstance = "+abortProcessInstance);
            sBuilder.append("\n\tenableLog = "+enableLog);
            sBuilder.append("\n\tstartNewProcessInstance = "+startNewProcessInstance);
            sBuilder.append("\n\tuseGuaranteedClaimApproach = "+useGuaranteedClaimApproach);
            sBuilder.append("\n\tdebuggerPort = "+debuggerPort);
            sBuilder.append("\n\tclientCount = "+clientCount);
            sBuilder.append("\n\trequestsPerClient = "+requestsPerClient);
            sBuilder.append("\n\tabsolutePathToBpmn = "+absolutePathToBpmn);
            sBuilder.append("\n\tsleepTimeMillis = "+sleepTimeMillis);
            sBuilder.append("\n\tuserId = "+userId);
            sBuilder.append("\n\tgroupId = "+groupId);
            log.info(sBuilder.toString());

            jndiContext = getInitialContext();
            getKnowledgeSessionProxy();
            getTaskServiceProxy();

            if(!SyncClientTest.useAgent)
                SyncClientTest.addProcessToKnowledgeBase();

            execObj = Executors.newFixedThreadPool(clientCount);

            Class handlerClass = Class.forName(clientHandlerName);
            Class[] classParams = new Class[] {Integer.class};
            for(int t=1; t <= clientCount; t++) {
                Object[] objParams = new Object[] {new Integer(t)};
                Constructor cObj = handlerClass.getConstructor(classParams);
                Runnable siClient = (Runnable)cObj.newInstance(objParams);
                execObj.execute(siClient);
                Thread.sleep(rampUpInterval);
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
            try {
                if(jndiContext != null) {
                    jndiContext.close();
                }
            } catch(Exception x) {
                x.printStackTrace();
            }
        }
    }

    public static synchronized long computeAverageDuration(long x) {
        totalDuration = totalDuration + x;
        long averageDuration = totalDuration / timesInvoked;
        timesInvoked++;
        return averageDuration;
    }

    public static synchronized int computeTotal(int x) {
        totalCount = totalCount + x;
        return totalCount;
    }

    public static synchronized ITaskService getTaskServiceProxy() {
        if(taskServiceProxy != null)
            return taskServiceProxy;

        try {
            taskServiceProxy = (ITaskService)jndiContext.lookup(ITaskService.TASK_SERVICE_JNDI);
            return taskServiceProxy;
        } catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    public static synchronized IKnowledgeSession getKnowledgeSessionProxy() {
        if(ksessionProxy != null)
            return ksessionProxy;

        try {
            ksessionProxy = (IKnowledgeSession)jndiContext.lookup(IKnowledgeSession.KNOWLEDGE_SESSION_SERVICE_JNDI);
            return ksessionProxy;
        } catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    public static void addProcessToKnowledgeBase() {
        File bpmnFile = new File(absolutePathToBpmn);
        if(!bpmnFile.exists())
            throw new RuntimeException("addProcessToKnowledgeBase() the following bpmn file does not exist: "+absolutePathToBpmn);
        IKnowledgeSession ksessionProxy = getKnowledgeSessionProxy();
        ksessionProxy.addProcessToKnowledgeBase(bpmnFile);
        log.info("addProcessToKnowledgeBase() just added the following bpmn to the PFP knowledgebase : "+absolutePathToBpmn);    
    }

    public static synchronized Context getInitialContext() throws Exception {
        if(jndiContext != null)
            return jndiContext;

        Properties jndiProps = new Properties();
        jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        jndiContext = new InitialContext(jndiProps);
        return jndiContext;
    }
}
