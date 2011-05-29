package com.icecondor.nest.types;
import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import android.location.Location;

public class LocationSerializer implements JsonSerializer<Location> {

	@Override
	public JsonElement serialize(Location location, Type type,
			JsonSerializationContext context) {
		JsonObject jgps = new JsonObject();
		jgps.addProperty("provider", location.getProvider());
		return jgps;
	}

}
