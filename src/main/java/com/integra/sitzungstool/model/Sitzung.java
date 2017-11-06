package com.integra.sitzungstool.model;

public class Sitzung
{
    private String datum;
    private String id;
    
    public Sitzung(String datum, String id)
    {
        this.datum = datum;
        this.id = id;
    }
    
    @Override
    public String toString()
    {
        return "MoSi " + datum;
    }
}
