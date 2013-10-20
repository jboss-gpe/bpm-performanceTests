package com.example.workItem;

import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;

public class BaseWIHandler {

    public static final String CLIENT_ID  = "clientId";
    protected boolean enableLog = false;
    protected static IKnowledgeSession kProxy = null;
    private static Object lockObj = new Object();

    public BaseWIHandler() {
        enableLog = Boolean.parseBoolean(System.getProperty("org.jboss.processFlow.enableLog", "TRUE"));

        if(kProxy == null){
            synchronized(lockObj){
                if(kProxy != null)
                    return;

                Context jndiContext = null;
                try {
                    jndiContext = new InitialContext();
                    kProxy = (IKnowledgeSession)jndiContext.lookup((IKnowledgeSession.KNOWLEDGE_SESSION_SERVICE_JNDI));
                } catch(Exception x) {
                        throw new RuntimeException("static()", x);
                }finally {
                    try {
                        if(jndiContext != null)
                            jndiContext.close();
                    }catch(Exception x){
                        x.printStackTrace();
                    }
                }
            }
        }
    }
} 
