package com.integra.sitzungstool.model;

import com.integra.sitzungstool.general.ServerCommunication;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import javafx.scene.image.Image;
import org.apache.commons.io.IOUtils;

public class DatabaseInterface {
    
    private static Connection conn;
    
    public static void updateIntegraner(ArrayList<Integraner> integraner) {
        try {
            if (DatabaseInterface.conn != null) {
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
                    }
                }
                insertStatement.close();
                updateStatement.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public static boolean isIntegranerInserted(Integraner integraner) {
        try {
            String selectSql = "SELECT bild_hash, bild FROM integraner WHERE benutzerkennung = ?;";
            PreparedStatement statement = DatabaseInterface.conn.prepareStatement(selectSql);
            statement.setString(1, integraner.getBenutzerkennung());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getClob("bild") == null);
                if (!(rs.getString("bild_hash").equals(integraner.getBildHash())) || rs.getClob("bild") == null) {
                    DatabaseInterface.saveImage(integraner.getBenutzerkennung());
                }
                statement.close();
                return true;
            }
            statement.close();
            DatabaseInterface.saveImage(integraner.getBenutzerkennung());
            return false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
    
    public static void saveImage(String benutzerkennung) {
        try {
            System.out.println(benutzerkennung);
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
        if (DatabaseInterface.conn != null) {
            try {
                String selectSql = "SELECT * FROM integraner WHERE benutzerkennung = ?";
                PreparedStatement statement = DatabaseInterface.conn.prepareStatement(selectSql);
                statement.setString(1, benutzerkennung);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    Integraner integraner = new Integraner(benutzerkennung);
                    integraner.setName(rs.getString("name"));
                    integraner.setBildHash(rs.getString("bild_hash"));
                    integraner.setStab(rs.getString("stab"));
                    integraner.setRessort(rs.getString("ressort"));
                    Clob imageClob = rs.getClob("bild");
                    Reader reader = imageClob.getCharacterStream();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(reader, writer);
                    String imageString = writer.toString();
                    byte[] encodedBytes = Base64.getDecoder().decode(imageString.getBytes());
                    integraner.setBild(new Image(new ByteArrayInputStream(encodedBytes)));
                    statement.close();
                    return integraner;
                }
                statement.close();
                return null;
            } catch (IOException | SQLException e) {
                System.out.println(e.getMessage());
                return null;
            }
        }
        return null;
    }
    
    public static void createTables() {
        try {
            Class.forName("org.h2.Driver");
            DatabaseInterface.conn = DriverManager.getConnection("jdbc:h2:~/.sitzungstool/database", "sa", "");
            Statement stmt = DatabaseInterface.conn.createStatement();
            String sqlCreate = "CREATE TABLE IF NOT EXISTS integraner"
                    + "( benutzerkennung VARCHAR(50) NOT NULL,"
                    + "  stab VARCHAR(50) NOT NULL,"
                    + "  ressort VARCHAR(50) NOT NULL,"
                    + "  name VARCHAR(50) NOT NULL,"
                    + "  bild_hash VARCHAR(32) NOT NULL,"
                    + "  bild CLOB,"
                    + "  PRIMARY KEY (benutzerkennung)"
                    + ");";
            stmt.execute(sqlCreate);
            stmt.close();
        } catch (ClassNotFoundException | SQLException e) {
            DatabaseInterface.conn = null;
            System.out.println(e.getMessage());
        }
    }
}
