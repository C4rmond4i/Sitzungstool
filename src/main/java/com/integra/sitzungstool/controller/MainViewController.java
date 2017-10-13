package com.integra.sitzungstool.controller;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.VLineTo;
import javafx.util.Duration;
import com.integra.sitzungstool.model.Integraner;
import com.integra.sitzungstool.general.ServerCommunication;

public class MainViewController
{
	//GUI
	@FXML private ImageView imageViewWebcam;
	
	@FXML private Label labelName;
	@FXML private ImageView imageViewPicture;
	@FXML private Rectangle rectangleScanner;
	
	@FXML private ListView<Integraner> listViewVorstand;
	@FXML private ListView<Integraner> listViewAkquise;
	@FXML private ListView<Integraner> listViewIT;
	@FXML private ListView<Integraner> listViewPersonal;
	@FXML private ListView<Integraner> listViewPR;
	@FXML private ListView<Integraner> listViewQM;

	@FXML private Label labelAmount;
	@FXML private Label labelVorstand;
	@FXML private Label labelAkquise;
	@FXML private Label labelIT;
	@FXML private Label labelPersonal;
	@FXML private Label labelPR;
	@FXML private Label labelQM;
	
	//Logik
	private Thread threadQRScanner;
	private BufferedImage grabbedImage;
	private Webcam webCam = null;
	private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>();
	private BinaryBitmap bitmap;
	private Result result;
	
	//Data
	private ArrayList<Integraner> integraner;

	
	public void init()
	{			
		//Integranet Daten ziehen
		integraner = ServerCommunication.getIntegranetData();
		
		
		//Scanner Animation
		PathTransition pathTransition = new PathTransition();
		
		Path path = new Path();
		path.getElements().add (new MoveTo (100, 5));
		path.getElements().add (new VLineTo (180));
		path.setSmooth(true);
		
		pathTransition.setDuration(Duration.millis(1000));
		pathTransition.setNode(rectangleScanner);
		pathTransition.setPath(path);
		pathTransition.setCycleCount(-1);
		pathTransition.setAutoReverse(true);
		pathTransition.setDelay(Duration.millis(5000));

	        pathTransition.play();
		
		//Webcam inistalisiren
		Task<Void> webCamIntilizer = new Task<Void>()
		{

			@Override
			protected Void call() throws Exception
			{

				if(webCam == null)
				{
					webCam = Webcam.getDefault();
					webCam.setViewSize(WebcamResolution.VGA.getSize());
//					webCam.setViewSize(new Dimension (320, 240));
					webCam.open();
				}
				else
				{
					closeCamera();
					webCam = Webcam.getDefault();
					webCam.open();
				}				
				startWebCamStream();
				return null;
			}

		};
		new Thread(webCamIntilizer).start();
	}
	
	public void useID()
	{
		System.out.println("use id");
	}
	
	protected void startWebCamStream()
	{
		Task<Void> taskVideoUpdate = new Task<Void>()
		{

			@Override
			protected Void call() throws Exception
			{
				while (true)
				{
						if((grabbedImage = webCam.getImage()) != null)
						{
							try
							{
								Thread.sleep(65);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							

							Platform.runLater(new Runnable()
							{

								@Override
								public void run()
								{
									//Video updaten
									final Image mainiamge = SwingFXUtils.toFXImage(grabbedImage, null);
									imageProperty.set(mainiamge);
								}
							});

							grabbedImage.flush();
					}
				}
			}

		};
		Thread threadVideoUpdate = new Thread(taskVideoUpdate);
		threadVideoUpdate.setDaemon(true);
		threadVideoUpdate.start();
		imageViewWebcam.imageProperty().bind(imageProperty);
		
		
		
		Task<Void> taskQRScanner = new Task<Void>()
		{

			@Override
			protected Void call() throws Exception
			{
				while (true)
				{	
					try
					{
						Thread.sleep(500);
					}
					
					catch (Exception e)
					{
						e.printStackTrace();
					}
					
					if((grabbedImage = webCam.getImage()) != null)
					{
						//Nach QR Suchen
						bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(grabbedImage)));
						Platform.runLater(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									result = new MultiFormatReader().decode(bitmap);
									loginUser(result.getText());
									result = null;
								}
								catch (NotFoundException e)
								{
									// fall thru, it means there is no QR code in image
								}
							}
						});
					}
				}
			}

		};
		threadQRScanner = new Thread(taskQRScanner);
		threadQRScanner.setDaemon(true);
		threadQRScanner.start();	
	}

	private void closeCamera()
	{
		if(webCam != null)
		{
			webCam.close();
		}
	}
	
	public void loginUser(String id)
	{
		for(Integraner i : integraner)
		{
			if(i.getBenutzerkennung().equals(id))
			{
				if(i.isAnwesend())
				{
					labelName.setText("Bereits eingeloggt");
				}
				else
				{
					labelName.setText(i.getName());
					imageViewPicture.setImage(i.getBild());
					i.setAnwesend(true);

					
					switch(i.getRessort())
					{
						case "IT":		listViewIT.getItems().add(i);
									labelIT.setText("Ressort IT (" + listViewIT.getItems().size() + ")");
									break;

					}
					
					
					labelAmount.setText("(" + listViewIT.getItems().size() + ")");
				}
			}
		}
	}
}