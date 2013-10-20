task_terminate_by_signalIntermediateEvent.bpmn2

USAGE:
    0)  ensure a package of "org.jboss.processFlow" has been created in Guvnor and is enabled in "guvnor.packages" property of $PFP_HOME/build.properties
    1)  import signalTaskChangeDetails/src/test/resources/task_terminate_by_signalIntermediateEvent.bpmn2 in guvnor
    2)  build "org.jboss.processFlow" package in Guvnor
    3)  edit properties at top of signalTaskChangeDetails/build.xml
    4)  execute:   ant signalTaskChangeDetailsTest


NOTES:
    - uses a BPMN2 "intermediateCatchEvent" to interrupt a subprocess that is in a wait state (in particular at a embedded human task)
    - via a signalKey of "floatingSignal" and a configurable signalValue, will route to one of two branches that execute a script task
    - with either branch, process instance along with subprocess are completed
    - puts task in status of:  "Obsolete"
    - NOTE:  "SignalRef" property of intermediateCatchEvent must match "signalType" of IKnowledgeSessionService.signalEvent(...)
    - the following is example log statements of what should occur when the SignalIntermediateEvent is signalled :

    13:30:54,405 INFO  [KnowledgeSessionService] signalEvent() 
        ksession = org.drools.command.impl.CommandBasedStatefulKnowledgeSession@c51ad4a
        processInstanceId = 5
        signalType=floatingSignal
        signalValue=branchB
    13:30:54,461 INFO  [STDOUT] branch B has been signaled
    13:30:54,617 INFO  [KnowledgeSessionService] afterProcessCompleted()    sessionId :  3 : WorkflowProcessInstance5 [processId=org.jboss.processFlow.floatingSignalEvent_on_subprocess,state=2] : session to be reused

