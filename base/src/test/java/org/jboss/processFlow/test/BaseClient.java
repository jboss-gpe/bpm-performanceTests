package org.jboss.processFlow.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import org.apache.log4j.Logger;

import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.test.SyncClientTest;

public abstract class BaseClient {

    protected IKnowledgeSession kSessionProxy = null;
    protected ITaskService taskServiceProxy = null;
    protected String nodeId;
    protected int counter = 0;
    protected long threadStart= 0L;
    protected String processId;
    protected Properties properties;

    public BaseClient(Integer id) throws IOException {
        this.nodeId = id.toString();
        properties = new Properties();
        properties.load(BaseClient.class.getResourceAsStream(SyncClientTest.PROPERTIES_FILE_NAME));
        if(properties.size() == 0)
            throw new RuntimeException("start() no properties defined in "+SyncClientTest.PROPERTIES_FILE_NAME);

        kSessionProxy = SyncClientTest.getKnowledgeSessionProxy();
        taskServiceProxy = SyncClientTest.getTaskServiceProxy();
    }
}
