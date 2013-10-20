package com.redhat.gpe.springService;

import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.redhat.gpe.persistenceService.IPersistenceService;
import com.redhat.gpe.domain.Customer;

@Local(IPersistenceService.class)
@Singleton(name="springService")
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SpringService implements IPersistenceService {

    private static Logger log = LoggerFactory.getLogger("SpringService");
    private static JdbcTemplate jdbcTemplate;
    private static Object cpLock = new Object();
    private static AtomicInteger counter = new AtomicInteger();

    private @Resource(name="java:/TransactionManager") TransactionManager tMgr;

    @PostConstruct
    public void start() {
        if(jdbcTemplate == null){
            try {
                if(jdbcTemplate != null)
                    return;
    
                Context jContext = new InitialContext();
                DataSource testCP = (DataSource)jContext.lookup("java:jboss/datasources/test-cp-xa");
                jdbcTemplate = new JdbcTemplate(testCP);
                log.info("start() testCP = "+testCP);
            }catch(Exception x) {
                throw new RuntimeException(x);
            }
        }
    }

    public int persist() {
        try {
            log.info("persist() trnx status = "+tMgr.getStatus());
            jdbcTemplate.update("INSERT INTO customer(id, firstname, lastname) values(?,?,?)", counter.getAndIncrement(), "Azra and Alex", "Bride");
            return 1;
        }catch(Throwable x) {
            x.printStackTrace();
            return 0;
        }
    }

    @PreDestroy
    public void stop() {
        log.info("stop()");
    }
}
