package com.integra.sitzungstool.model;

import java.io.UnsupportedEncodingException;
import javafx.scene.image.Image;

public class Integraner
{
	private String benutzerkennung;
	private Image bild;
	private String name;
	private String ressort;
        private String stab;
        private String bildHash;
	private boolean anwesend;
        
        public Integraner(String benutzerkennung) {
            this.benutzerkennung = benutzerkennung;
            this.anwesend = false;
        }
	
	public Integraner(String benutzerkennung, Image bild, String name, String ressort, String stab)
	{
		this.benutzerkennung = benutzerkennung;
		this.bild = bild;
		this.name = name;
		this.ressort = ressort;
                this.stab = stab;
		
		this.anwesend = false;
	}
	
        @Override
	public String toString()
	{
		return this.getName();
	}

	public String getBenutzerkennung()
	{
		return this.benutzerkennung;
	}

	public void setBenutzerkennung(String benutzerkennung)
	{
		this.benutzerkennung = benutzerkennung;
	}

	public Image getBild()
	{
		return this.bild;
	}

	public void setBild(Image bild)
	{
		this.bild = bild;
	}

	public String getName()
	{
            try {
                return java.net.URLDecoder.decode(this.name, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                System.out.println(ex.getMessage());
                return this.name.replace("+", " ");
            }
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getRessort()
	{
		return this.ressort;
	}

	public void setRessort(String ressort)
	{
		this.ressort = ressort;
	}
        
        public String getStab()
	{
		return this.stab;
	}

	public void setStab(String stab)
	{
		this.stab = stab;
	}

	public String getBildHash() {
                return this.bildHash;
        }
        
        public void setBildHash(String bildHash) {
                this.bildHash = bildHash;
        }
        
	public boolean isAnwesend()
	{
		return this.anwesend;
	}

	public void setAnwesend(boolean anwesend)
	{
		this.anwesend = anwesend;
	}
	
	
}
