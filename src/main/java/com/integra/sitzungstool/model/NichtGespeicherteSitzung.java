package com.integra.sitzungstool.model;

public class NichtGespeicherteSitzung {
    private String id;
    private String[] benutzerkennungen;
    
    public NichtGespeicherteSitzung (String id, String[] benutzerkennungen) {
        this.id = id;
        this.benutzerkennungen = benutzerkennungen;
    }
    
    public String getId() {
        return this.id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String[] getBenutzerkennungen() {
        return this.benutzerkennungen;
    }
    
    public void setBenutzerkennungen(String[] benutzerkennungen) {
        this.benutzerkennungen = benutzerkennungen;
    }
}
