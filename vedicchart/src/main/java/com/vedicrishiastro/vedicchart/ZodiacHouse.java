package com.vedicrishiastro.vedicchart;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ZodiacHouse {
    private final int sign;
    private final String signName;
    private final List<String> planets;
    private final List<String> planetShortNames;
    private final List<String> planetDegrees;

    public ZodiacHouse(
            int sign,
            String signName,
            List<String> planets,
            List<String> planetShortNames,
            List<String> planetDegrees
    ) {
        this.sign = sign;
        this.signName = signName == null ? "" : signName;
        this.planets = immutableCopy(planets);
        this.planetShortNames = immutableCopy(planetShortNames);
        this.planetDegrees = immutableCopy(planetDegrees);
    }

    public int getSign() {
        return sign;
    }

    public String getSignName() {
        return signName;
    }

    public List<String> getPlanets() {
        return planets;
    }

    public List<String> getPlanetShortNames() {
        return planetShortNames;
    }

    public List<String> getPlanetDegrees() {
        return planetDegrees;
    }

    public String getPlanetDisplayText() {
        List<String> source = planetShortNames.isEmpty() ? planets : planetShortNames;
        StringBuilder builder = new StringBuilder();
        for (String value : source) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }

    public static List<ZodiacHouse> fromJson(String json) throws JSONException {
        return fromJsonArray(new JSONArray(json));
    }

    public static List<ZodiacHouse> fromJsonArray(JSONArray array) throws JSONException {
        List<ZodiacHouse> houses = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.getJSONObject(index);
            houses.add(new ZodiacHouse(
                    item.optInt("sign"),
                    item.optString("sign_name"),
                    stringsFrom(item.optJSONArray("planet")),
                    stringsFrom(item.optJSONArray("planet_small")),
                    stringsFrom(item.optJSONArray("planet_degree"))
            ));
        }
        return houses;
    }

    private static List<String> stringsFrom(JSONArray array) throws JSONException {
        if (array == null) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            values.add(array.optString(index));
        }
        return values;
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
