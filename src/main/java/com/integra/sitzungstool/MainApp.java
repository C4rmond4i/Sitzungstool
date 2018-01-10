package com.integra.sitzungstool;

import com.integra.sitzungstool.controller.MainViewController;
import com.integra.sitzungstool.general.DataInterface;
import java.util.Optional;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class MainApp extends Application
{
    @Override
    public void start(Stage mainStage) throws Exception
    {   
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/MainViewRessort.fxml"));
	Parent geparsteFxmlFile = loader.load();
	Scene mainScene = new Scene(geparsteFxmlFile);
	mainStage.setScene(mainScene);
        
        //Controller initialisieren       
        MainViewController mvc = (MainViewController) loader.getController();
        DataInterface.setMainViewController(mvc);
	mvc.init();
        
        
        //Wirklich beenden Dialog hinzufügen
        mainStage.setOnCloseRequest((WindowEvent event) ->
        {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.initOwner(mainStage);
                alert.initModality(Modality.WINDOW_MODAL);
                alert.setTitle("Wirklich beenden?");
                alert.setHeaderText("Wirklich beenden?");
                alert.setContentText("Sind Sie sicher, dass Sie das Sitzungstool schließen wollen?");

                Optional<ButtonType> result = alert.showAndWait();
                if (!(result.get() == ButtonType.OK))
                {
                        //Schließen abbrechen
                        event.consume();
                }
            });
	
        mainScene.setOnKeyReleased((KeyEvent keyEvent) -> {
            if("ESCAPE".equals(keyEvent.getCode().name()))
            {
                mvc.clearTextField();
            }
            keyEvent.consume();
        });
        
        //Titel hinzufügen
	mainStage.setTitle("INTEGRA Sitzungstool");

	mainStage.show();
        mvc.showLoginPopup();
    }
    
    public static void main(String[] args)
    {
        launch(args);
    }
}