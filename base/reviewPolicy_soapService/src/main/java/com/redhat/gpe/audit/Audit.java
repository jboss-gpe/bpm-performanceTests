package com.redhat.gpe.audit;

import javax.jws.WebService;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@WebService(targetNamespace="urn:com.redhat.gpe.audit:1.0", serviceName="Audit", portName="AuditPort")
public class Audit implements IAudit {
    
    Logger log = LoggerFactory.getLogger("Audit");

    public String auditQuote(String payload) {
        log.info("auditQuote() payload = "+payload);
        return payload;
    }
}
