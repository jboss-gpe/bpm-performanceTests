package org.jboss.processFlow.test;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.tasks.ITaskService;

public class EJBServicesFactory {

    private static Context jndiContext;

    private static IKnowledgeSession ksessionProxy = null;
    private static ITaskService taskServiceProxy = null;

    static {
        try {
            Properties jndiProps = new Properties();
            jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            jndiContext = new InitialContext(jndiProps);
        } catch(Exception x) {
            throw new RuntimeException(x);
        }
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
}
