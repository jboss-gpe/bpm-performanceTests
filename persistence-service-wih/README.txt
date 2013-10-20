persistence-service-wih

objectives:
  - demonstrate use of BRMS custom workItem handler deployed as a static JBoss Module
    - custom workItemHandler class is visible to org.mvel static JBoss module
    - mvel parses drools.session.template config file & instantiates custom workItemHandler classes
  - demonstrate deployment of Spring libraries as static JBoss modules
  - demonstrate invocation of local EJB singleton services that implement database persistence via either standard JPA or spring
    - specifically, a BRMS5.3.1 custom WorkItemHandler invokes the co-located persistence service.
    - this occurs within the scope of the BPM engine's transaction

pre-reqs
  - JBoss EAP 6.1.1 environment provisioned with BRMS5.3.1 via the master branch of Process Flow Provision

set-up:
  - from root directory of this project, execute:   ant
  - review this project's build.xml to better understand what the default ant target does
  - also review and edit as appropriate this project's build.properties 

testing:
  - from this root project directory, execute:   ant test
  - the java test client included in this project will 
    1)  register the a BPMN2 process definition called "customWorkItemHandler" with the remote PFP knowledgeSessionService
        a graphical depiction of the "customWorkItemHandler" process definition can be found in this project at:
            jbpm-performanceTests/base/src/test/resources/img/customWorkItemHandler.png
    2)  smoke test the "customWorkItemHandler" process by invoking the startProcessAndReturnId function of the remote PFP knowledgeSessionService
    
            
