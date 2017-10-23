package com.integra.sitzungstool.model;

import java.util.Arrays;
import org.apache.commons.lang3.text.WordUtils;

public class Integraner {
    private String id;
    private String ressort;
    private String stab;
    private boolean anwesend = false;
    
    public String getId() {
        return this.id;
    }
    
    public String getName() {
        /* String[] names = this.id.split(".");
        String name = names[0] + " " + names[1];
        return WordUtils.capitalize(name); */
        return this.id;
    }
    
    public String getRessort() {
        return this.ressort;
    }
    
    public boolean getAnwesend() {
        return this.anwesend;
    }
    
    public void setAnwesend(boolean anwesend) {
        this.anwesend = anwesend;
    }
}
