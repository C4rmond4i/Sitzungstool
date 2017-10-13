package com.integra.sitzungstool.general;

import java.util.ArrayList;
import javafx.scene.image.Image;
import com.integra.sitzungstool.model.Integraner;

public class ServerCommunication
{
	public static ArrayList<Integraner> getIntegranetData()
	{
		ArrayList<Integraner> integraner = new ArrayList<Integraner>();
		
		integraner.add(new Integraner("luca.luethi", new Image("/images/imageLuca.jpg"),"Luca Lüthi", "IT"));
		integraner.add(new Integraner("marius.hessenthaler", new Image("/images/imageMariusHe.jpg"),"Marius Hessenthaler", "IT"));
		integraner.add(new Integraner("marius.haberstock", new Image("/images/imageMariusHa.jpg"),"Marius Haberstock", "IT"));
		integraner.add(new Integraner("gero.becker", new Image("/images/imageGero.jpg"),"Gero Becker", "IT"));
	
		return integraner;
	}
	
	public static void saveData()
	{
		
	}
}