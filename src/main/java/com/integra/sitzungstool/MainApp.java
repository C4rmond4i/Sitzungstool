package com.integra.sitzungstool;

import com.integra.sitzungstool.controller.MainViewController;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class MainApp extends Application
{
    @Override
    public void start(Stage mainStage) throws Exception
    {
        //Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));
        
        
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/MainViewRessort.fxml"));
	Parent geparsteFxmlFile = loader.load();
	Scene mainScene = new Scene(geparsteFxmlFile);
	mainStage.setScene(mainScene);
		
        MainViewController mvc = (MainViewController) loader.getController();
	mvc.init();
		
	mainStage.show();
                
    }
    
    public static void main(String[] args)
    {
        launch(args);
    }
}