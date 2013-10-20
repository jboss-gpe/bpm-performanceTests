package org.acme.insurance;

import java.io.Serializable;

public class Driver
        implements Serializable
{

    public Driver()
    {
        age = new Integer(0);
        numberOfAccidents = new Integer(0);
        numberOfTickets = new Integer(0);
        creditScore = new Integer(0);
    }

    public String toString()
    {
       StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("\n\tDriver\n\t\tdriverName = ");
        sBuilder.append(getDriverName());
        sBuilder.append("\n\t\tage = ");
        sBuilder.append(getAge());
        sBuilder.append("\n\t\tssn = ");
        sBuilder.append(getSsn());
        sBuilder.append("\n\t\tdlNumber = ");
        sBuilder.append(getDlNumber());
        sBuilder.append("\n\t\tnumberOfAccidents = ");
        sBuilder.append(getNumberOfAccidents());
        sBuilder.append("\n\t\tnumberOfTickets = ");
        sBuilder.append(getNumberOfTickets());
        sBuilder.append("\n\t\tcreditScore = ");
        sBuilder.append(getCreditScore());
        return sBuilder.toString();
    }

    public String getDriverName()
    {
        return driverName;
    }

    public void setDriverName(String driverName)
    {
        this.driverName = driverName;
    }

    public Integer getAge()
    {
        return age;
    }

    public void setAge(Integer age)
    {
        this.age = age;
    }

    public String getSsn()
    {
        return ssn;
    }

    public void setSsn(String ssn)
    {
        this.ssn = ssn;
    }

    public String getDlNumber()
    {
        return dlNumber;
    }

    public void setDlNumber(String dlNumber)
    {
        this.dlNumber = dlNumber;
    }

    public Integer getNumberOfAccidents()
    {
        return numberOfAccidents;
    }

    public void setNumberOfAccidents(Integer numberOfAccidents)
    {
        this.numberOfAccidents = numberOfAccidents;
    }

    public Integer getNumberOfTickets()
    {
        return numberOfTickets;
    }

    public void setNumberOfTickets(Integer numberOfTickets)
    {
        this.numberOfTickets = numberOfTickets;
    }

    public Integer getCreditScore()
    {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore)
    {
        this.creditScore = creditScore;
    }

    private static final long serialVersionUID = 1L;
    private String driverName;
    private Integer age;
    private String ssn;
    private String dlNumber;
    private Integer numberOfAccidents;
    private Integer numberOfTickets;
    private Integer creditScore;
}
