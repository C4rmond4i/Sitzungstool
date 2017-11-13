package com.integra.sitzungstool.model;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class Sitzung
{
    private String datumString;
    private String id;
    private GregorianCalendar datum;
    
    public Sitzung(String id, String datumString)
    {
        this.datumString = datumString;
        this.id = id;
        String[] datumStringTeile = datumString.split("-");
        int[] datumsTeile = new int[3];
        for (int i = 0; i < datumStringTeile.length; i++) {
            datumsTeile[i] = Integer.parseInt(datumStringTeile[i]);
        }
        this.datum = new GregorianCalendar(datumsTeile[0], datumsTeile[1] - 1, datumsTeile[2]);
    }
    
    public String getId() {
        return this.id;
    }
    
    @Override
    public String toString()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd'.'MM'.'YYYY");
        return "MoSi " + sdf.format(this.datum.getTime());
    }
}
