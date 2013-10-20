package com.example.workItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.process.instance.WorkItemHandler;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;

import org.apache.log4j.Logger;

public class CreateBatchedCollections extends BaseWorkItemHandler implements WorkItemHandler {

    Logger log = Logger.getLogger("CreateBatchedCollections");

    public CreateBatchedCollections() {
        // make sure whatever is included in this constructor is performant!!
        // this constructor is instantiated with every ksession re-load
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        int batchSize = Integer.parseInt((String)workItem.getParameter(BATCH_SIZE));
        log.info("executeWorkItem() batchSize = "+batchSize);

        List<Object> unbatchedCollection = (List<Object>)workItem.getParameter(UNBATCHED_COLLECTION);
        
        Collection<List<Object>> lists = new ArrayList<List<Object>>((unbatchedCollection.size() / batchSize) + 1);
        for (int i = 0; i < unbatchedCollection.size(); i += batchSize) {
        	List<Object> subList = unbatchedCollection.subList(i, Math.min(i + batchSize, unbatchedCollection.size()));
        	
        	//need to create a serializableList from the non-serializable subList otherwise process instance
        	//can not be persisted in the database
        	List<Object> serializableList = new ArrayList<Object>(subList);
            lists.add(serializableList);
        }

        Map<String, Object> resultsMap = new HashMap<String, Object>();
        resultsMap.put(QUANTITY_TO_PROCESS, unbatchedCollection.size());
        resultsMap.put(BATCHED_COLLECTION, lists);
        resultsMap.put(BATCH_SIZE, batchSize);
        manager.completeWorkItem(workItem.getId(), resultsMap);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
