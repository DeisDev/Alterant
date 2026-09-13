package com.deisdev.alterant.client.coating;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Strict scalar validation: malformed pack values must not silently become a different visual setting. */
final class CoatingJson {
    private CoatingJson() {}
    static int integer(JsonObject json, String key) {
        var value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key+" must be an integer");
        }
        try { return value.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException error) { throw new IllegalArgumentException(key+" must be an integer",error); }
    }
    static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected a string");
        }
        return value.getAsString();
    }
    static boolean bool(JsonObject json, String key) {
        var value=json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(key+" must be a boolean");
        }
        return value.getAsBoolean();
    }
}
