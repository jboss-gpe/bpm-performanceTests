package com.redhat.gpe.persistenceService;

import com.redhat.gpe.domain.Customer;

public interface IPersistenceService{

    // returns id of persisted object
    long persist(Customer cObj);
}
