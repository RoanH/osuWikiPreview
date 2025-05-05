package dev.roanh.wiki.github;

import java.lang.reflect.Type;
import java.util.Locale;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Deserialiser used to parse Enum instances by allowing both upper,
 * lower and mixed case constants for the value present in the json.
 * Also returns the 'UNKNOWN' constant as a fallback if present.
 * @author Roan
 */
@SuppressWarnings("rawtypes")
public class EnumDeserializer implements JsonDeserializer<Enum>{

	@SuppressWarnings("unchecked")
	@Override
	public Enum deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException{
		try{
			return Enum.<Enum>valueOf((Class<Enum>)typeOfT, json.getAsString().toUpperCase(Locale.ROOT));
		}catch(IllegalArgumentException ignore){
			return Enum.<Enum>valueOf((Class<Enum>)typeOfT, "UNKNOWN");
		}
	}
}
