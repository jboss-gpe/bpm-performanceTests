package com.redhat.gpe.approve;

import javax.jws.WebService;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@WebService(targetNamespace="urn:com.redhat.gpe.approve:1.0", serviceName="Approve", portName="ApprovePort")
public class Approve implements IApprove {
    
    Logger log = LoggerFactory.getLogger("Approve");

    public String approveQuote(String payload) {
        log.info("approveQuote() payload = "+payload);
        return payload;
    }
}
