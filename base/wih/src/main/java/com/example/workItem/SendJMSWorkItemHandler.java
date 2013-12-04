package com.example.workItem;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.Context;

import org.drools.process.instance.WorkItemHandler;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.util.MessagingUtil;

import org.apache.log4j.Logger;

public class SendJMSWorkItemHandler implements WorkItemHandler {

    private static final String gwDObjName = "processFlow.testQueue";
    private static Logger log = Logger.getLogger("SendJMSWorkItemHandler");
    private static Connection connectionObj = null;
    private static Destination gwDObj = null;
    
    static {
        try {
            ConnectionFactory cFactory = MessagingUtil.grabConnectionFactory();
            connectionObj = cFactory.createConnection();
            gwDObj = (Destination)MessagingUtil.grabDestination(gwDObjName);
        }catch(Exception x){
            throw new RuntimeException(x);
        }
    }

    public SendJMSWorkItemHandler() {
        // make sure whatever is included in this constructor is performant!!
        // this constructor is instantiated with every ksession re-load
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String taskClientIdIn = (String)workItem.getParameter("taskClientIdIn");
        log.info("executeWorkItem() will not execute completeWorkItem ...instead will send JMS message with clientId = "+taskClientIdIn);
        Session sessionObj = null;
        try {
            sessionObj = connectionObj.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer m_sender = sessionObj.createProducer(gwDObj);
            TextMessage tMessage = sessionObj.createTextMessage();
            tMessage.setStringProperty("clientId", taskClientIdIn);
            tMessage.setObjectProperty(IKnowledgeSession.WORK_ITEM_ID, workItem.getId());
            tMessage.setLongProperty(IKnowledgeSession.PROCESS_INSTANCE_ID, workItem.getProcessInstanceId());
            m_sender.send(tMessage);
        } catch(JMSException x) {
            throw new RuntimeException(x);
        }finally {
            if(sessionObj != null) {
                try { sessionObj.close(); }catch(Exception x){ x.printStackTrace(); }
            }
        }
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
