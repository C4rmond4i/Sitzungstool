package com.integra.sitzungstool.model;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class Sitzung
{
    private String datumString;
    private String id;
    private String semester;
    private GregorianCalendar datum;
    
    public Sitzung(String id, String datumString, String semester)
    {
        this.datumString = datumString;
        this.semester = semester;
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
    
    public String getDatumString() {
        return this.datumString;
    }
    
    public String getSemester() {
        return this.semester;
    }
    
    @Override
    public String toString()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd'.'MM'.'YYYY");
        return "MoSi " + sdf.format(this.datum.getTime());
    }
}
