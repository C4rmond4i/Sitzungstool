package com.integra.sitzungstool.general;

import com.integra.sitzungstool.controller.MainViewController;
import com.integra.sitzungstool.model.Integraner;
import com.integra.sitzungstool.model.Sitzung;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DataInterface {

    private static Sitzung selectedSitzung = null;
    private static boolean hasIntegranetConnection = false;
    private static boolean hasDatabaseConnection = false;

    public static Sitzung getSitzung() {
        return DataInterface.selectedSitzung;
    }

    public static void setSitzung(Sitzung sitzung) {
        DataInterface.selectedSitzung = sitzung;
    }

    public static boolean hasIntegranetConnection() {
        return DataInterface.hasIntegranetConnection;
    }

    public static void setHasIntegranetConnection(boolean hasIntegranetConnection) {
        DataInterface.hasIntegranetConnection = hasIntegranetConnection;
    }

    public static boolean hasDatabaseConnection() {
        return DataInterface.hasDatabaseConnection;
    }

    public static void setHasDatabaseConnection(boolean hasDatabaseConnection) {
        DataInterface.hasDatabaseConnection = hasDatabaseConnection;
    }

    public static int vorstandLogin(String username, String password) {
        return ServerCommunication.loginVorstand(username, password);
    }

    public static boolean showLoadingProgress() {
        return DataInterface.hasDatabaseConnection() && !DatabaseInterface.hasData();
    }

    public static boolean showOptionToSkipLogin() {
        return DataInterface.hasDatabaseConnection() && DatabaseInterface.hasData();
    }

    public static ObservableList<Sitzung> getSitzungen() {
        if (DataInterface.hasDatabaseConnection()) {
            return DatabaseInterface.getSitzungen(DataInterface.getCurrentSemester());
        } else if (DataInterface.hasIntegranetConnection()) {
            ArrayList<Sitzung> sitzungenArrayList = ServerCommunication.getSitzungen(DataInterface.getCurrentSemester());
            ObservableList<Sitzung> sitzungen = FXCollections.observableArrayList();
            sitzungenArrayList.forEach((s) -> {
                sitzungen.add(s);
            });
            return sitzungen;
        }
        return FXCollections.observableArrayList();
    }
    
    public static ArrayList<Integraner> getAnwesendeIntegraner() {
        if (DataInterface.hasDatabaseConnection() && DataInterface.getSitzung() != null) {
            return DatabaseInterface.getAnwesendeIntegraner(DataInterface.getSitzung());
        }
        return new ArrayList<>();
    }

    public static Integraner integranerLogin(String benutzerkennung) {
        return DatabaseInterface.loginIntegraner(benutzerkennung);
    }

    public static void init() {
        DatabaseInterface.createTables();
    }

    public static void getIntegranetDataWithLoadingProcess() {
        DatabaseInterface.updateIntegraner(ServerCommunication.getIntegraner());
        DatabaseInterface.updateSitzungen(ServerCommunication.getSitzungen(DataInterface.getCurrentSemester()));
    }

    public static boolean deleteAnwesenheit(String benutzerkennung) {
        if (DataInterface.hasDatabaseConnection() && DataInterface.getSitzung() != null) {
            return DatabaseInterface.deleteAnwesenheit(DataInterface.getSitzung() ,benutzerkennung);
        }
        return false;
    }
    
    public static Integraner getIntegraner(String benutzerkennung) {
        if (DataInterface.hasDatabaseConnection()) {
            return DatabaseInterface.getIntegraner(benutzerkennung);
        }
        return null;
    }
    
    public static boolean saveLocalDbToServer() {
        return ServerCommunication.saveLocalDbToServer(DatabaseInterface.getNichtGespeicherteSitzungen());
    }
    
    private static String getCurrentSemester() {
        /* int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        return currentYear + (currentMonth < 6 ? "1" : "2"); */
        return "20172";
    }
}
