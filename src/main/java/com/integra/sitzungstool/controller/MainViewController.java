package com.integra.sitzungstool.controller;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.integra.sitzungstool.general.DataInterface;
import com.integra.sitzungstool.general.ErrorHandler;
import com.integra.sitzungstool.model.Integraner;
import com.integra.sitzungstool.model.Sitzung;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javafx.animation.Interpolator;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.VLineTo;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class MainViewController
{
    //GUI
    @FXML private Label labelName;
    @FXML private ImageView imageViewPicture;

    @FXML private ImageView imageViewWebcam;
    @FXML private Rectangle rectangleScanner;

    @FXML private TextField textFieldKennung;
    @FXML private Label labelFalscheKennung;
    @FXML private Button buttonEnter;

    @FXML private ListView<Integraner> listViewVorstand1;
    @FXML private ListView<Integraner> listViewVorstand2;
    @FXML private ListView<Integraner> listViewVorstand3;
    @FXML private ListView<Integraner> listViewAkquise;
    @FXML private ListView<Integraner> listViewIT;
    @FXML private ListView<Integraner> listViewPersonal;
    @FXML private ListView<Integraner> listViewPR;
    @FXML private ListView<Integraner> listViewQM;
    @FXML private ListView<Integraner> listViewWeitere;

    @FXML private Label labelAmount;
    @FXML private Label labelVorstand1;
    @FXML private Label labelVorstand2;
    @FXML private Label labelVorstand3;
    @FXML private Label labelAkquise;
    @FXML private Label labelIT;
    @FXML private Label labelPersonal;
    @FXML private Label labelPR;
    @FXML private Label labelQM;
    @FXML private Label labelWeitere;

    //Logik
    private BufferedImage grabbedImage;
    private Webcam webCam = null;
    private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();
    private BinaryBitmap bitmap;
    private Result result;
    
    private RotateTransition rotateTransition;
    private Integraner lastIntegranerLogin;
    private Runnable pictureAndLastIntegranerLoginReseterRunnable;
    private Thread pictureAndLastIntegranerLoginReseterThread;
    
    //Relative Ressortangaben
    private double vorstand1Amount;
    private double vorstand2Amount;
    private double vorstand3Amount;
    private double akquiseAmount;
    private double itAmount;
    private double personalAmount;
    private double prAmount;
    private double qmAmount;
    private double weitereAmount;
    private double allAmount;
    private int actualAllAmount;
    
    //Data
    public void init()
    {
        DataInterface.init(); //Erstellt lokal SQL Tabellen
        createAnimations(); //Animationen erstellen
        createTasks();  //Tasks erstellen
        startWebCamStream(); //Webcam inistalisieren
        countIntegraner(); //Für Ressort Prozentanzeige
        
        
        //GUI vorbereiten
        buttonEnter.setDisable(true);
        labelFalscheKennung.setTextFill(Color.rgb(210, 39, 30));
        textFieldKennung.textProperty().addListener((observable, oldValue, newValue) -> {
            buttonEnter.setDisable(newValue.isEmpty());
            labelFalscheKennung.setText("");
        });
        textFieldKennung.requestFocus();
    }

    public void startWebCamStream()
    {
        Task<Void> webCamIntilizer = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                //Open Webcam
                if (webCam == null)
                {
                    webCam = Webcam.getDefault();
                    webCam.setViewSize(WebcamResolution.VGA.getSize());
                    webCam.open();
                }
                else
                {
                    //Restart webcam
                    if (webCam != null)
                    {
                        webCam.close();
                    }
                    webCam = Webcam.getDefault();
                    webCam.setViewSize(WebcamResolution.VGA.getSize());
                    webCam.open();
                }
                
                //Video Updater
                Task<Void> taskVideoUpdate = new Task<Void>()
                {
                    @Override
                    protected Void call() throws Exception
                    {
                        while (true)
                        {
                            if ((grabbedImage = webCam.getImage()) != null)
                            {
                                try
                                {
                                    Thread.sleep(65); //15 Frames per Second
                                }
                                catch (Exception e)
                                {
                                    ErrorHandler.showErrorPopup(e);
                                }

                                Platform.runLater(() -> {
                                    //Video updaten
                                    imageProperty.set(SwingFXUtils.toFXImage(grabbedImage, null));
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

                //QR Code Searcher
                Task<Void> taskQRScanner = new Task<Void>()
                {
                    @Override
                    protected Void call() throws Exception
                    {
                        while (true)
                        {
                            try
                            {
                                Thread.sleep(500); //Looking for QR every 500 milliseconds
                            }
                            catch (Exception e)
                            {
                                ErrorHandler.showErrorPopup(e);
                            }

                            if ((grabbedImage = webCam.getImage()) != null)
                            {
                                //Nach QR Suchen
                                bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(grabbedImage)));
                                Platform.runLater(() -> {
                                    try
                                    {
                                        result = new MultiFormatReader().decode(bitmap);
                                        loginUserWithQR(result.getText());
                                        result = null;
                                    }
                                    catch (NotFoundException e)
                                    {
                                        // fall thru, it means there is no QR code in image
                                    }
                                });
                            }
                        }
                    }
                };
                Thread threadQRScanner = new Thread(taskQRScanner);
                threadQRScanner.setDaemon(true);
                threadQRScanner.start();
                
                return null;
            }
        };
        new Thread(webCamIntilizer).start();
    }
    
    private void createAnimations() 
    {
        //Spin Animation
        rotateTransition = new RotateTransition();
        rotateTransition.setNode(imageViewPicture);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(1080);
        rotateTransition.setInterpolator(Interpolator.EASE_BOTH);
        rotateTransition.setDuration(Duration.millis(1000));

        //Scanner Animation
        PathTransition pathTransition = new PathTransition();

        Path path = new Path();
        path.getElements().add(new MoveTo(100, 5));
        path.getElements().add(new VLineTo(180));
        path.setSmooth(true);

        pathTransition.setDuration(Duration.millis(1000));
        pathTransition.setNode(rectangleScanner);
        pathTransition.setPath(path);
        pathTransition.setCycleCount(-1);
        pathTransition.setAutoReverse(true);
        pathTransition.setDelay(Duration.millis(5000));

        pathTransition.play();
    }
    
    public void createTasks()
    {        
        pictureAndLastIntegranerLoginReseterRunnable = () -> { 
            try
            {
                Thread.sleep(3000);
            }
            catch (InterruptedException ex){return;}
            
            lastIntegranerLogin = null;
            Platform.runLater(() ->
            {
                labelName.setText("");
                imageViewPicture.setImage(new Image("/images/imageIntegraLogo.png"));
                rotateTransition.play();
            });
        };
    }

    public void countIntegraner()
    {
        ArrayList<Integraner> allIntegraner = DataInterface.getAllIntegraner();
        for(int i = 0; i < allIntegraner.size(); i++)
        {
            switch (allIntegraner.get(i).getRessort())
            {
                case "ressort-it":
                    itAmount++;
                    break;
                case "ressort-per":
                    personalAmount++;
                    break;
                case "ressort-aq":
                    akquiseAmount++;
                    break;
                case "ressort-pr":
                    personalAmount++;
                    break;
                case "ressort-qm":
                    qmAmount++;
                    break;
                case "keins":

                    switch (allIntegraner.get(i).getStab())
                    {
                        case "stab-1v":
                            vorstand1Amount++;
                            break;
                        case "stab-2v":
                            vorstand2Amount++;
                            break;
                        case "stab-3v":
                           vorstand3Amount++;
                            break;
                        case "keiner":
                            weitereAmount++;
                            break;
                    }
                    break;
            }
        }
        allAmount =  vorstand1Amount + vorstand2Amount + vorstand3Amount + akquiseAmount + itAmount + personalAmount + prAmount + qmAmount + weitereAmount;
    }
    
    public void loginUserWithTextField()
    {
        if(textFieldKennung.getText().toLowerCase().startsWith("delete"))
        {
            if(DataInterface.deleteAnwesenheit(textFieldKennung.getText().toLowerCase().replace("delete ","")));
            {
                if(DataInterface.getIntegraner(textFieldKennung.getText().toLowerCase().replace("delete ","")) != null)
                {
                    removeIntegranerFromList(DataInterface.getIntegraner(textFieldKennung.getText().toLowerCase().replace("delete ","")));
                    textFieldKennung.setText("");
                }
            }
        }
        else
        {
            loginUserWithQR(textFieldKennung.getText().toLowerCase());
        }
        
    }
        
    public void loginUserWithQR(String id)
    {
        Integraner integraner = DataInterface.integranerLogin(id); //Speichere in lokaler Datenbank
        if (integraner != null && integraner.getBenutzerkennung().equals(id))
        {
            if(lastIntegranerLogin != null && integraner.getBenutzerkennung().equals(lastIntegranerLogin.getBenutzerkennung()))
            {

            }
            else if (integraner.isAnwesend()) //Bereits anwesend
            {
                labelName.setTextFill(Color.rgb(210, 39, 30));
                labelName.setText(integraner.getName().substring(0, integraner.getName().indexOf(" ")) + ", Du bist bereits eingeloggt!");
                textFieldKennung.selectAll();

                new java.util.Timer().schedule(new java.util.TimerTask()
                {
                    @Override
                    public void run()
                    {
                        Platform.runLater(() -> {
                            labelName.setText("");
                            labelName.setTextFill(Color.BLACK);
                        });
                    }
                }, 3000);
            }
            else //Einloggen
            {
                //Einloggen auf GUI anzeigen
                labelName.setText("Hallo, " + integraner.getName().substring(0, integraner.getName().indexOf(" ")) + "!");
                imageViewPicture.setImage(integraner.getBild());
                textFieldKennung.setText("");
                insertIntegranerIntoList(integraner);
                
                //In Logik einloggen
                integraner.setAnwesend(true);
                lastIntegranerLogin = integraner;
                                
                //Animation in der Warteschlange abbrechen wenn schnell hintereinander eingelogt wird
                if(pictureAndLastIntegranerLoginReseterThread != null && pictureAndLastIntegranerLoginReseterThread.isAlive())
                {
                    pictureAndLastIntegranerLoginReseterThread.interrupt();
                }
                
                //Neue Bild Reset und LastIntegraner Reset starten
                pictureAndLastIntegranerLoginReseterThread = new Thread(pictureAndLastIntegranerLoginReseterRunnable);
                pictureAndLastIntegranerLoginReseterThread.start();
            }
        }
        else //Falsche ID
        {
            labelFalscheKennung.setText("Falsche Kennung");
            textFieldKennung.selectAll();
        }
    }
        
    private void insertIntegranerIntoList(Integraner i)
    {
        switch (i.getRessort())
        {
            case "ressort-it":
                listViewIT.getItems().add(i);
                listViewIT.scrollTo(listViewIT.getItems().size());
                labelIT.setText("Ressort IT (" + listViewIT.getItems().size() + " | " + String.format("%.0f", (listViewIT.getItems().size() / itAmount)*100) + "%)");
                break;
            case "ressort-per":
                listViewPersonal.getItems().add(i);
                listViewPersonal.scrollTo(listViewPersonal.getItems().size());
                labelPersonal.setText("Ressort Personal (" + listViewPersonal.getItems().size() + " | " + String.format("%.0f", (listViewPersonal.getItems().size() / personalAmount)*100) + "%)");
                break;
            case "ressort-aq":
                listViewAkquise.getItems().add(i);
                listViewAkquise.scrollTo(listViewAkquise.getItems().size());
                labelAkquise.setText("Ressort Akquise (" + listViewAkquise.getItems().size() + " | " + String.format("%.0f", (listViewAkquise.getItems().size() / akquiseAmount)*100) + "%)");
                break;
            case "ressort-pr":
                listViewPR.getItems().add(i);
                listViewPR.scrollTo(listViewPR.getItems().size());
                labelPR.setText("Ressort PR (" + listViewPR.getItems().size() + " | " + String.format("%.0f", (listViewPR.getItems().size() / prAmount)*100) + "%)");
                break;
            case "ressort-qm":
                listViewQM.getItems().add(i);
                listViewQM.scrollTo(listViewQM.getItems().size());
                labelQM.setText("Ressort QM (" + listViewQM.getItems().size() + " | " + String.format("%.0f", (listViewQM.getItems().size() / qmAmount)*100) + "%)");
                break;
            case "keins":

                switch (i.getStab())
                {
                    case "stab-1v":
                        listViewVorstand1.getItems().add(i);
                        listViewVorstand1.scrollTo(listViewVorstand1.getItems().size());
                        labelVorstand1.setText("1. Vorstand & Stab (" + listViewVorstand1.getItems().size() + " | " + String.format("%.0f", (listViewVorstand1.getItems().size() / vorstand1Amount)*100) + "%)");
                        break;
                    case "stab-2v":
                        listViewVorstand2.getItems().add(i);
                        listViewVorstand2.scrollTo(listViewVorstand2.getItems().size());
                        labelVorstand2.setText("2. Vorstand & Stab (" + listViewVorstand2.getItems().size() + " | " + String.format("%.0f", (listViewVorstand2.getItems().size() / vorstand2Amount)*100) + "%)");
                        break;
                    case "stab-3v":
                        listViewVorstand3.getItems().add(i);
                        listViewVorstand3.scrollTo(listViewVorstand3.getItems().size());
                        labelVorstand3.setText("3. Vorstand & Stab (" + listViewVorstand3.getItems().size() + " | " + String.format("%.0f", (listViewVorstand3.getItems().size() / vorstand3Amount)*100) + "%)");
                        break;
                    case "keiner":
                        listViewWeitere.getItems().add(i);
                        listViewWeitere.scrollTo(listViewWeitere.getItems().size());
                        labelWeitere.setText("Weitere (" + listViewWeitere.getItems().size() + " | " + String.format("%.0f", (listViewWeitere.getItems().size() / weitereAmount)*100) + "%)");
                        break;
                }
                break;
        }

        //Anzahl erhöhen
        actualAllAmount++;
        labelAmount.setText("(" + actualAllAmount + " | " + String.format("%.0f", (actualAllAmount / allAmount)*100)+ "%)");
    }
    
    private void removeIntegranerFromList(Integraner integranerToDelete)
    {
        switch (integranerToDelete.getRessort())
        {
            case "ressort-it":
                for(int pos = 0; pos < listViewIT.getItems().size(); pos++)
                {
                    if(listViewIT.getItems().get(pos).getName().equals(integranerToDelete.getName()))
                    {
                        listViewIT.getItems().remove(pos);
                    }
                }
                labelIT.setText("Ressort IT (" + listViewIT.getItems().size() + " | " + String.format("%.0f", (listViewIT.getItems().size() / itAmount)*100) + "%)");
                break;
            case "ressort-per":
                for(int pos = 0; pos < listViewPersonal.getItems().size(); pos++)
                {
                    if(listViewPersonal.getItems().get(pos).getName().equals(integranerToDelete.getName()))
                    {
                        listViewPersonal.getItems().remove(pos);
                    }
                }
                labelPersonal.setText("Ressort Personal (" + listViewPersonal.getItems().size() + " | " + String.format("%.0f", (listViewPersonal.getItems().size() / personalAmount)*100) + "%)");
                break;
            case "ressort-aq":
                for(int pos = 0; pos < listViewAkquise.getItems().size(); pos++)
                {
                    if(listViewAkquise.getItems().get(pos).getName().equals(integranerToDelete.getName()))
                    {
                        listViewAkquise.getItems().remove(pos);
                    }
                }
                labelAkquise.setText("Ressort Akquise (" + listViewAkquise.getItems().size() + " | " + String.format("%.0f", (listViewAkquise.getItems().size() / akquiseAmount)*100) + "%)");
                break;
            case "ressort-pr":
                for(int pos = 0; pos < listViewPR.getItems().size(); pos++)
                {
                    if(listViewPR.getItems().get(pos).getName().equals(integranerToDelete.getName()))
                    {
                        listViewPR.getItems().remove(pos);
                    }
                }
                labelPR.setText("Ressort PR (" + listViewPR.getItems().size() + " | " + String.format("%.0f", (listViewPR.getItems().size() / prAmount)*100) + "%)");
                break;
            case "ressort-qm":
                for(int pos = 0; pos < listViewQM.getItems().size(); pos++)
                {
                    if(listViewQM.getItems().get(pos).getName().equals(integranerToDelete.getName()))
                    {
                        listViewQM.getItems().remove(pos);
                    }
                }
                labelQM.setText("Ressort QM (" + listViewQM.getItems().size() + " | " + String.format("%.0f", (listViewQM.getItems().size() / qmAmount)*100) + "%)");
                break;
            case "keins":

                switch (integranerToDelete.getStab())
                {
                    case "stab-1v":
                        for(int pos = 0; pos < listViewVorstand1.getItems().size(); pos++)
                        {
                            if(listViewVorstand1.getItems().get(pos).getName().equals(integranerToDelete.getName()))
                            {
                                listViewVorstand1.getItems().remove(pos);
                            }
                        }
                        labelVorstand1.setText("1. Vorstand & Stab (" + listViewVorstand1.getItems().size() + " | " + String.format("%.0f", (listViewVorstand1.getItems().size() / vorstand1Amount)*100) + "%)");
                        break;
                    case "stab-2v":
                        for(int pos = 0; pos < listViewVorstand2.getItems().size(); pos++)
                        {
                            if(listViewVorstand2.getItems().get(pos).getName().equals(integranerToDelete.getName()))
                            {
                                listViewVorstand2.getItems().remove(pos);
                            }
                        }
                        labelVorstand2.setText("2. Vorstand & Stab (" + listViewVorstand2.getItems().size() + " | " + String.format("%.0f", (listViewVorstand2.getItems().size() / vorstand2Amount)*100) + "%)");
                        break;
                    case "stab-3v":
                        for(int pos = 0; pos < listViewVorstand3.getItems().size(); pos++)
                        {
                            if(listViewVorstand3.getItems().get(pos).getName().equals(integranerToDelete.getName()))
                            {
                                listViewVorstand3.getItems().remove(pos);
                            }
                        }
                        labelVorstand3.setText("3. Vorstand & Stab (" + listViewVorstand3.getItems().size() + " | " + String.format("%.0f", (listViewVorstand3.getItems().size() / vorstand3Amount)*100) + "%)");
                        break;
                    case "keiner":
                        for(int pos = 0; pos < listViewWeitere.getItems().size(); pos++)
                        {
                            if(listViewWeitere.getItems().get(pos).getName().equals(integranerToDelete.getName()))
                            {
                                listViewWeitere.getItems().remove(pos);
                            }
                        }
                        labelWeitere.setText("Weitere (" + listViewWeitere.getItems().size() + " | " + String.format("%.0f", (listViewWeitere.getItems().size() / weitereAmount)*100) + "%)");
                        break;
                }
                break;
        }

        //Anzahl verringern
        actualAllAmount--;
        labelAmount.setText("(" + actualAllAmount + " | " + String.format("%.0f", (actualAllAmount / allAmount)*100)+ "%)");
    }

    
    public void clickOnSave()
    {
        if(saveLocalDbToServer())
        {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Daten der lokalen Datenbank wurden erfolgreich auf dem INTEGRA Server gespeichert!", ButtonType.OK);
            alert.initOwner(labelName.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
            alert.setTitle("Speichern");
            alert.setHeaderText("Daten erfolgreich gespeichert");
            alert.showAndWait();
        }
        else
        {
            Alert alert = new Alert(AlertType.ERROR, "Fehler beim Speichern der Daten der lokalen Datenbank auf dem INTEGRA Server!", ButtonType.OK);
            alert.initOwner(labelName.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
            alert.setTitle("Speichern");
            alert.setHeaderText("Fehler beim Speichern der Daten");
            alert.showAndWait();
        }
    }
    
    public boolean saveLocalDbToServer()
    {
        return DataInterface.saveLocalDbToServer();
    }
    
    public void clearTextField()
    {
        textFieldKennung.clear();
    }

    public void showLoginPopup()
    {
        // Create the custom dialog.
        Dialog loginPopup = new Dialog<>();
        loginPopup.setTitle("Login");
        loginPopup.initModality(Modality.APPLICATION_MODAL);
        loginPopup.initOwner(labelName.getScene().getWindow());
        loginPopup.setHeaderText("Login");
        loginPopup.setContentText("Bitte geben Sie Ihre INTEGRA Kennung und Ihr Passwort ein.");
        loginPopup.initStyle(StageStyle.UNDECORATED);
        
        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
        ButtonType noInternetButtonType = new ButtonType("Ohne Internet fortfahren", ButtonData.CANCEL_CLOSE);
        loginPopup.getDialogPane().getButtonTypes().addAll(loginButtonType, noInternetButtonType);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField textFieldUsername = new TextField();
        textFieldUsername.setPromptText("INTEGRA Kennung");
        PasswordField passwordFieldPassword = new PasswordField();
        passwordFieldPassword.setPromptText("Passwort");
        Label labelWrongPassword = new Label("");
        labelWrongPassword.setTextFill(Color.rgb(210, 39, 30));
        labelWrongPassword.setMinWidth(150);
        
        //Add everhing to grid
        grid.add(new Label("INTEGRA Kennung:"), 0, 0);
        grid.add(textFieldUsername, 1, 0);
        grid.add(new Label("Passwort:"), 0, 1);
        grid.add(passwordFieldPassword, 1, 1);
        grid.add(labelWrongPassword, 0, 2);

        // Beide Felder müssen gefüllt sein
        Node loginButton = loginPopup.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        Node noInternetButton = loginPopup.getDialogPane().lookupButton(noInternetButtonType);
        noInternetButton.setVisible(false);
        
                        
                
                

        textFieldUsername.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.isEmpty() || passwordFieldPassword.getText().isEmpty());
            labelWrongPassword.setText("");
        });
        passwordFieldPassword.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.isEmpty() || textFieldUsername.getText().isEmpty());
            labelWrongPassword.setText("");
        });

        //Prüfe Login Daten
        loginButton.addEventFilter(ActionEvent.ACTION, (ActionEvent event) -> {
            //0 Falscher Login Daten
            if (DataInterface.vorstandLogin(textFieldUsername.getText(), passwordFieldPassword.getText()) == 0)
            {
                passwordFieldPassword.clear();
                textFieldUsername.requestFocus();
                labelWrongPassword.setText("Falsche Login Daten!");
                event.consume();
            }
            //1 Erfolgreich
            else if (DataInterface.vorstandLogin(textFieldUsername.getText(), passwordFieldPassword.getText()) == 1)
            {
                DataInterface.getIntegranetDataWithLoadingProcess();
            }
            //-1 Verbindungsfehler
            else 
            {
                noInternetButton.setVisible(true);
                labelWrongPassword.setText("Verbindungsfehler");
                event.consume();
            }
        });
        
        noInternetButton.addEventFilter(ActionEvent.ACTION, (ActionEvent event) -> {
            DataInterface.getIntegranetDataWithLoadingProcess();     
        });
        


        //Setze Grid
        loginPopup.getDialogPane().setContent(grid);

        textFieldUsername.requestFocus();

        //Anzeigen
        loginPopup.showAndWait();
        
        saveLocalDbToServer(); //Speicher alles gespeicher wurde wenn Internet
        showSitzungsAuswahlDialog(textFieldUsername.getText().toLowerCase()); //Öffne Sitzungsauswahl
    }

    public void showSitzungsAuswahlDialog(String vorstandID)
    {
        Dialog sitzungsAuswahlPopup = new Dialog<>();
        sitzungsAuswahlPopup.getDialogPane().setPadding(new Insets(5, 5, 5, 5));
        sitzungsAuswahlPopup.setTitle("Sitzungsauswahl");
        sitzungsAuswahlPopup.initModality(Modality.APPLICATION_MODAL);
        sitzungsAuswahlPopup.initOwner(labelName.getScene().getWindow());
        sitzungsAuswahlPopup.setHeaderText("Bitte wählen Sie eine Veranstaltung aus.");
        sitzungsAuswahlPopup.initStyle(StageStyle.UNDECORATED);
        
        // Set the button types.
        ButtonType loginButtonType = new ButtonType("OK", ButtonData.OK_DONE);
        sitzungsAuswahlPopup.getDialogPane().getButtonTypes().addAll(loginButtonType);

        //ListView erstellen und Daten laden
        ListView<Sitzung> listViewSitzungsAuswahl = new ListView();
        listViewSitzungsAuswahl.setItems(DataInterface.getSitzungen());
        sitzungsAuswahlPopup.getDialogPane().setContent(listViewSitzungsAuswahl);

        // Es muss eine Sitzung ausgewählt werden
        Node confirmButton = sitzungsAuswahlPopup.getDialogPane().lookupButton(loginButtonType);
        confirmButton.setDisable(true);
        
        //Enter Hotkey
        Button buttonConfirmButton = (Button) confirmButton;
        buttonConfirmButton.setDefaultButton(true);
        
        listViewSitzungsAuswahl.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            DataInterface.setSitzung(newValue);
            confirmButton.setDisable(false);

        });

        listViewSitzungsAuswahl.requestFocus();
        sitzungsAuswahlPopup.showAndWait();

        
        boolean vorstandEingeloggt = false;
        
        //Anwesende Integraner der Sitzung in die Liste eintragen
        for(Integraner i : DataInterface.getAnwesendeIntegraner())
        {
            insertIntegranerIntoList(i);
            
            if(i.getBenutzerkennung().equals(vorstandID))
            {
                vorstandEingeloggt = true;
            }
        }
        
        //Instant Login für Vorstand
        if(!vorstandEingeloggt)
        {
            loginUserWithQR(vorstandID);
        }
    }
}