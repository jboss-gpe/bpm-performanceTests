== object marshalling

=== objectives:
* demonstrate use of JPAPlaceholderResolverStrategy ObjectMarshallingStrategy as per:  org.drools.persistence.jpa.marshaller.JPAPlaceholderResolverStrategy

=== pre-reqs
* JBoss EAP 6.1.1 environment provisioned with BRMS5.3.1 via the master branch of Process Flow Provision

=== set-up:
* modify the following in PFP and re-provision:
. add the following dependency in pfpServices/knowledgeSessionService/src/main/resources/META-INF/jboss-deployment-structure.xml
** <module name="com.redhat.gpe.shared" annotations="true" />
        
. add the following JPA entity class in pfpServices/knowledgeSessionService/src/main/resources/META-INF/persistence.xml
** <class>com.redhat.gpe.domain.Customer</class>

[NOTE] adding the above to stock PFP will initially break pfp-core-0 runtime.  Once this objectMarshalling project, dependency related issues will be corrected

. from root directory of this project, execute:   ant
. review this project's build.xml to better understand what the default ant target does
. also review and edit as appropriate this project's build.properties 

=== testing:
* from this root project directory, execute:   ant test
* using eclipse remote debugger, place a breakpoint in the unmarshal(...) function to determine whether the JPA find(...) operation works
