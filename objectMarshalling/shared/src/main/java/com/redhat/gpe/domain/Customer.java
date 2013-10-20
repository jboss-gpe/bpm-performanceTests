package com.redhat.gpe.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Customer implements java.io.Serializable {

    @Id
    private long id;
    private String firstName;
    private String lastName;

    private Customer() {}

    public Customer(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public long getId() { return id;    }
    public String getFirstName() { return firstName;    }
    public String getLastName() { return lastName;  }

    public void setId(long x) { id = x; }
    public void setFirstName(String x) { firstName = x; }
    public void setLastName(String x) { lastName = x; }

    @Override
    public String toString() {
        return String.format(
                "Customer[id=%d, firstName='%s', lastName='%s']",
                id, firstName, lastName);
    }

}
