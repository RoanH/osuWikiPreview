/*
 * osu! wiki preview site
 * Copyright (C) 2023  Roan Hofland (roan@roanh.dev) and contributors.
 * GitHub Repository: https://github.com/RoanH/osuWikiPreview
 * GitLab Repository: https://git.roanh.dev/roan/osuwikipreview
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
