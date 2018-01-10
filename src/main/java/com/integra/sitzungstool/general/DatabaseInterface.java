package com.integra.sitzungstool.general;

import com.integra.sitzungstool.model.Integraner;
import com.integra.sitzungstool.model.NichtGespeicherteSitzung;
import com.integra.sitzungstool.model.Sitzung;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.commons.io.IOUtils;

public class DatabaseInterface {

    private static Connection conn;

    public static void updateIntegraner(ArrayList<Integraner> integraner) {
        try {
            String sqlInsert = "INSERT INTO integraner ("
                    + "benutzerkennung,"
                    + "stab,"
                    + "ressort,"
                    + "name,"
                    + "bild_hash"
                    + ") VALUES ("
                    + "?,"
                    + "?,"
                    + "?,"
                    + "?,"
                    + "?"
                    + ");";
            PreparedStatement insertStatement = DatabaseInterface.conn.prepareStatement(sqlInsert);
            String sqlUpdate = "UPDATE integraner SET "
                    + "stab = ?,"
                    + "ressort = ?,"
                    + "name = ?,"
                    + "bild_hash = ?"
                    + "WHERE benutzerkennung = ?;";
            PreparedStatement updateStatement = DatabaseInterface.conn.prepareStatement(sqlUpdate);
            for (Integraner i : integraner) {
                if (DatabaseInterface.isIntegranerInserted(i)) {
                    updateStatement.setString(1, i.getStab());
                    updateStatement.setString(2, i.getRessort());
                    updateStatement.setString(3, i.getName());
                    updateStatement.setString(4, i.getBildHash());
                    updateStatement.setString(5, i.getBenutzerkennung());
                    updateStatement.executeUpdate();
                } else {
                    insertStatement.setString(1, i.getBenutzerkennung());
                    insertStatement.setString(2, i.getStab());
                    insertStatement.setString(3, i.getRessort());
                    insertStatement.setString(4, i.getName());
                    insertStatement.setString(5, i.getBildHash());
                    insertStatement.executeUpdate();
                    DatabaseInterface.saveImage(i.getBenutzerkennung());
                }
            }
            insertStatement.close();
            updateStatement.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void updateSitzungen(ArrayList<Sitzung> sitzungen) {
        try {
            String insertSql = "INSERT INTO sitzungen VALUES(?, ?, ?);";
            PreparedStatement insertStatement = DatabaseInterface.conn.prepareStatement(insertSql);
            String updateSql = "UPDATE sitzungen SET datum_string = ?, semester = ? WHERE id = ?";
            PreparedStatement updateStatement = DatabaseInterface.conn.prepareStatement(updateSql);
            for (Sitzung s : sitzungen) {
                if (DatabaseInterface.isSitzungInserted(s)) {
                    updateStatement.setString(1, s.getDatumString());
                    updateStatement.setString(2, s.getSemester());
                    updateStatement.setString(3, s.getId());
                    updateStatement.executeUpdate();
                } else {
                    insertStatement.setString(1, s.getId());
                    insertStatement.setString(2, s.getDatumString());
                    insertStatement.setString(3, s.getSemester());
                    insertStatement.executeUpdate();
                }
            }
            insertStatement.close();
            updateStatement.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static ObservableList<Sitzung> getSitzungen(String semester) {
        try {
            String selectSql = "SELECT id, datum_string, semester FROM sitzungen WHERE semester = ?";
            PreparedStatement selectStatement = DatabaseInterface.conn.prepareStatement(selectSql);
            selectStatement.setString(1, semester);
            ResultSet rs = selectStatement.executeQuery();
            ObservableList<Sitzung> sitzungen = FXCollections.observableArrayList();
            while (rs.next()) {
                sitzungen.add(new Sitzung(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
            selectStatement.close();
            return sitzungen;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return FXCollections.observableArrayList();
        }
    }

    private static boolean isIntegranerInserted(Integraner integraner) {
        try {
            String selectSql = "SELECT bild_hash, bild FROM integraner WHERE benutzerkennung = ?;";
            PreparedStatement statement = DatabaseInterface.conn.prepareStatement(selectSql);
            statement.setString(1, integraner.getBenutzerkennung());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                if (!(rs.getString("bild_hash").equals(integraner.getBildHash())) || rs.getClob("bild") == null) {
                    DatabaseInterface.saveImage(integraner.getBenutzerkennung());
                }
                statement.close();
                return true;
            }
            statement.close();
            return false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static boolean isSitzungInserted(Sitzung sitzung) {
        try {
            String selectSql = "SELECT id FROM sitzungen WHERE id = ?";
            PreparedStatement selectStatement = DatabaseInterface.conn.prepareStatement(selectSql);
            selectStatement.setString(1, sitzung.getId());
            ResultSet rs = selectStatement.executeQuery();
            while (rs.next()) {
                selectStatement.close();
                return true;
            }
            selectStatement.close();
            return false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static void saveImage(String benutzerkennung) {
        try {
            String imageString = ServerCommunication.getProfilePicture(benutzerkennung);
            if (imageString != null) {
                Clob imageClob = DatabaseInterface.conn.createClob();
                imageClob.setString(1, imageString);
                String insertSql = "UPDATE integraner SET bild = ? WHERE benutzerkennung = ?";
                PreparedStatement statement = DatabaseInterface.conn.prepareStatement(insertSql);
                statement.setClob(1, imageClob);
                statement.setString(2, benutzerkennung);
                statement.executeUpdate();
                statement.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static Integraner loginIntegraner(String benutzerkennung) {
        Integraner integraner = DatabaseInterface.getIntegraner(benutzerkennung);
        if (integraner == null) {
            return null;
        }
        boolean isAnwesend = DatabaseInterface.isAnwesend(benutzerkennung);
        integraner.setAnwesend(isAnwesend);
        if (!isAnwesend) {
            DatabaseInterface.saveAnwesenheit(benutzerkennung);
        }
        return integraner;
    }

    private static Integraner getIntegraner(String benutzerkennung) {
        try {
            String selectSql = "SELECT * FROM integraner WHERE benutzerkennung = ?";
            PreparedStatement selectStatement = DatabaseInterface.conn.prepareStatement(selectSql);
            selectStatement.setString(1, benutzerkennung);
            ResultSet rs = selectStatement.executeQuery();
            while (rs.next()) {
                Integraner integraner = new Integraner(benutzerkennung);
                integraner.setName(rs.getString("name"));
                integraner.setBildHash(rs.getString("bild_hash"));
                integraner.setStab(rs.getString("stab"));
                integraner.setRessort(rs.getString("ressort"));
                Clob imageClob = rs.getClob("bild");
                if (imageClob != null) {
                    Reader reader = imageClob.getCharacterStream();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(reader, writer);
                    String imageString = writer.toString();
                    byte[] encodedBytes = Base64.getDecoder().decode(imageString.getBytes());
                    integraner.setBild(new Image(new ByteArrayInputStream(encodedBytes)));
                }
                selectStatement.close();
                return integraner;
            }
            return null;
        } catch (IOException | SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private static boolean isAnwesend(String benutzerkennung) {
        try {
            String selectAnwesenheitSql = "SELECT * FROM anwesenheit WHERE sitzungs_id = ? and benutzerkennung = ?";
            PreparedStatement selectAnwesenheitStatement = DatabaseInterface.conn.prepareStatement(selectAnwesenheitSql);
            selectAnwesenheitStatement.setInt(1, Integer.parseInt(DataInterface.getSitzung().getId()));
            selectAnwesenheitStatement.setString(2, benutzerkennung);
            ResultSet anwesenheitsRS = selectAnwesenheitStatement.executeQuery();
            while (anwesenheitsRS.next()) {
                selectAnwesenheitStatement.close();
                return true;
            }
            selectAnwesenheitStatement.close();
            return false;
        } catch (NumberFormatException | SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static void saveAnwesenheit(String benutzerkennung) {
        try {
            String insertSql = "INSERT into anwesenheit VALUES (?, ?, ?)";
            PreparedStatement insertStatement = DatabaseInterface.conn.prepareStatement(insertSql);
            insertStatement.setInt(1, Integer.parseInt(DataInterface.getSitzung().getId()));
            insertStatement.setString(2, benutzerkennung);
            insertStatement.setInt(3, 0);
            insertStatement.executeUpdate();
            insertStatement.close();
        } catch (NumberFormatException | SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static ArrayList<Integraner> getAnwesendeIntegraner(Sitzung sitzung) {
        try {
            String selectAnwesendeIntegranerSql = "SELECT benutzerkennung FROM anwesenheit WHERE sitzungs_id = ?";
            PreparedStatement selectAnwesendeIntegranerStatement = DatabaseInterface.conn.prepareStatement(selectAnwesendeIntegranerSql);
            selectAnwesendeIntegranerStatement.setString(1, sitzung.getId());
            ResultSet anwesendeIntegranerResultSet = selectAnwesendeIntegranerStatement.executeQuery();
            ArrayList<Integraner> integraner = new ArrayList<>();
            while (anwesendeIntegranerResultSet.next()) {
                integraner.add(DatabaseInterface.getIntegraner(anwesendeIntegranerResultSet.getString(1)));
            }
            return integraner;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public static ArrayList<NichtGespeicherteSitzung> getNichtGespeicherteSitzungen() {
        try {
            String selectNichtGespeicherteSitzungenSql = "SELECT sitzungs_id, benutzerkennung FROM anwesenheit WHERE gespeichert = 0";
            PreparedStatement selectNichtGespeicherteSitzungenStatement = DatabaseInterface.conn.prepareStatement(selectNichtGespeicherteSitzungenSql);
            ResultSet nichtGespeicherteSitzungenResultSet = selectNichtGespeicherteSitzungenStatement.executeQuery();
            ArrayList<NichtGespeicherteSitzung> nichtGespeicherteSitzungen = new ArrayList<>();
            while (nichtGespeicherteSitzungenResultSet.next()) {
                String sitzungsID = nichtGespeicherteSitzungenResultSet.getString(1);
                String benutzerkennung = nichtGespeicherteSitzungenResultSet.getString(2);
                boolean istSitzungEnthalten = false;
                int i = 0;
                for (NichtGespeicherteSitzung ngs : nichtGespeicherteSitzungen) {
                    if (ngs.getId().equals(sitzungsID)) {
                        istSitzungEnthalten = true;
                        break;
                    }
                    i++;
                }
                if (!istSitzungEnthalten) {
                    String[] benutzerkennungen = {benutzerkennung};
                    nichtGespeicherteSitzungen.add(new NichtGespeicherteSitzung(sitzungsID, benutzerkennungen));
                } else {
                    String[] benutzerkennungen = nichtGespeicherteSitzungen.get(i).getBenutzerkennungen();
                    String[] neueBenutzerkennungen = new String[benutzerkennungen.length + 1];
                    System.arraycopy(benutzerkennungen, 0, neueBenutzerkennungen, 0, benutzerkennungen.length);
                    neueBenutzerkennungen[neueBenutzerkennungen.length - 1] = benutzerkennung;
                    nichtGespeicherteSitzungen.set(i, new NichtGespeicherteSitzung(sitzungsID, neueBenutzerkennungen));
                }
            }
            return nichtGespeicherteSitzungen;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public static void saveGespeicherteSitzungen(NichtGespeicherteSitzung nichtGespeicherteSitzung) {
        try {
            String updateAnwesenheitSql = "UPDATE anwesenheit SET gespeichert = 1 WHERE sitzungs_id = ? AND benutzerkennung = ?";
            PreparedStatement updateAnwesenheitStatement = DatabaseInterface.conn.prepareStatement(updateAnwesenheitSql);
            updateAnwesenheitStatement.setString(1, nichtGespeicherteSitzung.getId());
            String[] benutzerkennungen = nichtGespeicherteSitzung.getBenutzerkennungen();
            for (int i = 0; i < benutzerkennungen.length; i++) {
                updateAnwesenheitStatement.setString(2, benutzerkennungen[i]);
                System.out.println(updateAnwesenheitStatement);
                updateAnwesenheitStatement.executeUpdate();
            }
            updateAnwesenheitStatement.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void createTables() {
        try {
            Class.forName("org.h2.Driver");
            DatabaseInterface.conn = DriverManager.getConnection("jdbc:h2:~/.sitzungstool/database", "sa", "");
            Statement stmt = DatabaseInterface.conn.createStatement();
            String sqlCreateIntegraner = "CREATE TABLE IF NOT EXISTS integraner"
                    + "( benutzerkennung VARCHAR(50) NOT NULL,"
                    + "  stab VARCHAR(50) NOT NULL,"
                    + "  ressort VARCHAR(50) NOT NULL,"
                    + "  name VARCHAR(50) NOT NULL,"
                    + "  bild_hash VARCHAR(32) NOT NULL,"
                    + "  bild CLOB,"
                    + "  PRIMARY KEY (benutzerkennung)"
                    + ");";
            String sqlCreateSitzungen = "CREATE TABLE IF NOT EXISTS sitzungen"
                    + "( id NUMBER(5) NOT NULL,"
                    + "  datum_string VARCHAR(10) NOT NULL,"
                    + "  semester VARCHAR(5) NOT NULL,"
                    + "  PRIMARY KEY (id)"
                    + ");";
            String sqlCreateAnwesenheit = "CREATE TABLE IF NOT EXISTS anwesenheit"
                    + "( sitzungs_id NUMBER(5) NOT NULL,"
                    + "  benutzerkennung VARCHAR(50) NOT NULL,"
                    + "  gespeichert NUMBER(1) NOT NULL,"
                    + "  FOREIGN KEY (sitzungs_id)"
                    + "  REFERENCES sitzungen(id),"
                    + "  FOREIGN KEY (benutzerkennung)"
                    + "  REFERENCES integraner(benutzerkennung)"
                    + ");";
            stmt.execute(sqlCreateIntegraner);
            stmt.execute(sqlCreateSitzungen);
            stmt.execute(sqlCreateAnwesenheit);
            stmt.close();
            DataInterface.setHasDatabaseConnection(true);
        } catch (ClassNotFoundException | SQLException e) {
            DataInterface.setHasDatabaseConnection(false);
            DatabaseInterface.conn = null;
            System.out.println(e.getMessage());
        }
    }

    public static boolean hasData() {
        try {
            String selectSql = "SELECT benutzerkennung FROM integraner";
            PreparedStatement stmt = DatabaseInterface.conn.prepareStatement(selectSql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                stmt.close();
                return true;
            }
            stmt.close();
            return false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
