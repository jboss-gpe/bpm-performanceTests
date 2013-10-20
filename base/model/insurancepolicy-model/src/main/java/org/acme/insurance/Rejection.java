package org.acme.insurance;


public class Rejection
{

    public Rejection()
    {
    }

    public Rejection(String reason)
    {
        this.reason = reason;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    private String reason;
}
