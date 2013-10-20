purpose:
  - load test harness focused on execution of processes with a custom work item handler
    registration of workItemhandler is automated 

assumptions:
    - JBoss runtime has been provisioned via PFP with the following properties set  in $PFP_HOME/build.properties
        org.jboss.processFlow.provision.pfpCore=true

usage:
    - review ../build.properties
    - from root of project, execute:    
        ant deploy              :   registers workItemHandler with jbpm5 process engine
        ant                     :   runs the load test
