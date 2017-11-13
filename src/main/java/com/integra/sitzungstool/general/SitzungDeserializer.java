package com.integra.sitzungstool.general;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.integra.sitzungstool.model.Sitzung;
import java.lang.reflect.Type;

public class SitzungDeserializer implements JsonDeserializer<Sitzung> {
    @Override
    public Sitzung deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement jsonID = jsonObject.get("id");
        JsonElement jsonDatumString = jsonObject.get("datumString");
        return new Sitzung(jsonID.getAsString(), jsonDatumString.getAsString());
    }
}
