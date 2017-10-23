package com.integra.sitzungstool.general;

import com.integra.sitzungstool.model.Integraner;
import java.util.ArrayList;
import javafx.scene.image.Image;

public class ServerCommunication
{
        public static boolean vorstandLogin(String kennung, String password)
        {
            return kennung.equals("a") && password.equals("a");
        }
        
	public static ArrayList<Integraner> getIntegranetData()
	{
		ArrayList<Integraner> integraner = new ArrayList<Integraner>();
		
		integraner.add(new Integraner("luca.luethi", new Image("/images/imageLuca.jpg"),"Luca Lüthi", "IT", "keiner"));
		integraner.add(new Integraner("marius.hessenthaler", new Image("/images/imageMariusHe.jpg"),"Marius Hessenthaler", "IT", "keiner"));
		integraner.add(new Integraner("marius.haberstock", new Image("/images/imageMariusHa.jpg"),"Marius Haberstock", "IT", "keiner"));
		integraner.add(new Integraner("gero.becker", new Image("/images/imageGero.jpg"),"Gero Becker", "IT", "keiner"));
	
		return integraner;
	}
	
	public static void saveData()
	{
		
	}
}