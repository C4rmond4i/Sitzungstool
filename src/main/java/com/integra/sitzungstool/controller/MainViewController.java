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
import com.integra.sitzungstool.general.ServerCommunication;
import com.integra.sitzungstool.model.Integraner;
import com.integra.sitzungstool.model.Sitzung;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;
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
    private TimerTask taskResetImage;

    //Data
    public void init()
    {
        //Erstellt lokal SQL Tabellen
        DataInterface.init();

        createAnimations();
        createTasks();

        //Webcam inistalisiren
        Task<Void> webCamIntilizer = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                if (webCam == null)
                {
                    webCam = Webcam.getDefault();
                    webCam.setViewSize(WebcamResolution.VGA.getSize());
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
                            e.printStackTrace();
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

        Task<Void> taskQRScanner = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                while (true) {
                    try
                    {
                        Thread.sleep(500); //Looking for QR every 500 milliseconds
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    if ((grabbedImage = webCam.getImage()) != null) {
                        //Nach QR Suchen
                        bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(grabbedImage)));
                        Platform.runLater(() -> {
                            try {
                                result = new MultiFormatReader().decode(bitmap);
                                loginUserWithQR(result.getText());
                                result = null;
                            } catch (NotFoundException e) {
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
    }

    private void closeCamera()
    {
        if (webCam != null)
        {
            webCam.close();
        }
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
        taskResetImage = new TimerTask()
        {
            @Override
            public void run()
            {
                Platform.runLater(() ->
                {
                    labelName.setText("");
                    imageViewPicture.setImage(new Image("/images/imageIntegraLogo.png"));
                    rotateTransition.play();
                });
            }
        };
    }

    
    public void loginUserWithTextField()
    {
        loginUserWithQR(textFieldKennung.getText().toLowerCase());
    }
        
    public void loginUserWithQR(String id)
    {
        Integraner i = DataInterface.integranerLogin(id);
        if (i != null && i.getBenutzerkennung().equals(id))
        {
            if (i.isAnwesend())
            {
                labelName.setTextFill(Color.rgb(210, 39, 30));
                labelName.setText("Bereits eingeloggt");
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
            else
            {
                labelName.setText("Hallo, " + i.getName().substring(0, i.getName().indexOf(" ")) + "!");
                imageViewPicture.setImage(i.getBild());
                textFieldKennung.setText("");
                i.setAnwesend(true);

                insertIntegranerIntoList(i);


                taskResetImage.cancel();
                createTasks();
                new Timer().schedule(taskResetImage, 5000);
            }
        } else {
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
                labelIT.setText("Ressort IT (" + listViewIT.getItems().size() + ")");
                break;
            case "ressort-per":
                listViewPersonal.getItems().add(i);
                labelPersonal.setText("Ressort Personal (" + listViewPersonal.getItems().size() + ")");
                break;
            case "ressort-aq":
                listViewAkquise.getItems().add(i);
                labelAkquise.setText("Ressort Akquise (" + listViewAkquise.getItems().size() + ")");
                break;
            case "ressort-pr":
                listViewPR.getItems().add(i);
                labelPR.setText("Ressort PR (" + listViewPR.getItems().size() + ")");
                break;
            case "ressort-qm":
                listViewQM.getItems().add(i);
                labelQM.setText("Ressort QM (" + listViewQM.getItems().size() + ")");
                break;
            case "keins":

                switch (i.getStab())
                {
                    case "stab-1v":
                        listViewVorstand1.getItems().add(i);
                        labelVorstand1.setText("1. Vorstand & Stab (" + listViewVorstand1.getItems().size() + ")");
                        break;
                    case "stab-2v":
                        listViewVorstand2.getItems().add(i);
                        labelVorstand2.setText("2. Vorstand & Stab (" + listViewVorstand2.getItems().size() + ")");
                        break;
                    case "stab-3v":
                        listViewVorstand3.getItems().add(i);
                        labelVorstand3.setText("3. Vorstand & Stab (" + listViewVorstand3.getItems().size() + ")");
                        break;
                    case "keiner":
                        listViewWeitere.getItems().add(i);
                        labelWeitere.setText("Weitere (" + listViewWeitere.getItems().size() + ")");
                        break;
                }
                break;
        }

        //Anzahl erhöhen
        int amount = Integer.valueOf(labelAmount.getText().replace("(", "").replace(")", ""));
        amount++;
        labelAmount.setText("(" + amount + ")");
    }

    
    public void save()
    {
        ServerCommunication.save();
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

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
        loginPopup.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

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

        grid.add(new Label("INTEGRA Kennung:"), 0, 0);
        grid.add(textFieldUsername, 1, 0);
        grid.add(new Label("Passwort:"), 0, 1);
        grid.add(passwordFieldPassword, 1, 1);
        grid.add(labelWrongPassword, 0, 2);

        // Beide Felder müssen gefüllt sein
        Node loginButton = loginPopup.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        textFieldUsername.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.isEmpty() || passwordFieldPassword.getText().isEmpty());
            labelWrongPassword.setText("");
        });
        passwordFieldPassword.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.isEmpty() || textFieldUsername.getText().isEmpty());
            labelWrongPassword.setText("");
        });

        //Prüfe Login Daten
        loginButton.addEventFilter(ActionEvent.ACTION, (ActionEvent event)
                -> {
            if (!DataInterface.vorstandLogin(textFieldUsername.getText(), passwordFieldPassword.getText())) {
                passwordFieldPassword.clear();
                textFieldUsername.requestFocus();
                labelWrongPassword.setText("Falsche Login Daten!");
                event.consume();
            } else {
                DataInterface.getIntegranetDataWithLoadingProcess();
            }
        });

        //Setze Grid
        loginPopup.getDialogPane().setContent(grid);

        textFieldUsername.requestFocus();

        //Anzeigen
        loginPopup.showAndWait();
        showSitzungsAuswahlDialog(textFieldUsername.getText().toLowerCase());
    }

    public void showSitzungsAuswahlDialog(String vorstandID)
    {
        Dialog sitzungsAuswahlPopup = new Dialog<>();
        sitzungsAuswahlPopup.getDialogPane().setPadding(new Insets(5, 5, 5, 5));
        sitzungsAuswahlPopup.setTitle("Sitzungsauswahl");
        sitzungsAuswahlPopup.initModality(Modality.APPLICATION_MODAL);
        sitzungsAuswahlPopup.initOwner(labelName.getScene().getWindow());
        sitzungsAuswahlPopup.setHeaderText("Bitte wählen Sie eine Veranstaltung aus.");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("OK", ButtonData.OK_DONE);
        sitzungsAuswahlPopup.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        //ListView erstellen und Daten laden
        ListView<Sitzung> listViewSitzungsAuswahl = new ListView();
        listViewSitzungsAuswahl.setItems(DataInterface.getSitzungen());
        sitzungsAuswahlPopup.getDialogPane().setContent(listViewSitzungsAuswahl);

        // Es muss eine Sitzung ausgewählt werden
        Node confirmButton = sitzungsAuswahlPopup.getDialogPane().lookupButton(loginButtonType);
        confirmButton.setDisable(true);
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