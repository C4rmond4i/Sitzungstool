package com.integra.sitzungstool.general;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.integra.sitzungstool.model.Integraner;
import com.integra.sitzungstool.model.Sitzung;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerCommunication {
   private static OkHttpClient client;
   public static Sitzung selectedSitzung;
   
   public static ObservableList<Sitzung> getSitzungen()
   {
        //Dummy Daten
        ObservableList<Sitzung> sitzungen = FXCollections.observableArrayList();
        sitzungen.add(new Sitzung("01.01.2017", "id 1"));
        sitzungen.add(new Sitzung("08.01.2017", "id 2"));
        sitzungen.add(new Sitzung("15.01.2017", "id 3"));
              
        return sitzungen;
   }
   
   public static boolean vorstandLogin(String username, String password) {
       ServerCommunication.client = new OkHttpClient.Builder()
               .addInterceptor(new AuthenticationInterceptor(username, password))
               .build();
       ArrayList<Integraner> integraner = getIntegraner();
       return integraner.size() > 0;
   }
   
   public static ArrayList<Integraner> getIntegraner() {
       if (ServerCommunication.client != null) {
            try {
                Request request = new Request.Builder()
                    .url("https://integranet-dev.integra-ev.de/module/sitzungsanwesenheit/api/anwesenheit-api.php?method=users")
                    .build();
                Response response = ServerCommunication.client.newCall(request).execute();
                String body = response.body().string();
                Gson gson = new Gson();
                ArrayList<Integraner> integraner = gson.fromJson(body, new TypeToken<List<Integraner>>(){}.getType());
                return integraner;
            } catch (JsonSyntaxException | IOException e) {
                System.out.println(Arrays.toString(e.getStackTrace()));
                return new ArrayList<>();
            }
       }
       return new ArrayList<>();
   }
   
   public static Image getProfilePicture(String benutzerkennung) {
       if (ServerCommunication.client != null) {
           try {
                Request request = new Request.Builder()
                    .url("https://integranet-dev.integra-ev.de/module/sitzungsanwesenheit/api/anwesenheit-api.php?method=bild&id=" + benutzerkennung)
                    .build();
                Response response = ServerCommunication.client.newCall(request).execute();
                String body = response.body().string();
                if (body.startsWith("data:image/")) {
                    String imageString = body.split(",")[1];
                    byte[] encodedBytes = Base64.getDecoder().decode(imageString.getBytes());
                    return new Image(new ByteArrayInputStream(encodedBytes));
                }
                System.out.println(body);
                return null;
           } catch (IOException e) {
                System.out.println(Arrays.toString(e.getStackTrace()));
                return null;
            }
       }
       return null;
   }
}