package com.integra.sitzungstool.model;

import javafx.scene.image.Image;

public class Integraner
{
	private String benutzerkennung;
	private Image bild;
	private String name;
	private String ressort;
	private boolean anwesend;
	
	public Integraner(String benutzerkennung, Image bild, String name, String ressort)
	{
		this.benutzerkennung = benutzerkennung;
		this.bild = bild;
		this.name = name;
		this.ressort = ressort;
		
		this.anwesend = false;
	}
	
	public String toString()
	{
		return name;
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
		return this.name;
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

	
	public boolean isAnwesend()
	{
		return this.anwesend;
	}

	public void setAnwesend(boolean anwesend)
	{
		this.anwesend = anwesend;
	}
	
	
}