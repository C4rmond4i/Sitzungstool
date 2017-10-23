package com.integra.sitzungstool.general;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.integra.sitzungstool.model.Integraner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerCommunication {
   private OkHttpClient client;
   
   public ServerCommunication(String username, String password) {
       this.client = new OkHttpClient.Builder()
               .addInterceptor(new AuthenticationInterceptor(username, password))
               .build();
   }
   
   public ArrayList<Integraner> getIntegraner() {
       try {
            Request request = new Request.Builder()
                    .url("https://integranet-dev.integra-ev.de/module/sitzungsanwesenheit/api/anwesenheit-api.php?method=users")
                    .build();
            Response response = this.client.newCall(request).execute();
            String body = response.body().string();
            Gson gson = new Gson();
            ArrayList<Integraner> integraner = gson.fromJson(body, new TypeToken<List<Integraner>>(){}.getType());
            return integraner;
       } catch (JsonSyntaxException | IOException e) {
           System.out.println(e.getStackTrace());
           return new ArrayList<>();
       }
   }
}
